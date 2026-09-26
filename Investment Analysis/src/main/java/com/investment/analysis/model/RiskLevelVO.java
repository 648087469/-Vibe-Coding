package com.investment.analysis.model;

import java.math.BigDecimal;

/**
 * 风险评估-单个档位的建议（一行 = 一个持仓的一个阶梯档位）。
 */
public class RiskLevelVO {

    /** 方向：PROFIT=止盈阶梯，LOSS=止损阶梯 */
    private String side;
    private String sideLabel;

    /** 触发点（%），止盈为正、止损为负 */
    private BigDecimal triggerPercent;
    /** 触发点文本，如 +3% / -5% */
    private String triggerLabel;

    /** 卖出比例：占触发时剩余仓位的比例（0.25 = 1/4） */
    private BigDecimal sellRatio;
    /** 卖出比例文本：1/4、1/2、全部 */
    private String sellRatioLabel;

    /** 该档触发前剩余仓位比例（1 = 100%） */
    private BigDecimal remainRatioBefore;
    /** 该档卖出量占初始仓位的比例 */
    private BigDecimal sellRatioOfInitial;
    /** 该档执行后累计卖出比例（占初始仓位） */
    private BigDecimal cumulativeSellRatio;
    /** 该档执行后剩余仓位比例 */
    private BigDecimal remainRatioAfter;

    /** 按当前市值折算的该档建议卖出金额（元） */
    private BigDecimal amountAtCurrentValue;

    /** 是否已触发 */
    private boolean triggered;
    /** 是否为当前所处档位（已触发档位中最深的一档） */
    private boolean current;
    /** 状态文本 */
    private String status;
    /** 建议说明 */
    private String remark;

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSideLabel() {
        return sideLabel;
    }

    public void setSideLabel(String sideLabel) {
        this.sideLabel = sideLabel;
    }

    public BigDecimal getTriggerPercent() {
        return triggerPercent;
    }

    public void setTriggerPercent(BigDecimal triggerPercent) {
        this.triggerPercent = triggerPercent;
    }

    public String getTriggerLabel() {
        return triggerLabel;
    }

    public void setTriggerLabel(String triggerLabel) {
        this.triggerLabel = triggerLabel;
    }

    public BigDecimal getSellRatio() {
        return sellRatio;
    }

    public void setSellRatio(BigDecimal sellRatio) {
        this.sellRatio = sellRatio;
    }

    public String getSellRatioLabel() {
        return sellRatioLabel;
    }

    public void setSellRatioLabel(String sellRatioLabel) {
        this.sellRatioLabel = sellRatioLabel;
    }

    public BigDecimal getRemainRatioBefore() {
        return remainRatioBefore;
    }

    public void setRemainRatioBefore(BigDecimal remainRatioBefore) {
        this.remainRatioBefore = remainRatioBefore;
    }

    public BigDecimal getSellRatioOfInitial() {
        return sellRatioOfInitial;
    }

    public void setSellRatioOfInitial(BigDecimal sellRatioOfInitial) {
        this.sellRatioOfInitial = sellRatioOfInitial;
    }

    public BigDecimal getCumulativeSellRatio() {
        return cumulativeSellRatio;
    }

    public void setCumulativeSellRatio(BigDecimal cumulativeSellRatio) {
        this.cumulativeSellRatio = cumulativeSellRatio;
    }

    public BigDecimal getRemainRatioAfter() {
        return remainRatioAfter;
    }

    public void setRemainRatioAfter(BigDecimal remainRatioAfter) {
        this.remainRatioAfter = remainRatioAfter;
    }

    public BigDecimal getAmountAtCurrentValue() {
        return amountAtCurrentValue;
    }

    public void setAmountAtCurrentValue(BigDecimal amountAtCurrentValue) {
        this.amountAtCurrentValue = amountAtCurrentValue;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
