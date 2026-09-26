package com.investment.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指数胜率分析结果（表 index_analysis_result）。
 */
@TableName("index_analysis_result")
public class IndexAnalysisResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String indexCode;
    private String indexName;
    private LocalDate latestTradeDate;
    private BigDecimal latestClose;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer sampleCount;
    private BigDecimal minClose;
    private BigDecimal maxClose;
    private BigDecimal baseWinRate;
    private LocalDate compareTradeDate;
    private BigDecimal compareClose;
    private BigDecimal changePercent;
    private BigDecimal changeAdjust;
    private BigDecimal rawWinRate;
    private BigDecimal finalWinRate;
    private String dataSource;
    private LocalDateTime analyzedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public BigDecimal getMinClose() {
        return minClose;
    }

    public void setMinClose(BigDecimal minClose) {
        this.minClose = minClose;
    }

    public BigDecimal getMaxClose() {
        return maxClose;
    }

    public void setMaxClose(BigDecimal maxClose) {
        this.maxClose = maxClose;
    }

    public BigDecimal getBaseWinRate() {
        return baseWinRate;
    }

    public void setBaseWinRate(BigDecimal baseWinRate) {
        this.baseWinRate = baseWinRate;
    }

    public LocalDate getCompareTradeDate() {
        return compareTradeDate;
    }

    public void setCompareTradeDate(LocalDate compareTradeDate) {
        this.compareTradeDate = compareTradeDate;
    }

    public BigDecimal getCompareClose() {
        return compareClose;
    }

    public void setCompareClose(BigDecimal compareClose) {
        this.compareClose = compareClose;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(BigDecimal changePercent) {
        this.changePercent = changePercent;
    }

    public BigDecimal getChangeAdjust() {
        return changeAdjust;
    }

    public void setChangeAdjust(BigDecimal changeAdjust) {
        this.changeAdjust = changeAdjust;
    }

    public BigDecimal getRawWinRate() {
        return rawWinRate;
    }

    public void setRawWinRate(BigDecimal rawWinRate) {
        this.rawWinRate = rawWinRate;
    }

    public BigDecimal getFinalWinRate() {
        return finalWinRate;
    }

    public void setFinalWinRate(BigDecimal finalWinRate) {
        this.finalWinRate = finalWinRate;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
