package com.investment.analysis.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.investment.analysis.common.IndexDataException;
import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.config.HoldingProperties;
import com.investment.analysis.entity.HoldingBuyRecord;
import com.investment.analysis.entity.HoldingPosition;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.mapper.HoldingBuyRecordMapper;
import com.investment.analysis.mapper.HoldingPositionMapper;
import com.investment.analysis.mapper.IndexDailyMapper;
import com.investment.analysis.model.HoldingBuyRecordVO;
import com.investment.analysis.model.HoldingBuyRequest;
import com.investment.analysis.model.HoldingOverviewVO;
import com.investment.analysis.model.HoldingPositionVO;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.model.IndexQuoteVO;
import com.investment.analysis.model.IndexWinRateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 持仓管理服务。
 * <pre>
 * 录入：用户输入「持仓名称 + 具体日期 + 买入金额」，系统按该日期回溯行情算出
 *       买入成本（当日指数收盘点位）与买入时的预测胜率，一并落库；
 * 展示：按最新点位判断每笔买入与每个持仓当前是盈还是亏，并统计
 *       真实胜率（当前盈利笔数 ÷ 买入次数）、预测胜率（每笔预测胜率之和 ÷ 买入次数）、
 *       以及预测胜率 >= 70% 的买入对应的真实胜率。
 * </pre>
 */
@Service
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);

    private final HoldingPositionMapper positionMapper;
    private final HoldingBuyRecordMapper recordMapper;
    private final IndexDailyMapper indexDailyMapper;
    private final IndexDataService indexDataService;
    private final WinRateCalculator winRateCalculator;
    private final HoldingCalculator holdingCalculator;
    private final HoldingProperties properties;
    private final AnalysisProperties analysisProperties;

    public HoldingService(HoldingPositionMapper positionMapper,
                          HoldingBuyRecordMapper recordMapper,
                          IndexDailyMapper indexDailyMapper,
                          IndexDataService indexDataService,
                          WinRateCalculator winRateCalculator,
                          HoldingCalculator holdingCalculator,
                          HoldingProperties properties,
                          AnalysisProperties analysisProperties) {
        this.positionMapper = positionMapper;
        this.recordMapper = recordMapper;
        this.indexDailyMapper = indexDailyMapper;
        this.indexDataService = indexDataService;
        this.winRateCalculator = winRateCalculator;
        this.holdingCalculator = holdingCalculator;
        this.properties = properties;
        this.analysisProperties = analysisProperties;
    }

    /**
     * 持仓总览：持仓列表（含每笔买入明细）+ 汇总统计 + 三大指数最新行情。
     *
     * @param forceRefresh 是否强制调用行情接口刷新最新点位
     */
    public HoldingOverviewVO overview(boolean forceRefresh) {
        List<HoldingPosition> positions = positionMapper.selectList(
                new QueryWrapper<HoldingPosition>().orderByAsc("id"));
        List<HoldingBuyRecord> records = recordMapper.selectAllOrdered();

        Map<Long, List<HoldingBuyRecord>> grouped = new LinkedHashMap<Long, List<HoldingBuyRecord>>();
        for (HoldingBuyRecord record : records) {
            List<HoldingBuyRecord> list = grouped.get(record.getPositionId());
            if (list == null) {
                list = new ArrayList<HoldingBuyRecord>();
                grouped.put(record.getPositionId(), list);
            }
            list.add(record);
        }

        Map<String, IndexQuoteVO> quoteMap = loadQuotes(positions, forceRefresh);

        List<HoldingPositionVO> positionVOs = new ArrayList<HoldingPositionVO>(positions.size());
        List<HoldingBuyRecordVO> recordVOs = new ArrayList<HoldingBuyRecordVO>();
        for (HoldingPosition position : positions) {
            List<HoldingBuyRecord> own = grouped.get(position.getId());
            IndexQuoteVO quote = quoteMap.get(position.getIndexCode());
            BigDecimal latestClose = quote != null && quote.isAvailable() ? quote.getLatestClose() : null;
            LocalDate latestDate = quote != null && quote.isAvailable() ? quote.getLatestTradeDate() : null;
            HoldingPositionVO vo = holdingCalculator.buildPosition(position, own, latestClose, latestDate);
            positionVOs.add(vo);
            recordVOs.addAll(vo.getRecords());
        }

        HoldingOverviewVO overview = new HoldingOverviewVO();
        overview.setGeneratedAt(LocalDateTime.now());
        overview.setPositions(positionVOs);
        overview.setRecords(recordVOs);
        overview.setStats(holdingCalculator.buildStats(positionVOs, recordVOs));
        overview.setQuotes(new ArrayList<IndexQuoteVO>(quoteMap.values()));
        overview.setQuoteSummary(buildQuoteSummary(quoteMap));
        overview.setWinRateRule(buildWinRateRule());
        overview.setProfitRule("盈亏判断：以关联指数最新点位为「最新价格」，"
                + "单笔盈亏 = 买入金额 ×（最新点位 − 买入成本）÷ 买入成本；"
                + "持仓盈亏 = Σ(买入金额 ÷ 成本点位) × 最新点位 − 累计买入金额（加权平均成本口径）");
        return overview;
    }

    /**
     * 新增一笔买入：持仓名称相同且关联指数相同时自动合并到已有持仓。
     */
    @Transactional(rollbackFor = Exception.class)
    public HoldingOverviewVO addBuy(HoldingBuyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请填写买入信息");
        }
        String positionName = trimToNull(request.getPositionName());
        if (positionName == null) {
            throw new IllegalArgumentException("请填写持仓名称");
        }
        if (positionName.length() > properties.getMaxPositionNameLength()) {
            throw new IllegalArgumentException("持仓名称最长 "
                    + properties.getMaxPositionNameLength() + " 个字符");
        }
        String indexCode = trimToNull(request.getIndexCode());
        if (indexCode == null) {
            throw new IllegalArgumentException("请选择持仓关联的指数");
        }
        IndexDefinition definition = IndexDefinition.ofCode(indexCode);

        LocalDate buyDate = request.getBuyDate();
        if (buyDate == null) {
            throw new IllegalArgumentException("请选择具体的买入日期");
        }
        if (buyDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("买入日期不能晚于今天（" + LocalDate.now() + "）");
        }

        BigDecimal amount = request.getBuyAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("请输入大于 0 的买入金额");
        }
        if (amount.compareTo(BigDecimal.valueOf(properties.getMaxBuyAmount())) > 0) {
            throw new IllegalArgumentException("单笔买入金额不能超过 "
                    + properties.getMaxBuyAmount() + " 元");
        }
        BigDecimal buyAmount = amount.setScale(2, RoundingMode.HALF_UP);

        HoldingPosition position = resolvePosition(request, positionName, definition);

        // 成本点位与买入时预测胜率：取「买入日期之前（含当日）最近的一个交易日」的收盘点位
        List<IndexDaily> series = loadSeriesUpTo(definition, buyDate);
        IndexDaily costBar = series.get(series.size() - 1);

        HoldingBuyRecord record = new HoldingBuyRecord();
        record.setPositionId(position.getId());
        record.setBuyDate(buyDate);
        record.setCostTradeDate(costBar.getTradeDate());
        record.setCostPrice(costBar.getClosePrice());
        record.setBuyAmount(buyAmount);

        StringBuilder note = new StringBuilder();
        if (!costBar.getTradeDate().equals(buyDate)) {
            note.append("买入日非交易日，成本与预测胜率取最近交易日 ").append(costBar.getTradeDate());
        }
        if (series.size() >= Math.max(2, properties.getMinWinRateSamples())) {
            IndexWinRateVO winRate = winRateCalculator.calculate(definition, series);
            record.setBuyWinRate(winRate.getWinRate());
            record.setWinRatePeriodStart(winRate.getPeriodStart());
            record.setWinRatePeriodEnd(winRate.getPeriodEnd());
            record.setWinRateSampleCount(winRate.getSampleCount());
            record.setWinRateFormula(buildWinRateFormula(winRate));
        } else {
            record.setWinRateSampleCount(series.size());
            record.setWinRatePeriodStart(series.get(0).getTradeDate());
            record.setWinRatePeriodEnd(costBar.getTradeDate());
            if (note.length() > 0) {
                note.append("；");
            }
            note.append("可用行情仅 ").append(series.size()).append(" 个交易日（不足 ")
                    .append(properties.getMinWinRateSamples()).append(" 个），未计算预测胜率");
        }
        if (note.length() > 0) {
            record.setWinRateNote(note.length() > 255 ? note.substring(0, 255) : note.toString());
        }
        recordMapper.insert(record);
        log.info("持仓 {}（{}）新增买入：{} {} 元，成本 {}，买入时预测胜率 {}",
                position.getPositionName(), definition.getName(), buyDate, buyAmount.toPlainString(),
                costBar.getClosePrice(), record.getBuyWinRate());
        return overview(false);
    }

    /**
     * 删除持仓及其全部买入记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public HoldingOverviewVO deletePosition(Long positionId) {
        if (positionId == null) {
            throw new IllegalArgumentException("请指定要删除的持仓");
        }
        HoldingPosition position = positionMapper.selectById(positionId);
        if (position == null) {
            throw new IllegalArgumentException("持仓不存在或已被删除");
        }
        recordMapper.deleteByPositionId(positionId);
        positionMapper.deleteById(positionId);
        log.info("已删除持仓 {}（{}）及其全部买入记录", position.getPositionName(), position.getIndexCode());
        return overview(false);
    }

    /**
     * 删除单笔买入记录；该持仓下已无买入记录时，持仓一并删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public HoldingOverviewVO deleteRecord(Long recordId) {
        if (recordId == null) {
            throw new IllegalArgumentException("请指定要删除的买入记录");
        }
        HoldingBuyRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("买入记录不存在或已被删除");
        }
        recordMapper.deleteById(recordId);
        if (recordMapper.countByPositionId(record.getPositionId()) == 0) {
            positionMapper.deleteById(record.getPositionId());
        }
        return overview(false);
    }

    /**
     * 强制刷新三大指数最新点位后重建总览。
     */
    public HoldingOverviewVO refreshQuotes() {
        return overview(true);
    }

    private HoldingPosition resolvePosition(HoldingBuyRequest request,
                                            String positionName,
                                            IndexDefinition definition) {
        if (request.getPositionId() != null) {
            HoldingPosition existing = positionMapper.selectById(request.getPositionId());
            if (existing == null) {
                throw new IllegalArgumentException("指定的持仓不存在");
            }
            return existing;
        }
        HoldingPosition existing = positionMapper.selectByNameAndIndex(positionName, definition.getCode());
        if (existing != null) {
            return existing;
        }
        HoldingPosition position = new HoldingPosition();
        position.setPositionName(positionName);
        position.setIndexCode(definition.getCode());
        position.setIndexName(definition.getName());
        position.setRemark(trimToNull(request.getRemark()));
        positionMapper.insert(position);
        return position;
    }

    /**
     * 取「买入日期之前（含当日）」的取样窗口行情：窗口长度与投资分析模块一致（默认 365 天）。
     * <p>库里没有该区间数据时先尝试调用行情接口补数；仍无数据则提示可用的最早日期。</p>
     */
    private List<IndexDaily> loadSeriesUpTo(IndexDefinition definition, LocalDate buyDate) {
        LocalDate start = buyDate.minusDays(Math.max(2, analysisProperties.getWindowDays()) - 1L);
        List<IndexDaily> series = indexDailyMapper.selectRange(definition.getCode(), start, buyDate);
        if (series.isEmpty()) {
            try {
                indexDataService.loadWindow(definition, false);
            } catch (IndexDataException e) {
                log.warn("补全 {} 行情数据失败：{}", definition.getCode(), e.getMessage());
            }
            series = indexDailyMapper.selectRange(definition.getCode(), start, buyDate);
        }
        if (series.isEmpty()) {
            LocalDate earliest = indexDailyMapper.selectEarliestTradeDate(definition.getCode());
            if (earliest == null) {
                throw new IllegalArgumentException("暂无 " + definition.getName()
                        + " 的行情数据，请先在「投资分析」模块刷新数据后再录入持仓");
            }
            throw new IllegalArgumentException("买入日期 " + buyDate + " 早于系统可用的行情数据（最早 "
                    + earliest + "），无法取得买入成本与买入时的预测胜率");
        }
        return series;
    }

    /**
     * 装载三大指数最新行情；单个指数失败不影响其它指数与页面展示。
     */
    private Map<String, IndexQuoteVO> loadQuotes(List<HoldingPosition> positions, boolean forceRefresh) {
        Map<String, IndexQuoteVO> quotes = new LinkedHashMap<String, IndexQuoteVO>();
        for (IndexDefinition definition : IndexDefinition.values()) {
            IndexQuoteVO quote = new IndexQuoteVO();
            quote.setCode(definition.getCode());
            quote.setName(definition.getName());
            quote.setUsed(false);
            for (HoldingPosition position : positions) {
                if (definition.getCode().equals(position.getIndexCode())) {
                    quote.setUsed(true);
                    break;
                }
            }
            try {
                IndexDataBundle bundle = indexDataService.loadWindow(definition, forceRefresh);
                List<IndexDaily> series = bundle.getSeries();
                IndexDaily latest = series.get(series.size() - 1);
                quote.setLatestTradeDate(latest.getTradeDate());
                quote.setLatestClose(latest.getClosePrice().setScale(3, RoundingMode.HALF_UP));
                quote.setDataSourceLabel(bundle.isFromDatabase()
                        ? "本地数据库" : toSourceLabel(bundle.getDataSource()) + "接口");
                quote.setAvailable(true);
            } catch (Exception e) {
                quote.setAvailable(false);
                quote.setMessage(e.getMessage());
                log.warn("指数 {} 最新行情不可用：{}", definition.getCode(), e.getMessage());
            }
            quotes.put(definition.getCode(), quote);
        }
        return quotes;
    }

    private String buildQuoteSummary(Map<String, IndexQuoteVO> quotes) {
        StringBuilder builder = new StringBuilder();
        for (IndexQuoteVO quote : quotes.values()) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(quote.getName()).append(" ");
            if (quote.isAvailable()) {
                builder.append(quote.getLatestTradeDate()).append(" 收盘 ")
                        .append(quote.getLatestClose().toPlainString()).append("（")
                        .append(quote.getDataSourceLabel()).append("）");
            } else {
                builder.append("行情不可用");
            }
        }
        return builder.toString();
    }

    private String buildWinRateRule() {
        return "买入时预测胜率：取该买入日期之前（含当日）最近交易日为止的最近 "
                + analysisProperties.getWindowDays() + " 天行情，按投资分析模块同一套规则计算"
                + "（基础胜率 = " + trim(analysisProperties.getMaxWinRate()) + "% + ("
                + trim(analysisProperties.getMinWinRate()) + "% - " + trim(analysisProperties.getMaxWinRate())
                + "%) × (最高值 - 最新值) ÷ (最高值 - 最低值)"
                + (analysisProperties.isInverseChange() ? " - " : " + ")
                + "7日涨跌幅(%) × " + trim(analysisProperties.getChangeMultiplier()) + "）；"
                + "持仓预测胜率 = 每笔买入的预测胜率之和 ÷ 买入次数；"
                + "真实胜率 = 按最新点位判断当前盈利的买入笔数 ÷ 买入次数；"
                + "高预测胜率买入 = 买入时预测胜率 ≥ "
                + trim(properties.getHighWinRateThreshold()) + "% 的买入，其真实胜率单独统计";
    }

    private String buildWinRateFormula(IndexWinRateVO winRate) {
        StringBuilder builder = new StringBuilder();
        if (winRate.getBaseFormula() != null) {
            builder.append("基础胜率：").append(winRate.getBaseFormula()).append("；");
        }
        if (winRate.getFinalFormula() != null) {
            builder.append("最终胜率：").append(winRate.getFinalFormula());
        }
        String text = builder.toString();
        return text.length() > 512 ? text.substring(0, 512) : text;
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

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
