package com.investment.analysis.provider;

import com.investment.analysis.model.IndexDailyData;
import com.investment.analysis.model.IndexDefinition;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数行情数据源统一接口。数据库无数据时按优先级依次尝试各实现。
 */
public interface IndexDataProvider {

    /** 数据源标识，如 EASTMONEY */
    String sourceName();

    /** 数据源中文名，用于日志与前端展示 */
    String displayName();

    /** 优先级，数字越小越优先 */
    int order();

    /**
     * 拉取指定指数在 [start, end] 区间内的日线数据。
     */
    List<IndexDailyData> fetch(IndexDefinition definition, LocalDate start, LocalDate end);
}
