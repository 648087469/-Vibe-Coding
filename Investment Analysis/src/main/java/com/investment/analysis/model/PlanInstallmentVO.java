package com.investment.analysis.model;

import java.math.BigDecimal;

/**
 * 投资方案中的一期投资。
 */
public class PlanInstallmentVO {

    /** 第几期，从 1 开始 */
    private int periodNo;
    /** 本期投资金额 */
    private BigDecimal amount;
    /** 本期投入后的累计投入金额 */
    private BigDecimal cumulativeAmount;
    /** 本期金额占可用总金额的百分比 */
    private BigDecimal ratioOfTotal;
    /** 本期累计投入占可用总金额的百分比 */
    private BigDecimal ratioCumulative;
    /** 计算说明 */
    private String remark;

    public int getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(int periodNo) {
        this.periodNo = periodNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getCumulativeAmount() {
        return cumulativeAmount;
    }

    public void setCumulativeAmount(BigDecimal cumulativeAmount) {
        this.cumulativeAmount = cumulativeAmount;
    }

    public BigDecimal getRatioOfTotal() {
        return ratioOfTotal;
    }

    public void setRatioOfTotal(BigDecimal ratioOfTotal) {
        this.ratioOfTotal = ratioOfTotal;
    }

    public BigDecimal getRatioCumulative() {
        return ratioCumulative;
    }

    public void setRatioCumulative(BigDecimal ratioCumulative) {
        this.ratioCumulative = ratioCumulative;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
