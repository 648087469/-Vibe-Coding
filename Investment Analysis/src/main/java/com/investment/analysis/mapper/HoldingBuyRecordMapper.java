package com.investment.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investment.analysis.entity.HoldingBuyRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 持仓买入记录 Mapper。
 */
@Mapper
public interface HoldingBuyRecordMapper extends BaseMapper<HoldingBuyRecord> {

    /**
     * 查询全部买入记录，按持仓、买入日期升序。
     */
    @Select("SELECT * FROM holding_buy_record ORDER BY position_id ASC, buy_date ASC, id ASC")
    List<HoldingBuyRecord> selectAllOrdered();

    /**
     * 查询某个持仓下的买入记录，按买入日期升序。
     */
    @Select("SELECT * FROM holding_buy_record WHERE position_id = #{positionId} "
            + "ORDER BY buy_date ASC, id ASC")
    List<HoldingBuyRecord> selectByPositionId(@Param("positionId") Long positionId);

    /**
     * 删除某个持仓下的全部买入记录。
     */
    @Delete("DELETE FROM holding_buy_record WHERE position_id = #{positionId}")
    int deleteByPositionId(@Param("positionId") Long positionId);

    /**
     * 统计某个持仓下的买入笔数。
     */
    @Select("SELECT COUNT(1) FROM holding_buy_record WHERE position_id = #{positionId}")
    int countByPositionId(@Param("positionId") Long positionId);
}
