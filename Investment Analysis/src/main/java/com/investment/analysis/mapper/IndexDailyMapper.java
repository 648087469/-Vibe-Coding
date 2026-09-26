package com.investment.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.model.IndexDailyData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 指数日线行情 Mapper。
 */
@Mapper
public interface IndexDailyMapper extends BaseMapper<IndexDaily> {

    /**
     * 按指数代码查询指定日期区间内的日线数据，按交易日升序。
     */
    @Select("SELECT * FROM index_daily WHERE index_code = #{indexCode} "
            + "AND trade_date BETWEEN #{start} AND #{end} ORDER BY trade_date ASC")
    List<IndexDaily> selectRange(@Param("indexCode") String indexCode,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);

    /**
     * 查询某指数在数据库中的最新交易日，无数据返回 null。
     */
    @Select("SELECT MAX(trade_date) FROM index_daily WHERE index_code = #{indexCode}")
    LocalDate selectLatestTradeDate(@Param("indexCode") String indexCode);

    /**
     * 查询某指数在数据库中的最早交易日，无数据返回 null。
     */
    @Select("SELECT MIN(trade_date) FROM index_daily WHERE index_code = #{indexCode}")
    LocalDate selectEarliestTradeDate(@Param("indexCode") String indexCode);

    /**
     * 统计某指数在数据库中的记录条数。
     */
    @Select("SELECT COUNT(1) FROM index_daily WHERE index_code = #{indexCode}")
    int countByCode(@Param("indexCode") String indexCode);

    /**
     * 批量 UPSERT：按 (index_code, trade_date) 唯一键写入或更新，天然幂等。
     */
    @org.apache.ibatis.annotations.Insert("<script>"
            + "INSERT INTO index_daily (index_code, index_name, trade_date, open_price, close_price,"
            + " high_price, low_price, volume, amount, data_source) VALUES "
            + "<foreach collection='list' item='it' separator=','>"
            + "(#{it.indexCode}, #{it.indexName}, #{it.tradeDate}, #{it.openPrice}, #{it.closePrice},"
            + " #{it.highPrice}, #{it.lowPrice}, #{it.volume}, #{it.amount}, #{it.dataSource})"
            + "</foreach> "
            + "ON DUPLICATE KEY UPDATE index_name = VALUES(index_name), open_price = VALUES(open_price),"
            + " close_price = VALUES(close_price), high_price = VALUES(high_price),"
            + " low_price = VALUES(low_price), volume = VALUES(volume), amount = VALUES(amount),"
            + " data_source = VALUES(data_source)"
            + "</script>")
    int upsertBatch(@Param("list") List<IndexDailyData> list);
}
