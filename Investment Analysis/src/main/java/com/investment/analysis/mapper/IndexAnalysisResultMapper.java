package com.investment.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investment.analysis.entity.IndexAnalysisResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 指数胜率分析结果 Mapper。
 */
@Mapper
public interface IndexAnalysisResultMapper extends BaseMapper<IndexAnalysisResult> {

    /**
     * 按 (index_code, latest_trade_date) 唯一键写入或更新分析结果。
     */
    @Insert("INSERT INTO index_analysis_result (index_code, index_name, latest_trade_date, latest_close,"
            + " period_start, period_end, sample_count, min_close, max_close, base_win_rate,"
            + " compare_trade_date, compare_close, change_percent, change_adjust, raw_win_rate,"
            + " final_win_rate, data_source) VALUES (#{indexCode}, #{indexName}, #{latestTradeDate},"
            + " #{latestClose}, #{periodStart}, #{periodEnd}, #{sampleCount}, #{minClose}, #{maxClose},"
            + " #{baseWinRate}, #{compareTradeDate}, #{compareClose}, #{changePercent}, #{changeAdjust},"
            + " #{rawWinRate}, #{finalWinRate}, #{dataSource})"
            + " ON DUPLICATE KEY UPDATE index_name = VALUES(index_name), latest_close = VALUES(latest_close),"
            + " period_start = VALUES(period_start), period_end = VALUES(period_end),"
            + " sample_count = VALUES(sample_count), min_close = VALUES(min_close),"
            + " max_close = VALUES(max_close), base_win_rate = VALUES(base_win_rate),"
            + " compare_trade_date = VALUES(compare_trade_date), compare_close = VALUES(compare_close),"
            + " change_percent = VALUES(change_percent), change_adjust = VALUES(change_adjust),"
            + " raw_win_rate = VALUES(raw_win_rate), final_win_rate = VALUES(final_win_rate),"
            + " data_source = VALUES(data_source), analyzed_at = CURRENT_TIMESTAMP")
    int upsertResult(IndexAnalysisResult result);

    /**
     * 查询最新的分析批次快照（同一最新交易日下的全部指数）。
     */
    @Select("SELECT * FROM index_analysis_result WHERE latest_trade_date = "
            + "(SELECT MAX(latest_trade_date) FROM index_analysis_result) ORDER BY index_code ASC")
    List<IndexAnalysisResult> selectLatestSnapshot();

    /**
     * 按分析时间倒序查询历史结果。
     */
    @Select("SELECT * FROM index_analysis_result ORDER BY analyzed_at DESC, id DESC LIMIT #{limit}")
    List<IndexAnalysisResult> selectHistory(@Param("limit") int limit);
}
