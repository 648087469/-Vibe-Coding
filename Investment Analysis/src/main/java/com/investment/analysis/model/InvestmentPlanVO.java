package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投资方案计算结果。
 */
public class InvestmentPlanVO {

    /** 用户输入的可用投资总金额 */
    private BigDecimal totalAmount;
    /** 投资次数 n */
    private int times;
    /** 首次（基础）投入金额 */
    private BigDecimal baseAmount;
    /** 方案实际使用的总体金额 */
    private BigDecimal usedAmount;
    /** 末期相对首期的资金放大倍数 2^(n-1) */
    private BigDecimal growthMultiple;
    /** 末期尾差调整金额（四舍五入到分产生的差额） */
    private BigDecimal lastAdjustment;
    /** 规则说明 */
    private String rule;
    private LocalDateTime generatedAt;
    private List<PlanInstallmentVO> installments;

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getUsedAmount() {
        return usedAmount;
    }

    public void setUsedAmount(BigDecimal usedAmount) {
        this.usedAmount = usedAmount;
    }

    public BigDecimal getGrowthMultiple() {
        return growthMultiple;
    }

    public void setGrowthMultiple(BigDecimal growthMultiple) {
        this.growthMultiple = growthMultiple;
    }

    public BigDecimal getLastAdjustment() {
        return lastAdjustment;
    }

    public void setLastAdjustment(BigDecimal lastAdjustment) {
        this.lastAdjustment = lastAdjustment;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<PlanInstallmentVO> getInstallments() {
        return installments;
    }

    public void setInstallments(List<PlanInstallmentVO> installments) {
        this.installments = installments;
    }
}
