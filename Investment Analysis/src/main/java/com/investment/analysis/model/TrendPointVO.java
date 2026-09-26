package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指数走势曲线上的一个点。
 */
public class TrendPointVO {

    private LocalDate tradeDate;
    private BigDecimal close;
    private BigDecimal volume;

    public TrendPointVO() {
    }

    public TrendPointVO(LocalDate tradeDate, BigDecimal close, BigDecimal volume) {
        this.tradeDate = tradeDate;
        this.close = close;
        this.volume = volume;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }
}
