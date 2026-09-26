package com.investment.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 投资方案模块参数，对应 application.yml 中的 plan.* 配置。
 */
@ConfigurationProperties(prefix = "plan")
public class PlanProperties {

    /** 投资次数默认值 */
    private int defaultTimes = 4;

    /** 投资次数下限 */
    private int minTimes = 1;

    /** 投资次数上限 */
    private int maxTimes = 20;

    /** 金额保留小数位 */
    private int amountScale = 2;

    /** 页面默认的可用投资总金额 */
    private String defaultTotalAmount = "100000";

    public int getDefaultTimes() {
        return defaultTimes;
    }

    public void setDefaultTimes(int defaultTimes) {
        this.defaultTimes = defaultTimes;
    }

    public int getMinTimes() {
        return minTimes;
    }

    public void setMinTimes(int minTimes) {
        this.minTimes = minTimes;
    }

    public int getMaxTimes() {
        return maxTimes;
    }

    public void setMaxTimes(int maxTimes) {
        this.maxTimes = maxTimes;
    }

    public int getAmountScale() {
        return amountScale;
    }

    public void setAmountScale(int amountScale) {
        this.amountScale = amountScale;
    }

    public String getDefaultTotalAmount() {
        return defaultTotalAmount;
    }

    public void setDefaultTotalAmount(String defaultTotalAmount) {
        this.defaultTotalAmount = defaultTotalAmount;
    }
}
