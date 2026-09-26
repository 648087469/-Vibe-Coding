package com.investment.analysis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 持仓买入记录（表 holding_buy_record）。
 * <p>每笔买入保存：用户输入的买入日期与金额、买入成本（该交易日指数收盘点位）、
 * 以及买入时按历史数据算出的预测胜率。</p>
 */
@TableName("holding_buy_record")
public class HoldingBuyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long positionId;
    private LocalDate buyDate;
    private LocalDate costTradeDate;
    private BigDecimal costPrice;
    private BigDecimal buyAmount;
    private BigDecimal buyWinRate;
    private LocalDate winRatePeriodStart;
    private LocalDate winRatePeriodEnd;
    private Integer winRateSampleCount;
    private String winRateFormula;
    private String winRateNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public LocalDate getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDate buyDate) {
        this.buyDate = buyDate;
    }

    public LocalDate getCostTradeDate() {
        return costTradeDate;
    }

    public void setCostTradeDate(LocalDate costTradeDate) {
        this.costTradeDate = costTradeDate;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getBuyAmount() {
        return buyAmount;
    }

    public void setBuyAmount(BigDecimal buyAmount) {
        this.buyAmount = buyAmount;
    }

    public BigDecimal getBuyWinRate() {
        return buyWinRate;
    }

    public void setBuyWinRate(BigDecimal buyWinRate) {
        this.buyWinRate = buyWinRate;
    }

    public LocalDate getWinRatePeriodStart() {
        return winRatePeriodStart;
    }

    public void setWinRatePeriodStart(LocalDate winRatePeriodStart) {
        this.winRatePeriodStart = winRatePeriodStart;
    }

    public LocalDate getWinRatePeriodEnd() {
        return winRatePeriodEnd;
    }

    public void setWinRatePeriodEnd(LocalDate winRatePeriodEnd) {
        this.winRatePeriodEnd = winRatePeriodEnd;
    }

    public Integer getWinRateSampleCount() {
        return winRateSampleCount;
    }

    public void setWinRateSampleCount(Integer winRateSampleCount) {
        this.winRateSampleCount = winRateSampleCount;
    }

    public String getWinRateFormula() {
        return winRateFormula;
    }

    public void setWinRateFormula(String winRateFormula) {
        this.winRateFormula = winRateFormula;
    }

    public String getWinRateNote() {
        return winRateNote;
    }

    public void setWinRateNote(String winRateNote) {
        this.winRateNote = winRateNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
