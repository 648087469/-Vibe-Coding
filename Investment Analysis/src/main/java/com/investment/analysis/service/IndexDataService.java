package com.investment.analysis.service;

import com.investment.analysis.common.IndexDataException;
import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.mapper.IndexDailyMapper;
import com.investment.analysis.model.IndexDailyData;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.provider.IndexDataProvider;
import com.investment.analysis.provider.ProviderHealthTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 指数行情数据装载服务。
 * 策略：优先读数据库；当库中无数据、数据不完整、数据过期或手动强制刷新时，
 * 依次调用外部行情接口（东方财富 -> 腾讯财经 -> 新浪财经）拉取 365 天数据写入数据库，再回读数据库。
 */
@Service
public class IndexDataService {

    private static final Logger log = LoggerFactory.getLogger(IndexDataService.class);

    private final IndexDailyMapper indexDailyMapper;
    private final ProviderHealthTracker healthTracker;
    private final AnalysisProperties properties;
    private final List<IndexDataProvider> providers;

    public IndexDataService(IndexDailyMapper indexDailyMapper,
                            ProviderHealthTracker healthTracker,
                            AnalysisProperties properties,
                            List<IndexDataProvider> providers) {
        this.indexDailyMapper = indexDailyMapper;
        this.healthTracker = healthTracker;
        this.properties = properties;
        List<IndexDataProvider> sorted = new ArrayList<IndexDataProvider>(providers);
        Collections.sort(sorted, Comparator.comparingInt(IndexDataProvider::order));
        this.providers = sorted;
    }

    /**
     * 装载指定指数最近 windowDays 天的日线数据。
     *
     * @param definition   指数定义
     * @param forceRefresh 是否强制走接口刷新
     */
    public IndexDataBundle loadWindow(IndexDefinition definition, boolean forceRefresh) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(2, properties.getWindowDays()) - 1L);

        List<IndexDaily> dbSeries = indexDailyMapper.selectRange(definition.getCode(), start, end);
        String reason = resolveRefreshReason(dbSeries, forceRefresh);

        IndexDataBundle bundle = new IndexDataBundle();
        if (reason != null) {
            log.info("指数 {} 触发接口拉取，原因：{}", definition.getCode(), reason);
            FetchOutcome outcome = fetchAndStore(definition, start, end);
            if (outcome.success) {
                dbSeries = indexDailyMapper.selectRange(definition.getCode(), start, end);
                bundle.setFromDatabase(false);
                bundle.setFetchedFrom(outcome.providerName);
                bundle.setRefreshReason(reason);
            } else if (!dbSeries.isEmpty()) {
                // 接口全部失败但库中有历史数据：降级使用库中数据，保证页面可用
                log.warn("指数 {} 外部接口全部不可用，降级使用数据库中的 {} 条历史数据",
                        definition.getCode(), dbSeries.size());
                bundle.setFromDatabase(true);
                bundle.setRefreshReason(reason + "（接口不可用，已降级使用库中数据）");
            } else {
                throw new IndexDataException("指数 " + definition.getName() + "（" + definition.getCode()
                        + "）数据库无数据且外部接口获取失败：" + outcome.message);
            }
        } else {
            bundle.setFromDatabase(true);
        }

        if (dbSeries.isEmpty()) {
            throw new IndexDataException("指数 " + definition.getName() + " 暂无可用行情数据");
        }
        bundle.setSeries(dbSeries);
        bundle.setDataSource(resolveDataSource(bundle, dbSeries));
        return bundle;
    }

    private String resolveDataSource(IndexDataBundle bundle, List<IndexDaily> series) {
        if (!bundle.isFromDatabase() && bundle.getFetchedFrom() != null) {
            return bundle.getFetchedFrom();
        }
        IndexDaily latest = series.get(series.size() - 1);
        return latest.getDataSource() == null ? "DATABASE" : latest.getDataSource();
    }

    /**
     * 判断是否需要走接口刷新，返回 null 表示库中数据可直接使用。
     */
    private String resolveRefreshReason(List<IndexDaily> dbSeries, boolean forceRefresh) {
        if (forceRefresh) {
            return "手动强制刷新";
        }
        if (dbSeries == null || dbSeries.isEmpty()) {
            return "数据库中无该指数数据";
        }
        if (dbSeries.size() < properties.getMinRequiredRecords()) {
            return "数据库记录不足（当前 " + dbSeries.size() + " 条，要求至少 "
                    + properties.getMinRequiredRecords() + " 条）";
        }
        LocalDate latest = dbSeries.get(dbSeries.size() - 1).getTradeDate();
        long gap = ChronoUnit.DAYS.between(latest, LocalDate.now());
        if (gap > properties.getMaxDataStalenessDays()) {
            return "数据库数据已过期（最新交易日 " + latest + "，距今 " + gap + " 天）";
        }
        return null;
    }

    private static class FetchOutcome {
        private boolean success;
        private String providerName;
        private String message;
    }

    /**
     * 依次尝试各数据源，成功即写库并返回。
     */
    private FetchOutcome fetchAndStore(IndexDefinition definition, LocalDate start, LocalDate end) {
        FetchOutcome outcome = new FetchOutcome();
        StringBuilder errors = new StringBuilder();

        for (IndexDataProvider provider : providers) {
            if (!isEnabled(provider)) {
                continue;
            }
            if (healthTracker.isBlocked(provider.sourceName())) {
                log.debug("数据源 {} 处于熔断冷却期，跳过", provider.sourceName());
                continue;
            }
            try {
                long begin = System.currentTimeMillis();
                List<IndexDailyData> data = provider.fetch(definition, start, end);
                if (data == null || data.isEmpty()) {
                    throw new IndexDataException("返回数据为空");
                }
                saveData(data);
                healthTracker.recordSuccess(provider.sourceName());
                log.info("指数 {} 通过 {} 获取 {} 条数据，耗时 {} ms", definition.getCode(),
                        provider.displayName(), data.size(), System.currentTimeMillis() - begin);
                outcome.success = true;
                outcome.providerName = provider.displayName();
                outcome.message = "OK";
                return outcome;
            } catch (Exception e) {
                healthTracker.recordFailure(provider.sourceName(),
                        properties.getFetch().getFailureThreshold(),
                        properties.getFetch().getCircuitOpenMinutes());
                log.warn("指数 {} 通过 {} 获取失败：{}", definition.getCode(), provider.displayName(), e.getMessage());
                if (errors.length() > 0) {
                    errors.append("；");
                }
                errors.append(provider.displayName()).append(" - ").append(e.getMessage());
            }
        }
        outcome.success = false;
        outcome.message = errors.length() == 0 ? "所有数据源均已禁用或熔断" : errors.toString();
        return outcome;
    }

    private boolean isEnabled(IndexDataProvider provider) {
        AnalysisProperties.Providers providers = properties.getProviders();
        switch (provider.sourceName()) {
            case "EASTMONEY":
                return providers.getEastmoney().isEnabled();
            case "TENCENT":
                return providers.getTencent().isEnabled();
            case "SINA":
                return providers.getSina().isEnabled();
            default:
                return true;
        }
    }

    /**
     * 批量写入数据库（按 index_code + trade_date 幂等 UPSERT）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveData(List<IndexDailyData> data) {
        final int batchSize = 200;
        for (int i = 0; i < data.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, data.size());
            indexDailyMapper.upsertBatch(data.subList(i, endIndex));
        }
    }

    /**
     * 查询某指数指定天数的走势数据。
     */
    public List<IndexDaily> loadTrend(IndexDefinition definition, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(2, days) - 1L);
        List<IndexDaily> series = indexDailyMapper.selectRange(definition.getCode(), start, end);
        if (series.isEmpty()) {
            loadWindow(definition, false);
            series = indexDailyMapper.selectRange(definition.getCode(), start, end);
        }
        return series;
    }
}
