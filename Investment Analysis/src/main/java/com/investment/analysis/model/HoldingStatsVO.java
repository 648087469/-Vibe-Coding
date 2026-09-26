package com.investment.analysis.model;

import java.math.BigDecimal;

/**
 * 持仓管理汇总统计：覆盖全部持仓的投入、市值、盈亏、真实胜率与预测胜率。
 */
public class HoldingStatsVO {

    /** 持仓数量 */
    private int positionCount;
    /** 买入总次数 */
    private int buyCount;
    /** 累计投入金额（元） */
    private BigDecimal totalAmount;
    /** 当前市值（元） */
    private BigDecimal marketValue;
    /** 累计盈亏（元）与盈亏比例（%） */
    private BigDecimal profitAmount;
    private BigDecimal profitPercent;

    /** 真实胜率：当前盈利的买入笔数 ÷ 买入总次数 */
    private BigDecimal realWinRate;
    private int winCount;
    private int lossCount;

    /** 预测胜率：每笔买入的预测胜率之和 ÷ 买入总次数 */
    private BigDecimal predictedWinRate;
    /** 参与预测胜率统计的买入笔数（买入时成功算出预测胜率的笔数） */
    private int predictedRecordCount;

    /** 高预测胜率阈值（%） */
    private BigDecimal highWinRateThreshold;
    /** 预测胜率 >= 阈值的买入笔数、真实胜率、平均预测胜率 */
    private int highWinRateCount;
    private BigDecimal highWinRateRealWinRate;
    private BigDecimal highWinRatePredictedWinRate;
    /** 预测胜率 < 阈值的买入笔数、真实胜率 */
    private int lowWinRateCount;
    private BigDecimal lowWinRateRealWinRate;

    public int getPositionCount() {
        return positionCount;
    }

    public void setPositionCount(int positionCount) {
        this.positionCount = positionCount;
    }

    public int getBuyCount() {
        return buyCount;
    }

    public void setBuyCount(int buyCount) {
        this.buyCount = buyCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getProfitAmount() {
        return profitAmount;
    }

    public void setProfitAmount(BigDecimal profitAmount) {
        this.profitAmount = profitAmount;
    }

    public BigDecimal getProfitPercent() {
        return profitPercent;
    }

    public void setProfitPercent(BigDecimal profitPercent) {
        this.profitPercent = profitPercent;
    }

    public BigDecimal getRealWinRate() {
        return realWinRate;
    }

    public void setRealWinRate(BigDecimal realWinRate) {
        this.realWinRate = realWinRate;
    }

    public int getWinCount() {
        return winCount;
    }

    public void setWinCount(int winCount) {
        this.winCount = winCount;
    }

    public int getLossCount() {
        return lossCount;
    }

    public void setLossCount(int lossCount) {
        this.lossCount = lossCount;
    }

    public BigDecimal getPredictedWinRate() {
        return predictedWinRate;
    }

    public void setPredictedWinRate(BigDecimal predictedWinRate) {
        this.predictedWinRate = predictedWinRate;
    }

    public int getPredictedRecordCount() {
        return predictedRecordCount;
    }

    public void setPredictedRecordCount(int predictedRecordCount) {
        this.predictedRecordCount = predictedRecordCount;
    }

    public BigDecimal getHighWinRateThreshold() {
        return highWinRateThreshold;
    }

    public void setHighWinRateThreshold(BigDecimal highWinRateThreshold) {
        this.highWinRateThreshold = highWinRateThreshold;
    }

    public int getHighWinRateCount() {
        return highWinRateCount;
    }

    public void setHighWinRateCount(int highWinRateCount) {
        this.highWinRateCount = highWinRateCount;
    }

    public BigDecimal getHighWinRateRealWinRate() {
        return highWinRateRealWinRate;
    }

    public void setHighWinRateRealWinRate(BigDecimal highWinRateRealWinRate) {
        this.highWinRateRealWinRate = highWinRateRealWinRate;
    }

    public BigDecimal getHighWinRatePredictedWinRate() {
        return highWinRatePredictedWinRate;
    }

    public void setHighWinRatePredictedWinRate(BigDecimal highWinRatePredictedWinRate) {
        this.highWinRatePredictedWinRate = highWinRatePredictedWinRate;
    }

    public int getLowWinRateCount() {
        return lowWinRateCount;
    }

    public void setLowWinRateCount(int lowWinRateCount) {
        this.lowWinRateCount = lowWinRateCount;
    }

    public BigDecimal getLowWinRateRealWinRate() {
        return lowWinRateRealWinRate;
    }

    public void setLowWinRateRealWinRate(BigDecimal lowWinRateRealWinRate) {
        this.lowWinRateRealWinRate = lowWinRateRealWinRate;
    }
}
