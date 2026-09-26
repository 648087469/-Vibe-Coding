package com.investment.analysis.model;

import java.math.BigDecimal;

/**
 * 风险评估汇总统计。
 */
public class RiskStatsVO {

    /** 参与评估的持仓方案数量 */
    private int planCount;
    /** 建议止盈的方案数（盈亏比例 >= 首个止盈档） */
    private int takeProfitCount;
    /** 建议止损的方案数（盈亏比例 <= 首个止损档） */
    private int stopLossCount;
    /** 持有观察的方案数（未触发任何档位） */
    private int watchCount;
    /** 已到「全部卖出」档的方案数 */
    private int liquidationCount;
    /** 行情不可用、无法评估的方案数 */
    private int unavailableCount;

    /** 当前建议卖出金额合计（元） */
    private BigDecimal suggestedSellAmount;
    /** 累计投入 / 当前市值 / 当前盈亏 */
    private BigDecimal totalAmount;
    private BigDecimal marketValue;
    private BigDecimal profitAmount;
    private BigDecimal profitPercent;

    /** 止盈首个触发点 / 止损首个触发点（%） */
    private BigDecimal takeProfitTrigger;
    private BigDecimal stopLossTrigger;
    /** 止盈全部卖出触发点 / 止损全部卖出触发点（%，阶梯最后一档） */
    private BigDecimal takeProfitClearTrigger;
    private BigDecimal stopLossClearTrigger;

    public int getPlanCount() {
        return planCount;
    }

    public void setPlanCount(int planCount) {
        this.planCount = planCount;
    }

    public int getTakeProfitCount() {
        return takeProfitCount;
    }

    public void setTakeProfitCount(int takeProfitCount) {
        this.takeProfitCount = takeProfitCount;
    }

    public int getStopLossCount() {
        return stopLossCount;
    }

    public void setStopLossCount(int stopLossCount) {
        this.stopLossCount = stopLossCount;
    }

    public int getWatchCount() {
        return watchCount;
    }

    public void setWatchCount(int watchCount) {
        this.watchCount = watchCount;
    }

    public int getLiquidationCount() {
        return liquidationCount;
    }

    public void setLiquidationCount(int liquidationCount) {
        this.liquidationCount = liquidationCount;
    }

    public int getUnavailableCount() {
        return unavailableCount;
    }

    public void setUnavailableCount(int unavailableCount) {
        this.unavailableCount = unavailableCount;
    }

    public BigDecimal getSuggestedSellAmount() {
        return suggestedSellAmount;
    }

    public void setSuggestedSellAmount(BigDecimal suggestedSellAmount) {
        this.suggestedSellAmount = suggestedSellAmount;
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

    public BigDecimal getTakeProfitTrigger() {
        return takeProfitTrigger;
    }

    public void setTakeProfitTrigger(BigDecimal takeProfitTrigger) {
        this.takeProfitTrigger = takeProfitTrigger;
    }

    public BigDecimal getStopLossTrigger() {
        return stopLossTrigger;
    }

    public void setStopLossTrigger(BigDecimal stopLossTrigger) {
        this.stopLossTrigger = stopLossTrigger;
    }

    public BigDecimal getTakeProfitClearTrigger() {
        return takeProfitClearTrigger;
    }

    public void setTakeProfitClearTrigger(BigDecimal takeProfitClearTrigger) {
        this.takeProfitClearTrigger = takeProfitClearTrigger;
    }

    public BigDecimal getStopLossClearTrigger() {
        return stopLossClearTrigger;
    }

    public void setStopLossClearTrigger(BigDecimal stopLossClearTrigger) {
        this.stopLossClearTrigger = stopLossClearTrigger;
    }
}
