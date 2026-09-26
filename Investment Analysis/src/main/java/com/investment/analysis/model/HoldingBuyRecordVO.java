package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单笔买入记录展示对象：买入信息 + 成本 + 买入时预测胜率 + 按最新点位算出的盈亏。
 */
public class HoldingBuyRecordVO {

    private Long id;
    private Long positionId;
    private String positionName;

    /** 用户输入的买入日期 */
    private LocalDate buyDate;
    /** 实际取用成本对应的交易日（买入日非交易日时向前取最近交易日） */
    private LocalDate costTradeDate;
    /** 买入金额（元） */
    private BigDecimal buyAmount;
    /** 买入成本：该交易日指数收盘点位 */
    private BigDecimal costPrice;

    /** 买入时的预测胜率（%），数据不足时为 null */
    private BigDecimal buyWinRate;
    private LocalDate winRatePeriodStart;
    private LocalDate winRatePeriodEnd;
    private Integer winRateSampleCount;
    private String winRateFormula;
    private String winRateNote;

    /** 最新交易日与最新点位 */
    private LocalDate latestTradeDate;
    private BigDecimal latestClose;
    /** 相对成本点位的涨跌幅（%） */
    private BigDecimal changePercent;
    /** 该笔买入当前的盈亏金额（元） */
    private BigDecimal profitAmount;
    /** 是否盈利（可用于统计真实胜率） */
    private boolean win;
    /** 结果标签：盈利 / 亏损 / 持平 */
    private String resultLabel;
    /** 预测命中：买入时预测胜率 >= 70% 且当前盈利 */
    private boolean highWinRateHit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public LocalDate getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDate buyDate) {
        this.buyDate = buyDate;
    }

    public LocalDate getCostTradeDate() {
        return costTradeDate;
    }

    public void setCostTradeDate(LocalDate costTradeDate) {
        this.costTradeDate = costTradeDate;
    }

    public BigDecimal getBuyAmount() {
        return buyAmount;
    }

    public void setBuyAmount(BigDecimal buyAmount) {
        this.buyAmount = buyAmount;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getBuyWinRate() {
        return buyWinRate;
    }

    public void setBuyWinRate(BigDecimal buyWinRate) {
        this.buyWinRate = buyWinRate;
    }

    public LocalDate getWinRatePeriodStart() {
        return winRatePeriodStart;
    }

    public void setWinRatePeriodStart(LocalDate winRatePeriodStart) {
        this.winRatePeriodStart = winRatePeriodStart;
    }

    public LocalDate getWinRatePeriodEnd() {
        return winRatePeriodEnd;
    }

    public void setWinRatePeriodEnd(LocalDate winRatePeriodEnd) {
        this.winRatePeriodEnd = winRatePeriodEnd;
    }

    public Integer getWinRateSampleCount() {
        return winRateSampleCount;
    }

    public void setWinRateSampleCount(Integer winRateSampleCount) {
        this.winRateSampleCount = winRateSampleCount;
    }

    public String getWinRateFormula() {
        return winRateFormula;
    }

    public void setWinRateFormula(String winRateFormula) {
        this.winRateFormula = winRateFormula;
    }

    public String getWinRateNote() {
        return winRateNote;
    }

    public void setWinRateNote(String winRateNote) {
        this.winRateNote = winRateNote;
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

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    public void setProfitAmount(BigDecimal profitAmount) {
        this.profitAmount = profitAmount;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public String getResultLabel() {
        return resultLabel;
    }

    public void setResultLabel(String resultLabel) {
        this.resultLabel = resultLabel;
    }

    public boolean isHighWinRateHit() {
        return highWinRateHit;
    }

    public void setHighWinRateHit(boolean highWinRateHit) {
        this.highWinRateHit = highWinRateHit;
    }
}
