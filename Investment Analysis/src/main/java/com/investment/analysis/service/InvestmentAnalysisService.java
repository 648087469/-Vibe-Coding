package com.investment.analysis.service;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.entity.IndexAnalysisResult;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.mapper.IndexAnalysisResultMapper;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.model.IndexWinRateVO;
import com.investment.analysis.model.TrendPointVO;
import com.investment.analysis.model.WinRateSummaryVO;
import com.investment.analysis.provider.ProviderHealthTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 投资分析模块核心服务：装载数据 -> 计算胜率 -> 落库 -> 汇总。
 */
@Service
public class InvestmentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentAnalysisService.class);

    private final IndexDataService indexDataService;
    private final WinRateCalculator winRateCalculator;
    private final IndexAnalysisResultMapper resultMapper;
    private final ProviderHealthTracker healthTracker;
    private final AnalysisProperties properties;

    public InvestmentAnalysisService(IndexDataService indexDataService,
                                     WinRateCalculator winRateCalculator,
                                     IndexAnalysisResultMapper resultMapper,
                                     ProviderHealthTracker healthTracker,
                                     AnalysisProperties properties) {
        this.indexDataService = indexDataService;
        this.winRateCalculator = winRateCalculator;
        this.resultMapper = resultMapper;
        this.healthTracker = healthTracker;
        this.properties = properties;
    }

    /**
     * 计算三大指数最新胜率并返回汇总结果。
     *
     * @param forceRefresh true=强制走接口刷新数据
     */
    public WinRateSummaryVO analyze(boolean forceRefresh) {
        if (forceRefresh) {
            healthTracker.resetAll();
        }

        List<IndexWinRateVO> items = new ArrayList<IndexWinRateVO>();
        Set<String> sourceLabels = new LinkedHashSet<String>();
        boolean allFromDatabase = true;

        for (IndexDefinition definition : IndexDefinition.values()) {
            IndexDataBundle bundle = indexDataService.loadWindow(definition, forceRefresh);
            List<IndexDaily> series = bundle.getSeries();
            IndexWinRateVO vo = winRateCalculator.calculate(definition, series);
            vo.setDataSource(bundle.getDataSource());
            vo.setDataSourceLabel(toSourceLabel(bundle.getDataSource()));
            vo.setFromDatabase(bundle.isFromDatabase());
            items.add(vo);
            allFromDatabase = allFromDatabase && bundle.isFromDatabase();
            sourceLabels.add(bundle.isFromDatabase() ? "数据库" : vo.getDataSourceLabel() + "接口");

            try {
                resultMapper.upsertResult(toEntity(vo));
            } catch (Exception e) {
                // 结果落库失败不影响本次分析返回
                log.warn("指数 {} 分析结果落库失败：{}", definition.getCode(), e.getMessage());
            }
        }

        return buildSummary(items, sourceLabels, allFromDatabase);
    }

    private WinRateSummaryVO buildSummary(List<IndexWinRateVO> items,
                                          Set<String> sourceLabels,
                                          boolean allFromDatabase) {
        WinRateSummaryVO summary = new WinRateSummaryVO();
        summary.setGeneratedAt(LocalDateTime.now());
        summary.setWindowDays(properties.getWindowDays());
        summary.setChangeWindowDays(properties.getChangeWindowDays());
        summary.setMinWinRate(BigDecimal.valueOf(properties.getMinWinRate()));
        summary.setMaxWinRate(BigDecimal.valueOf(properties.getMaxWinRate()));
        summary.setChangeMultiplier(BigDecimal.valueOf(properties.getChangeMultiplier()));
        summary.setClampWinRate(properties.isClampWinRate());
        summary.setFormula("基础胜率 = " + trim(properties.getMaxWinRate()) + "% + ("
                + trim(properties.getMinWinRate()) + "% - " + trim(properties.getMaxWinRate()) + "%) × (最高值 - 最新值) ÷ (最高值 - 最低值)；"
                + "最终胜率 = 基础胜率 " + (properties.isInverseChange() ? "-" : "+")
                + " 7日涨跌幅(%) × " + trim(properties.getChangeMultiplier())
                + (properties.isInverseChange() ? "（指数上涨则扣减胜率、下跌则增加胜率）" : "（指数上涨则增加胜率）")
                + (properties.isClampWinRate() ? "（结果限制在 0% ~ 100%）" : ""));
        summary.setItems(items);

        BigDecimal total = BigDecimal.ZERO;
        IndexWinRateVO strongest = items.get(0);
        IndexWinRateVO weakest = items.get(0);
        int bullish = 0;
        for (IndexWinRateVO item : items) {
            total = total.add(item.getWinRate());
            if (item.getWinRate().compareTo(strongest.getWinRate()) > 0) {
                strongest = item;
            }
            if (item.getWinRate().compareTo(weakest.getWinRate()) < 0) {
                weakest = item;
            }
            if (item.getWinRate().doubleValue() >= 50D) {
                bullish++;
            }
        }
        summary.setAverageWinRate(total.divide(BigDecimal.valueOf(items.size()), 4, RoundingMode.HALF_UP));
        summary.setStrongestName(strongest.getName());
        summary.setWeakestName(weakest.getName());
        summary.setBullishCount(bullish);
        summary.setAllFromDatabase(allFromDatabase);
        summary.setDataSourceSummary(join(sourceLabels));
        return summary;
    }

    private String join(Set<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append(label);
        }
        return builder.toString();
    }

    private String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String toSourceLabel(String source) {
        if (source == null) {
            return "未知";
        }
        switch (source) {
            case "EASTMONEY":
                return "东方财富";
            case "TENCENT":
                return "腾讯财经";
            case "SINA":
                return "新浪财经";
            case "DATABASE":
                return "本地数据库";
            default:
                return source;
        }
    }

    private IndexAnalysisResult toEntity(IndexWinRateVO vo) {
        IndexAnalysisResult entity = new IndexAnalysisResult();
        entity.setIndexCode(vo.getCode());
        entity.setIndexName(vo.getName());
        entity.setLatestTradeDate(vo.getLatestTradeDate());
        entity.setLatestClose(vo.getLatestClose());
        entity.setPeriodStart(vo.getPeriodStart());
        entity.setPeriodEnd(vo.getPeriodEnd());
        entity.setSampleCount(vo.getSampleCount());
        entity.setMinClose(vo.getMinClose());
        entity.setMaxClose(vo.getMaxClose());
        entity.setBaseWinRate(vo.getBaseWinRate());
        entity.setCompareTradeDate(vo.getCompareTradeDate());
        entity.setCompareClose(vo.getCompareClose());
        entity.setChangePercent(vo.getChangePercent());
        entity.setChangeAdjust(vo.getChangeAdjust());
        entity.setRawWinRate(vo.getRawWinRate());
        entity.setFinalWinRate(vo.getWinRate());
        entity.setDataSource(vo.getDataSource());
        return entity;
    }

    /**
     * 查询指定指数最近 days 天的走势数据（用于折线图）。
     */
    public List<TrendPointVO> trend(String code, int days) {
        IndexDefinition definition = IndexDefinition.ofCode(code);
        List<IndexDaily> series = indexDataService.loadTrend(definition, days);
        List<TrendPointVO> points = new ArrayList<TrendPointVO>(series.size());
        for (IndexDaily item : series) {
            points.add(new TrendPointVO(item.getTradeDate(), item.getClosePrice(), null));
        }
        return points;
    }

    /**
     * 查询历史分析结果（用于历史胜率表格）。
     */
    public List<IndexAnalysisResult> history(int limit) {
        return resultMapper.selectHistory(Math.max(1, Math.min(limit, 200)));
    }

    /**
     * 查询最近一次分析快照。
     */
    public List<IndexAnalysisResult> latestSnapshot() {
        return resultMapper.selectLatestSnapshot();
    }
}
