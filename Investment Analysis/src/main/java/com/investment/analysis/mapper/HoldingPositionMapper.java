package com.investment.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.investment.analysis.entity.HoldingPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 持仓主表 Mapper。
 */
@Mapper
public interface HoldingPositionMapper extends BaseMapper<HoldingPosition> {

    /**
     * 按「持仓名称 + 关联指数」查找已有持仓，不存在返回 null。
     */
    @Select("SELECT * FROM holding_position WHERE position_name = #{positionName} "
            + "AND index_code = #{indexCode} LIMIT 1")
    HoldingPosition selectByNameAndIndex(@Param("positionName") String positionName,
                                         @Param("indexCode") String indexCode);
}
