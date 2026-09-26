package com.investment.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 投资分析模块业务参数，对应 application.yml 中的 analysis.* 配置。
 */
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {

    /** 取样窗口天数，默认 365 天 */
    private int windowDays = 365;

    /** 涨跌幅对比窗口天数，默认对比 7 天前 */
    private int changeWindowDays = 7;

    /** 对比基准取法：DATE=按自然日回溯后的最近交易日；BAR=倒数第 N 根 K 线 */
    private String changeWindowMode = "DATE";

    /** 窗口最低点对应的胜率 */
    private double minWinRate = 90D;

    /** 窗口最高点对应的胜率 */
    private double maxWinRate = 10D;

    /** 涨跌幅换算系数，胜率 = 基础胜率 + 涨跌幅(%) * 系数 */
    private double changeMultiplier = 10D;

    /**
     * 涨跌幅对胜率的作用方向：
     * INVERSE = 反向（指数上涨则下调胜率、下跌则上调胜率，与「点位越低胜率越高」的模型一致，默认）；
     * DIRECT  = 同向（指数上涨则上调胜率、下跌则下调胜率）。
     */
    private String changeDirection = "INVERSE";

    /** 最终胜率是否裁剪到 [0, 100] */
    private boolean clampWinRate = true;

    /** 库中数据超过多少自然日未更新视为过期 */
    private int maxDataStalenessDays = 3;

    /** 库中至少需要多少条记录才视为数据完整 */
    private int minRequiredRecords = 200;

    private Fetch fetch = new Fetch();

    private Providers providers = new Providers();

    public static class Fetch {
        private int timeoutMillis = 10000;
        private int retryTimes = 2;
        private long retryIntervalMillis = 800L;
        private int failureThreshold = 2;
        private int circuitOpenMinutes = 10;
        private boolean autoRefreshOnStartup = true;
        private boolean scheduledEnabled = true;
        private String scheduleCron = "0 30 18 * * MON-FRI";

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public void setRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
        }

        public long getRetryIntervalMillis() {
            return retryIntervalMillis;
        }

        public void setRetryIntervalMillis(long retryIntervalMillis) {
            this.retryIntervalMillis = retryIntervalMillis;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public int getCircuitOpenMinutes() {
            return circuitOpenMinutes;
        }

        public void setCircuitOpenMinutes(int circuitOpenMinutes) {
            this.circuitOpenMinutes = circuitOpenMinutes;
        }

        public boolean isAutoRefreshOnStartup() {
            return autoRefreshOnStartup;
        }

        public void setAutoRefreshOnStartup(boolean autoRefreshOnStartup) {
            this.autoRefreshOnStartup = autoRefreshOnStartup;
        }

        public boolean isScheduledEnabled() {
            return scheduledEnabled;
        }

        public void setScheduledEnabled(boolean scheduledEnabled) {
            this.scheduledEnabled = scheduledEnabled;
        }

        public String getScheduleCron() {
            return scheduleCron;
        }

        public void setScheduleCron(String scheduleCron) {
            this.scheduleCron = scheduleCron;
        }
    }

    public static class Providers {
        private ProviderToggle eastmoney = new ProviderToggle();
        private ProviderToggle tencent = new ProviderToggle();
        private ProviderToggle sina = new ProviderToggle();

        public ProviderToggle getEastmoney() {
            return eastmoney;
        }

        public void setEastmoney(ProviderToggle eastmoney) {
            this.eastmoney = eastmoney;
        }

        public ProviderToggle getTencent() {
            return tencent;
        }

        public void setTencent(ProviderToggle tencent) {
            this.tencent = tencent;
        }

        public ProviderToggle getSina() {
            return sina;
        }

        public void setSina(ProviderToggle sina) {
            this.sina = sina;
        }
    }

    public static class ProviderToggle {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getChangeWindowDays() {
        return changeWindowDays;
    }

    public void setChangeWindowDays(int changeWindowDays) {
        this.changeWindowDays = changeWindowDays;
    }

    public String getChangeWindowMode() {
        return changeWindowMode;
    }

    public void setChangeWindowMode(String changeWindowMode) {
        this.changeWindowMode = changeWindowMode;
    }

    public double getMinWinRate() {
        return minWinRate;
    }

    public void setMinWinRate(double minWinRate) {
        this.minWinRate = minWinRate;
    }

    public double getMaxWinRate() {
        return maxWinRate;
    }

    public void setMaxWinRate(double maxWinRate) {
        this.maxWinRate = maxWinRate;
    }

    public double getChangeMultiplier() {
        return changeMultiplier;
    }

    public void setChangeMultiplier(double changeMultiplier) {
        this.changeMultiplier = changeMultiplier;
    }

    public String getChangeDirection() {
        return changeDirection;
    }

    public void setChangeDirection(String changeDirection) {
        this.changeDirection = changeDirection;
    }

    /** 是否为反向调整（涨减跌加） */
    public boolean isInverseChange() {
        return !"DIRECT".equalsIgnoreCase(changeDirection);
    }

    public boolean isClampWinRate() {
        return clampWinRate;
    }

    public void setClampWinRate(boolean clampWinRate) {
        this.clampWinRate = clampWinRate;
    }

    public int getMaxDataStalenessDays() {
        return maxDataStalenessDays;
    }

    public void setMaxDataStalenessDays(int maxDataStalenessDays) {
        this.maxDataStalenessDays = maxDataStalenessDays;
    }

    public int getMinRequiredRecords() {
        return minRequiredRecords;
    }

    public void setMinRequiredRecords(int minRequiredRecords) {
        this.minRequiredRecords = minRequiredRecords;
    }

    public Fetch getFetch() {
        return fetch;
    }

    public void setFetch(Fetch fetch) {
        this.fetch = fetch;
    }

    public Providers getProviders() {
        return providers;
    }

    public void setProviders(Providers providers) {
        this.providers = providers;
    }
}
