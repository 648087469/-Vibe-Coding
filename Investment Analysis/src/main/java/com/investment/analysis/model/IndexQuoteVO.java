package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指数最新行情快照：用于判断持仓当前是盈还是亏。
 */
public class IndexQuoteVO {

    private String code;
    private String name;
    /** 最新交易日 */
    private LocalDate latestTradeDate;
    /** 最新点位（收盘） */
    private BigDecimal latestClose;
    /** 数据来源标签，如「本地数据库」「东方财富」 */
    private String dataSourceLabel;
    /** 该指数是否被持仓引用 */
    private boolean used;
    /** 行情是否可用（不可用时前端提示） */
    private boolean available;
    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDataSourceLabel() {
        return dataSourceLabel;
    }

    public void setDataSourceLabel(String dataSourceLabel) {
        this.dataSourceLabel = dataSourceLabel;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
