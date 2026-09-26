package com.investment.analysis.model;

import java.math.BigDecimal;

/**
 * 投资方案生成请求。
 */
public class InvestmentPlanRequest {

    /** 可用投资总金额 */
    private BigDecimal totalAmount;

    /** 投资次数 n，为空时取配置的默认值 */
    private Integer times;

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }
}
