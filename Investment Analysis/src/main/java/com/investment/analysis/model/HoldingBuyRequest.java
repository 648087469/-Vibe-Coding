package com.investment.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增买入的入参：持仓名称 + 关联指数 + 具体买入日期 + 买入金额。
 * <p>同名同指数的持仓会自动合并为一笔新的买入记录。</p>
 */
public class HoldingBuyRequest {

    /** 持仓名称（用户输入） */
    private String positionName;

    /** 关联指数代码：000001 上证指数 / 399001 深证成指 / 399006 创业板指 */
    private String indexCode;

    /** 具体买入日期 */
    private LocalDate buyDate;

    /** 买入金额（元） */
    private BigDecimal buyAmount;

    /** 备注（可选） */
    private String remark;

    /** 直接追加到指定持仓（可选，填写后忽略持仓名称匹配） */
    private Long positionId;

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getIndexCode() {
        return indexCode;
    }

    public void setIndexCode(String indexCode) {
        this.indexCode = indexCode;
    }

    public LocalDate getBuyDate() {
        return buyDate;
    }

    public void setBuyDate(LocalDate buyDate) {
        this.buyDate = buyDate;
    }

    public BigDecimal getBuyAmount() {
        return buyAmount;
    }

    public void setBuyAmount(BigDecimal buyAmount) {
        this.buyAmount = buyAmount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }
}
