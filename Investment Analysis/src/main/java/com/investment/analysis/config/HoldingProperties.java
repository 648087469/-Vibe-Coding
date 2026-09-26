package com.investment.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 持仓管理模块参数，对应 application.yml 中的 holding.* 配置。
 */
@ConfigurationProperties(prefix = "holding")
public class HoldingProperties {

    /** 高预测胜率阈值：预测胜率 >= 该值的买入单独统计真实胜率，默认 70% */
    private double highWinRateThreshold = 70D;

    /** 持仓名称最大长度 */
    private int maxPositionNameLength = 32;

    /** 单笔买入金额上限（元），防止误输入 */
    private long maxBuyAmount = 100000000L;

    /** 计算买入时预测胜率所需的最少交易日数量，样本不足时该笔不记录预测胜率 */
    private int minWinRateSamples = 20;

    public double getHighWinRateThreshold() {
        return highWinRateThreshold;
    }

    public void setHighWinRateThreshold(double highWinRateThreshold) {
        this.highWinRateThreshold = highWinRateThreshold;
    }

    public int getMaxPositionNameLength() {
        return maxPositionNameLength;
    }

    public void setMaxPositionNameLength(int maxPositionNameLength) {
        this.maxPositionNameLength = maxPositionNameLength;
    }

    public long getMaxBuyAmount() {
        return maxBuyAmount;
    }

    public void setMaxBuyAmount(long maxBuyAmount) {
        this.maxBuyAmount = maxBuyAmount;
    }

    public int getMinWinRateSamples() {
        return minWinRateSamples;
    }

    public void setMinWinRateSamples(int minWinRateSamples) {
        this.minWinRateSamples = minWinRateSamples;
    }
}
