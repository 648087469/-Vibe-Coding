package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 风险评估-单个持仓的方案建议（一个持仓 = 一个方案）。
 */
public class RiskPlanVO {

    private Long positionId;
    private String positionName;
    private String indexCode;
    private String indexName;

    /** 累计买入金额 / 当前市值 / 当前盈亏 */
    private BigDecimal totalAmount;
    private BigDecimal marketValue;
    private BigDecimal avgCostPrice;
    private BigDecimal latestClose;
    private LocalDate latestTradeDate;
    private BigDecimal profitAmount;
    private BigDecimal profitPercent;
    /** 行情是否可用 */
    private boolean quoteAvailable;

    /** 风险等级标签与样式 */
    private String riskLevel;
    private String riskLevelTag;
    /** 当前建议（主文案） */
    private String suggestion;
    /** 当前建议动作：卖出 1/4 / 卖出 1/2 / 全部卖出 / 持有观察 / 无法评估 */
    private String actionLabel;
    /** 当前建议卖出金额（元），无操作时为 null */
    private BigDecimal actionAmount;
    /** 当前建议卖出占初始仓位的比例（%） */
    private BigDecimal actionRatioOfInitial;
    /** 若执行当前建议，剩余仓位比例（%） */
    private BigDecimal remainRatioAfterAction;
    /** 已触发档位说明，如「+3% 卖出 1/4 → +5% 卖出 1/2」 */
    private String triggeredSummary;
    /** 已触发档位数量 */
    private int triggeredCount;
    /** 下一档触发点（%）与说明 */
    private BigDecimal nextTriggerPercent;
    private String nextTriggerLabel;
    /** 距离下一档还差多少个百分点 */
    private BigDecimal gapToNextPercent;
    /** 下一档建议动作与金额 */
    private String nextActionLabel;
    private BigDecimal nextActionAmount;
    /** 反向提示：转入亏损/盈利方向时适用的阶梯 */
    private String oppositeHint;
    /** 当前方向的完整阶梯 */
    private List<RiskLevelVO> levels = new ArrayList<RiskLevelVO>();

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

    public BigDecimal getAvgCostPrice() {
        return avgCostPrice;
    }

    public void setAvgCostPrice(BigDecimal avgCostPrice) {
        this.avgCostPrice = avgCostPrice;
    }

    public BigDecimal getLatestClose() {
        return latestClose;
    }

    public void setLatestClose(BigDecimal latestClose) {
        this.latestClose = latestClose;
    }

    public LocalDate getLatestTradeDate() {
        return latestTradeDate;
    }

    public void setLatestTradeDate(LocalDate latestTradeDate) {
        this.latestTradeDate = latestTradeDate;
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

    public boolean isQuoteAvailable() {
        return quoteAvailable;
    }

    public void setQuoteAvailable(boolean quoteAvailable) {
        this.quoteAvailable = quoteAvailable;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRiskLevelTag() {
        return riskLevelTag;
    }

    public void setRiskLevelTag(String riskLevelTag) {
        this.riskLevelTag = riskLevelTag;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public BigDecimal getActionAmount() {
        return actionAmount;
    }

    public void setActionAmount(BigDecimal actionAmount) {
        this.actionAmount = actionAmount;
    }

    public BigDecimal getActionRatioOfInitial() {
        return actionRatioOfInitial;
    }

    public void setActionRatioOfInitial(BigDecimal actionRatioOfInitial) {
        this.actionRatioOfInitial = actionRatioOfInitial;
    }

    public BigDecimal getRemainRatioAfterAction() {
        return remainRatioAfterAction;
    }

    public void setRemainRatioAfterAction(BigDecimal remainRatioAfterAction) {
        this.remainRatioAfterAction = remainRatioAfterAction;
    }

    public String getTriggeredSummary() {
        return triggeredSummary;
    }

    public void setTriggeredSummary(String triggeredSummary) {
        this.triggeredSummary = triggeredSummary;
    }

    public int getTriggeredCount() {
        return triggeredCount;
    }

    public void setTriggeredCount(int triggeredCount) {
        this.triggeredCount = triggeredCount;
    }

    public BigDecimal getNextTriggerPercent() {
        return nextTriggerPercent;
    }

    public void setNextTriggerPercent(BigDecimal nextTriggerPercent) {
        this.nextTriggerPercent = nextTriggerPercent;
    }

    public String getNextTriggerLabel() {
        return nextTriggerLabel;
    }

    public void setNextTriggerLabel(String nextTriggerLabel) {
        this.nextTriggerLabel = nextTriggerLabel;
    }

    public BigDecimal getGapToNextPercent() {
        return gapToNextPercent;
    }

    public void setGapToNextPercent(BigDecimal gapToNextPercent) {
        this.gapToNextPercent = gapToNextPercent;
    }

    public String getNextActionLabel() {
        return nextActionLabel;
    }

    public void setNextActionLabel(String nextActionLabel) {
        this.nextActionLabel = nextActionLabel;
    }

    public BigDecimal getNextActionAmount() {
        return nextActionAmount;
    }

    public void setNextActionAmount(BigDecimal nextActionAmount) {
        this.nextActionAmount = nextActionAmount;
    }

    public String getOppositeHint() {
        return oppositeHint;
    }

    public void setOppositeHint(String oppositeHint) {
        this.oppositeHint = oppositeHint;
    }

    public List<RiskLevelVO> getLevels() {
        return levels;
    }

    public void setLevels(List<RiskLevelVO> levels) {
        this.levels = levels;
    }
}
