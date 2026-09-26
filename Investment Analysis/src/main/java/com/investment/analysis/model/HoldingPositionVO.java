package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 单个持仓的展示对象：持仓信息 + 买入次数 + 成本 / 最新点位 + 盈亏 + 预测胜率与真实胜率。
 */
public class HoldingPositionVO {

    private Long id;
    private String positionName;
    private String indexCode;
    private String indexName;
    private String remark;

    /** 买入次数 */
    private int buyCount;
    /** 累计买入金额（元） */
    private BigDecimal totalAmount;
    /** 加权平均成本点位（= 累计买入金额 ÷ 累计份额） */
    private BigDecimal avgCostPrice;

    /** 首次买入日期 / 最近一次买入日期 */
    private LocalDate firstBuyDate;
    private LocalDate lastBuyDate;

    /** 最新交易日与最新点位 */
    private LocalDate latestTradeDate;
    private BigDecimal latestClose;
    /** 最新点位相对平均成本的涨跌幅（%） */
    private BigDecimal changePercent;

    /** 当前市值（元）与累计盈亏（元）/ 盈亏比例（%） */
    private BigDecimal marketValue;
    private BigDecimal profitAmount;
    private BigDecimal profitPercent;
    /** 当前是否盈利 */
    private boolean profit;

    /** 预测胜率：每笔买入的预测胜率之和 ÷ 买入次数 */
    private BigDecimal predictedWinRate;
    /** 真实胜率：按最新点位判断当前盈利的买入笔数 ÷ 买入次数 */
    private BigDecimal realWinRate;
    private int winCount;
    private int lossCount;

    /** 预测胜率 >= 阈值的买入笔数及其真实胜率 */
    private int highWinRateCount;
    private BigDecimal highWinRateRealWinRate;
    /** 预测胜率 >= 阈值的买入平均预测胜率 */
    private BigDecimal highWinRatePredictedWinRate;

    /** 明细：该持仓下的每一笔买入 */
    private List<HoldingBuyRecordVO> records = new ArrayList<HoldingBuyRecordVO>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getIndexCode() {
        return indexCode;
    }

    public void setIndexCode(String indexCode) {
        this.indexCode = indexCode;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public BigDecimal getAvgCostPrice() {
        return avgCostPrice;
    }

    public void setAvgCostPrice(BigDecimal avgCostPrice) {
        this.avgCostPrice = avgCostPrice;
    }

    public LocalDate getFirstBuyDate() {
        return firstBuyDate;
    }

    public void setFirstBuyDate(LocalDate firstBuyDate) {
        this.firstBuyDate = firstBuyDate;
    }

    public LocalDate getLastBuyDate() {
        return lastBuyDate;
    }

    public void setLastBuyDate(LocalDate lastBuyDate) {
        this.lastBuyDate = lastBuyDate;
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

    public boolean isProfit() {
        return profit;
    }

    public void setProfit(boolean profit) {
        this.profit = profit;
    }

    public BigDecimal getPredictedWinRate() {
        return predictedWinRate;
    }

    public void setPredictedWinRate(BigDecimal predictedWinRate) {
        this.predictedWinRate = predictedWinRate;
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

    public List<HoldingBuyRecordVO> getRecords() {
        return records;
    }

    public void setRecords(List<HoldingBuyRecordVO> records) {
        this.records = records;
    }
}
