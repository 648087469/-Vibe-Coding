package com.investment.analysis.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 持仓管理页面主数据：持仓列表 + 汇总统计 + 指数最新行情 + 规则说明。
 */
public class HoldingOverviewVO {

    private LocalDateTime generatedAt;
    /** 全部持仓（含每笔买入明细） */
    private List<HoldingPositionVO> positions = new ArrayList<HoldingPositionVO>();
    /** 全部买入记录（跨持仓汇总，便于表格排序与筛选） */
    private List<HoldingBuyRecordVO> records = new ArrayList<HoldingBuyRecordVO>();
    /** 汇总统计 */
    private HoldingStatsVO stats;
    /** 三大指数最新行情 */
    private List<IndexQuoteVO> quotes = new ArrayList<IndexQuoteVO>();
    /** 行情数据说明 */
    private String quoteSummary;
    /** 预测胜率计算说明 */
    private String winRateRule;
    /** 盈亏计算说明 */
    private String profitRule;

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<HoldingPositionVO> getPositions() {
        return positions;
    }

    public void setPositions(List<HoldingPositionVO> positions) {
        this.positions = positions;
    }

    public List<HoldingBuyRecordVO> getRecords() {
        return records;
    }

    public void setRecords(List<HoldingBuyRecordVO> records) {
        this.records = records;
    }

    public HoldingStatsVO getStats() {
        return stats;
    }

    public void setStats(HoldingStatsVO stats) {
        this.stats = stats;
    }

    public List<IndexQuoteVO> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<IndexQuoteVO> quotes) {
        this.quotes = quotes;
    }

    public String getQuoteSummary() {
        return quoteSummary;
    }

    public void setQuoteSummary(String quoteSummary) {
        this.quoteSummary = quoteSummary;
    }

    public String getWinRateRule() {
        return winRateRule;
    }

    public void setWinRateRule(String winRateRule) {
        this.winRateRule = winRateRule;
    }

    public String getProfitRule() {
        return profitRule;
    }

    public void setProfitRule(String profitRule) {
        this.profitRule = profitRule;
    }
}
