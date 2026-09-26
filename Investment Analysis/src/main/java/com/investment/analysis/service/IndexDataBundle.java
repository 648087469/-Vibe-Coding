package com.investment.analysis.service;

import com.investment.analysis.entity.IndexDaily;

import java.time.LocalDate;
import java.util.List;

/**
 * 单个指数的数据装载结果：数据本身 + 来源信息。
 */
public class IndexDataBundle {

    private List<IndexDaily> series;
    /** 本次数据是否直接读自数据库 */
    private boolean fromDatabase;
    /** 数据来源标识：DATABASE 或具体数据源（EASTMONEY/TENCENT/SINA） */
    private String dataSource;
    /** 实际触发接口拉取时使用的数据源中文名，未拉取时为 null */
    private String fetchedFrom;
    /** 库中数据未及时更新而触发接口拉取的原因 */
    private String refreshReason;

    public List<IndexDaily> getSeries() {
        return series;
    }

    public void setSeries(List<IndexDaily> series) {
        this.series = series;
    }

    public boolean isFromDatabase() {
        return fromDatabase;
    }

    public void setFromDatabase(boolean fromDatabase) {
        this.fromDatabase = fromDatabase;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getFetchedFrom() {
        return fetchedFrom;
    }

    public void setFetchedFrom(String fetchedFrom) {
        this.fetchedFrom = fetchedFrom;
    }

    public String getRefreshReason() {
        return refreshReason;
    }

    public void setRefreshReason(String refreshReason) {
        this.refreshReason = refreshReason;
    }

    public LocalDate getFirstDate() {
        return series == null || series.isEmpty() ? null : series.get(0).getTradeDate();
    }

    public LocalDate getLastDate() {
        return series == null || series.isEmpty() ? null : series.get(series.size() - 1).getTradeDate();
    }
}
