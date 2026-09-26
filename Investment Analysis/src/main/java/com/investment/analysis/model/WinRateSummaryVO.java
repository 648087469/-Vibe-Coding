package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 三大指数胜率汇总（前端页面主数据）。
 */
public class WinRateSummaryVO {

    private LocalDateTime generatedAt;
    private int windowDays;
    private int changeWindowDays;
    private BigDecimal minWinRate;
    private BigDecimal maxWinRate;
    private BigDecimal changeMultiplier;
    private boolean clampWinRate;
    /** 计算说明，便于前端展示公式（含涨跌幅作用方向） */
    private String formula;

    private List<IndexWinRateVO> items;

    /** 三大指数平均胜率 */
    private BigDecimal averageWinRate;
    /** 胜率最高的指数名称 */
    private String strongestName;
    /** 胜率最低的指数名称 */
    private String weakestName;
    /** 胜率 >= 50% 的指数数量 */
    private int bullishCount;

    /** 本次数据来源说明，如「数据库」「数据库 + 东方财富接口」 */
    private String dataSourceSummary;
    /** 是否全部命中数据库 */
    private boolean allFromDatabase;

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
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

    public BigDecimal getMinWinRate() {
        return minWinRate;
    }

    public void setMinWinRate(BigDecimal minWinRate) {
        this.minWinRate = minWinRate;
    }

    public BigDecimal getMaxWinRate() {
        return maxWinRate;
    }

    public void setMaxWinRate(BigDecimal maxWinRate) {
        this.maxWinRate = maxWinRate;
    }

    public BigDecimal getChangeMultiplier() {
        return changeMultiplier;
    }

    public void setChangeMultiplier(BigDecimal changeMultiplier) {
        this.changeMultiplier = changeMultiplier;
    }

    public boolean isClampWinRate() {
        return clampWinRate;
    }

    public void setClampWinRate(boolean clampWinRate) {
        this.clampWinRate = clampWinRate;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public List<IndexWinRateVO> getItems() {
        return items;
    }

    public void setItems(List<IndexWinRateVO> items) {
        this.items = items;
    }

    public BigDecimal getAverageWinRate() {
        return averageWinRate;
    }

    public void setAverageWinRate(BigDecimal averageWinRate) {
        this.averageWinRate = averageWinRate;
    }

    public String getStrongestName() {
        return strongestName;
    }

    public void setStrongestName(String strongestName) {
        this.strongestName = strongestName;
    }

    public String getWeakestName() {
        return weakestName;
    }

    public void setWeakestName(String weakestName) {
        this.weakestName = weakestName;
    }

    public int getBullishCount() {
        return bullishCount;
    }

    public void setBullishCount(int bullishCount) {
        this.bullishCount = bullishCount;
    }

    public String getDataSourceSummary() {
        return dataSourceSummary;
    }

    public void setDataSourceSummary(String dataSourceSummary) {
        this.dataSourceSummary = dataSourceSummary;
    }

    public boolean isAllFromDatabase() {
        return allFromDatabase;
    }

    public void setAllFromDatabase(boolean allFromDatabase) {
        this.allFromDatabase = allFromDatabase;
    }
}
