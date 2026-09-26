package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单个指数的胜率分析结果（前端表格与柱形图共用）。
 */
public class IndexWinRateVO {

    private String code;
    private String name;
    private String market;

    /** 最新交易日 */
    private LocalDate latestTradeDate;
    /** 最新指数数值（收盘） */
    private BigDecimal latestClose;

    /** 取样窗口起止日期 */
    private LocalDate periodStart;
    private LocalDate periodEnd;
    /** 窗口内样本数量（交易日数） */
    private int sampleCount;

    /** 窗口内最低指数数值及其出现日期 */
    private BigDecimal minClose;
    private LocalDate minCloseDate;
    /** 窗口内最高指数数值及其出现日期 */
    private BigDecimal maxClose;
    private LocalDate maxCloseDate;
    /** 最新值在 [最低, 最高] 区间中的位置，0=最低，1=最高 */
    private BigDecimal positionRatio;
    /** 距离窗口最低点、最高点的百分比 */
    private BigDecimal fromMinPercent;
    private BigDecimal fromMaxPercent;

    /** 基础胜率：最低点 90%，最高点 10%，线性均分 */
    private BigDecimal baseWinRate;

    /** 对比基准（7 天前的交易日）*/
    private LocalDate compareTradeDate;
    private BigDecimal compareClose;
    /** 最新一天对比 7 天前的涨跌幅（%），负数表示下跌 */
    private BigDecimal changePercent;
    /** 涨跌幅调整项：涨跌幅(%) × 系数，并按方向取号（反向规则下：涨为负、跌为正） */
    private BigDecimal changeAdjust;

    /** 未裁剪的最终胜率 */
    private BigDecimal rawWinRate;
    /** 最终胜率（%），经 [0,100] 裁剪 */
    private BigDecimal winRate;

    /** 基础胜率计算过程（代值后的算式） */
    private String baseFormula;
    /** 涨跌幅换算过程：涨跌幅(%) × 10 = 调整值 */
    private String changeFormula;
    /** 最终胜率计算过程：基础胜率 + 调整值 = 最终胜率 */
    private String finalFormula;

    /** 估值区间标签 */
    private String valueLevel;

    private String dataSource;
    private String dataSourceLabel;
    /** 本次分析的数据是否直接读自数据库（true=命中库，false=调用接口后入库） */
    private boolean fromDatabase;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public LocalDate getLatestTradeDate() {
        return latestTradeDate;
    }

    public void setLatestTradeDate(LocalDate latestTradeDate) {
        this.latestTradeDate = latestTradeDate;
    }

    public BigDecimal getLatestClose() {
        return latestClose;
    }

    public void setLatestClose(BigDecimal latestClose) {
        this.latestClose = latestClose;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public BigDecimal getMinClose() {
        return minClose;
    }

    public void setMinClose(BigDecimal minClose) {
        this.minClose = minClose;
    }

    public LocalDate getMinCloseDate() {
        return minCloseDate;
    }

    public void setMinCloseDate(LocalDate minCloseDate) {
        this.minCloseDate = minCloseDate;
    }

    public BigDecimal getMaxClose() {
        return maxClose;
    }

    public void setMaxClose(BigDecimal maxClose) {
        this.maxClose = maxClose;
    }

    public LocalDate getMaxCloseDate() {
        return maxCloseDate;
    }

    public void setMaxCloseDate(LocalDate maxCloseDate) {
        this.maxCloseDate = maxCloseDate;
    }

    public BigDecimal getPositionRatio() {
        return positionRatio;
    }

    public void setPositionRatio(BigDecimal positionRatio) {
        this.positionRatio = positionRatio;
    }

    public BigDecimal getFromMinPercent() {
        return fromMinPercent;
    }

    public void setFromMinPercent(BigDecimal fromMinPercent) {
        this.fromMinPercent = fromMinPercent;
    }

    public BigDecimal getFromMaxPercent() {
        return fromMaxPercent;
    }

    public void setFromMaxPercent(BigDecimal fromMaxPercent) {
        this.fromMaxPercent = fromMaxPercent;
    }

    public BigDecimal getBaseWinRate() {
        return baseWinRate;
    }

    public void setBaseWinRate(BigDecimal baseWinRate) {
        this.baseWinRate = baseWinRate;
    }

    public LocalDate getCompareTradeDate() {
        return compareTradeDate;
    }

    public void setCompareTradeDate(LocalDate compareTradeDate) {
        this.compareTradeDate = compareTradeDate;
    }

    public BigDecimal getCompareClose() {
        return compareClose;
    }

    public void setCompareClose(BigDecimal compareClose) {
        this.compareClose = compareClose;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getChangeAdjust() {
        return changeAdjust;
    }

    public void setChangeAdjust(BigDecimal changeAdjust) {
        this.changeAdjust = changeAdjust;
    }

    public BigDecimal getRawWinRate() {
        return rawWinRate;
    }

    public void setRawWinRate(BigDecimal rawWinRate) {
        this.rawWinRate = rawWinRate;
    }

    public BigDecimal getWinRate() {
        return winRate;
    }

    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }

    public String getBaseFormula() {
        return baseFormula;
    }

    public void setBaseFormula(String baseFormula) {
        this.baseFormula = baseFormula;
    }

    public String getChangeFormula() {
        return changeFormula;
    }

    public void setChangeFormula(String changeFormula) {
        this.changeFormula = changeFormula;
    }

    public String getFinalFormula() {
        return finalFormula;
    }

    public void setFinalFormula(String finalFormula) {
        this.finalFormula = finalFormula;
    }

    public String getValueLevel() {
        return valueLevel;
    }

    public void setValueLevel(String valueLevel) {
        this.valueLevel = valueLevel;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getDataSourceLabel() {
        return dataSourceLabel;
    }

    public void setDataSourceLabel(String dataSourceLabel) {
        this.dataSourceLabel = dataSourceLabel;
    }

    public boolean isFromDatabase() {
        return fromDatabase;
    }

    public void setFromDatabase(boolean fromDatabase) {
        this.fromDatabase = fromDatabase;
    }
}
