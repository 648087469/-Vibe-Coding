package com.investment.analysis.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 风险评估页面主数据：每个持仓一个方案建议 + 汇总统计 + 规则说明。
 */
public class RiskOverviewVO {

    private LocalDateTime generatedAt;
    /** 方案建议列表（一个持仓 = 一个方案，含完整阶梯） */
    private List<RiskPlanVO> plans = new ArrayList<RiskPlanVO>();
    private RiskStatsVO stats;
    /** 止盈阶梯规则文本 */
    private String takeProfitRule;
    /** 止损阶梯规则文本 */
    private String stopLossRule;
    /** 计算口径说明 */
    private String calcRule;
    /** 行情说明（来自持仓管理模块） */
    private String quoteSummary;

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<RiskPlanVO> getPlans() {
        return plans;
    }

    public void setPlans(List<RiskPlanVO> plans) {
        this.plans = plans;
    }

    public RiskStatsVO getStats() {
        return stats;
    }

    public void setStats(RiskStatsVO stats) {
        this.stats = stats;
    }

    public String getTakeProfitRule() {
        return takeProfitRule;
    }

    public void setTakeProfitRule(String takeProfitRule) {
        this.takeProfitRule = takeProfitRule;
    }

    public String getStopLossRule() {
        return stopLossRule;
    }

    public void setStopLossRule(String stopLossRule) {
        this.stopLossRule = stopLossRule;
    }

    public String getCalcRule() {
        return calcRule;
    }

    public void setCalcRule(String calcRule) {
        this.calcRule = calcRule;
    }

    public String getQuoteSummary() {
        return quoteSummary;
    }

    public void setQuoteSummary(String quoteSummary) {
        this.quoteSummary = quoteSummary;
    }
}
