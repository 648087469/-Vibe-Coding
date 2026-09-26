package com.investment.analysis.service;

import com.investment.analysis.config.RiskProperties;
import com.investment.analysis.model.HoldingPositionVO;
import com.investment.analysis.model.RiskLevelVO;
import com.investment.analysis.model.RiskOverviewVO;
import com.investment.analysis.model.RiskPlanVO;
import com.investment.analysis.model.RiskStatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 风险评估（分档止盈/止损建议）单元测试。
 * <p>规则：盈利超过 3% 卖出剩余仓位的 1/4；超过 5% 再卖出剩余的 1/2；
 * 此后每上涨 1 个点卖出剩余的 1/4；到 10% 全部卖出；亏损方向对称。</p>
 */
class RiskCalculatorTest {

    private final RiskProperties properties = new RiskProperties();
    private final RiskCalculator calculator = new RiskCalculator(properties);

    private HoldingPositionVO position(String name, String profitPercent, String marketValue, String totalAmount) {
        HoldingPositionVO vo = new HoldingPositionVO();
        vo.setId(1L);
        vo.setPositionName(name);
        vo.setIndexCode("000001");
        vo.setIndexName("上证指数");
        vo.setLatestTradeDate(LocalDate.parse("2026-09-24"));
        vo.setLatestClose(new BigDecimal("3888.370"));
        vo.setAvgCostPrice(new BigDecimal("3700.000"));
        if (profitPercent != null) {
            vo.setProfitPercent(new BigDecimal(profitPercent));
        }
        if (marketValue != null) {
            vo.setMarketValue(new BigDecimal(marketValue));
        }
        vo.setTotalAmount(new BigDecimal(totalAmount));
        return vo;
    }

    private RiskLevelVO level(RiskPlanVO plan, String triggerLabel) {
        for (RiskLevelVO level : plan.getLevels()) {
            if (level.getTriggerLabel().equals(triggerLabel)) {
                return level;
            }
        }
        throw new IllegalArgumentException("阶梯中不存在档位 " + triggerLabel);
    }

    @Test
    @DisplayName("阶梯结构：3% 卖 1/4、5% 卖 1/2、6%~9% 每档卖剩余 1/4、10% 全部卖出")
    void ladderShouldMatchRule() {
        List<RiskLevelVO> ladder = calculator.previewLadder(true);

        assertEquals(7, ladder.size());
        assertLevel(ladder.get(0), "+3%", "1/4", "0.25", "25", "75");
        assertLevel(ladder.get(1), "+5%", "1/2", "0.625", "62.5", "37.5");
        assertLevel(ladder.get(2), "+6%", "1/4", "0.71875", "71.875", "28.125");
        assertLevel(ladder.get(3), "+7%", "1/4", "0.7890625", "78.90625", "21.09375");
        assertLevel(ladder.get(4), "+8%", "1/4", "0.841796875", "84.1796875", "15.8203125");
        assertLevel(ladder.get(5), "+9%", "1/4", "0.88134765625", "88.134765625", "11.865234375");
        assertLevel(ladder.get(6), "+10%", "全部", "1", "100", "0");
    }

    @Test
    @DisplayName("盈利 4.2%：已触发 +3% 档，建议卖出 1/4（按当前市值折算金额）")
    void shouldSuggestQuarterSellWhenProfitOverThreePercent() {
        RiskPlanVO plan = calculator.buildPlan(position("上证指数定投", "4.2000", "10000.00", "9600.00"));

        assertEquals("止盈区", plan.getRiskLevel());
        assertEquals("卖出 1/4", plan.getActionLabel());
        assertEquals(0, new BigDecimal("2500.00").compareTo(plan.getActionAmount()));
        assertEquals(0, new BigDecimal("25.0000").compareTo(plan.getActionRatioOfInitial()));
        assertEquals(0, new BigDecimal("75.0000").compareTo(plan.getRemainRatioAfterAction()));
        assertEquals(1, plan.getTriggeredCount());
        assertEquals("+3% 卖出 1/4", plan.getTriggeredSummary());
        // 下一档：+5%，还差 0.8 个百分点
        assertEquals("+5%", plan.getNextTriggerLabel());
        assertEquals(0, new BigDecimal("0.8000").compareTo(plan.getGapToNextPercent()));
        assertEquals(0, new BigDecimal("3750.00").compareTo(plan.getNextActionAmount()));
        assertTrue(plan.getSuggestion().contains("卖出当时剩余仓位的 1/4"));
    }

    @Test
    @DisplayName("盈利 7.5%：已触发 +3%/+5%/+6%/+7% 四档，当前建议按 +7% 档执行")
    void shouldTrackTriggeredLevelsForHigherProfit() {
        RiskPlanVO plan = calculator.buildPlan(position("持仓A", "7.5000", "10000.00", "9300.00"));

        assertEquals(4, plan.getTriggeredCount());
        assertEquals("+7%", level(plan, "+7%").getTriggerLabel());
        assertEquals("1/4", level(plan, "+7%").getSellRatioLabel());
        assertEquals(0, new BigDecimal("703.13").compareTo(plan.getActionAmount()));
        // 当前档（+7%）卖出量占初始仓位 = 0.25 × 0.75 × 0.25 × 0.25 = 7.03125%
        assertEquals(0, new BigDecimal("7.03125").compareTo(plan.getActionRatioOfInitial()));
        assertEquals(0, new BigDecimal("21.09375").compareTo(plan.getRemainRatioAfterAction()));
        assertTrue(level(plan, "+7%").isCurrent());
        assertFalse(level(plan, "+8%").isTriggered());
        assertEquals("+8%", plan.getNextTriggerLabel());
        assertEquals(0, new BigDecimal("0.5000").compareTo(plan.getGapToNextPercent()));
    }

    @Test
    @DisplayName("盈利 10% 及以上：建议全部卖出剩余仓位，累计卖出达到 100%")
    void shouldSuggestLiquidationAtTenPercent() {
        RiskPlanVO plan = calculator.buildPlan(position("持仓B", "10.5000", "10000.00", "9000.00"));

        assertEquals("高位·建议清仓", plan.getRiskLevel());
        assertEquals("全部卖出", plan.getActionLabel());
        assertEquals(0, new BigDecimal("1186.52").compareTo(plan.getActionAmount()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(plan.getRemainRatioAfterAction()));
        assertEquals(7, plan.getTriggeredCount());
        assertNull(plan.getNextTriggerLabel());
    }

    @Test
    @DisplayName("亏损 6.5%：按止损阶梯触发 -3%/-5%/-6% 三档，当前建议按 -6% 档执行")
    void shouldMirrorLadderForLoss() {
        RiskPlanVO plan = calculator.buildPlan(position("持仓C", "-6.5000", "10000.00", "10700.00"));

        assertEquals("止损区", plan.getRiskLevel());
        assertEquals("-6%", level(plan, "-6%").getTriggerLabel());
        assertTrue(level(plan, "-6%").isCurrent());
        assertEquals(3, plan.getTriggeredCount());
        assertEquals("卖出 1/4", plan.getActionLabel());
        assertEquals(0, new BigDecimal("937.50").compareTo(plan.getActionAmount()));
        assertEquals(0, new BigDecimal("28.1250").compareTo(plan.getRemainRatioAfterAction()));
        // 下一档 -7%，还差 0.5 个百分点
        assertEquals("-7%", plan.getNextTriggerLabel());
        assertEquals(0, new BigDecimal("0.5000").compareTo(plan.getGapToNextPercent()));
        assertEquals("止损阶梯", level(plan, "-6%").getSideLabel());
        assertTrue(plan.getOppositeHint().startsWith("若转为盈利"));
    }

    @Test
    @DisplayName("盈亏在 ±3% 之间：未触发任何档位，建议持有观察并给出距下一档的差距")
    void shouldWatchWhenWithinThreshold() {
        RiskPlanVO profitSide = calculator.buildPlan(position("持仓D", "1.5000", "10000.00", "9850.00"));
        assertEquals("持有观察", profitSide.getRiskLevel());
        assertEquals("持有观察", profitSide.getActionLabel());
        assertNull(profitSide.getActionAmount());
        assertEquals(0, profitSide.getTriggeredCount());
        assertEquals("+3%", profitSide.getNextTriggerLabel());
        assertEquals(0, new BigDecimal("1.5000").compareTo(profitSide.getGapToNextPercent()));

        RiskPlanVO lossSide = calculator.buildPlan(position("持仓E", "-2.2000", "10000.00", "10200.00"));
        assertEquals("持有观察", lossSide.getRiskLevel());
        assertEquals("-3%", lossSide.getNextTriggerLabel());
        assertEquals(0, new BigDecimal("0.8000").compareTo(lossSide.getGapToNextPercent()));
        assertTrue(lossSide.getSuggestion().contains("距 -3% 档还差 0.8 个百分点"));
    }

    @Test
    @DisplayName("汇总统计：止盈/止损/观察方案数与建议卖出金额合计")
    void shouldSummarizePlans() {
        List<HoldingPositionVO> positions = Arrays.asList(
                position("止盈方案", "4.2000", "10000.00", "9600.00"),
                position("止损方案", "-5.5000", "8000.00", "8500.00"),
                position("观察方案", "0.6000", "5000.00", "4970.00"),
                position("清仓方案", "12.0000", "3000.00", "2600.00"));

        RiskOverviewVO overview = calculator.buildOverview(positions);
        RiskStatsVO stats = overview.getStats();

        assertEquals(4, stats.getPlanCount());
        assertEquals(2, stats.getTakeProfitCount());
        assertEquals(1, stats.getStopLossCount());
        assertEquals(1, stats.getWatchCount());
        assertEquals(1, stats.getLiquidationCount());
        assertEquals(0, stats.getUnavailableCount());
        assertEquals(0, new BigDecimal("3.0000").compareTo(stats.getTakeProfitTrigger()));
        assertEquals(0, new BigDecimal("-3.0000").compareTo(stats.getStopLossTrigger()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(stats.getTakeProfitClearTrigger()));
        assertEquals(0, new BigDecimal("-10.0000").compareTo(stats.getStopLossClearTrigger()));
        // 建议卖出金额合计 = 2500.00（+3% 档）+ 3000.00（-3% 档）+ 0 + 355.96（+10% 档，3000 × 11.865234375%）
        assertEquals(0, new BigDecimal("5855.96").compareTo(stats.getSuggestedSellAmount()));
        assertEquals(0, new BigDecimal("26000.00").compareTo(stats.getMarketValue()));
        assertEquals(0, new BigDecimal("25670.00").compareTo(stats.getTotalAmount()));
        assertEquals(4, overview.getPlans().size());
    }

    @Test
    @DisplayName("行情不可用时给出无法评估的说明，不影响其它方案")
    void shouldHandleUnavailableQuote() {
        HoldingPositionVO unavailable = position("无行情持仓", null, null, "10000.00");
        RiskPlanVO plan = calculator.buildPlan(unavailable);

        assertFalse(plan.isQuoteAvailable());
        assertEquals("无法评估", plan.getActionLabel());
        assertNull(plan.getActionAmount());
        assertTrue(plan.getSuggestion().contains("缺少最新点位"));

        List<HoldingPositionVO> positions = new ArrayList<HoldingPositionVO>();
        positions.add(unavailable);
        positions.add(position("正常持仓", "4.0000", "10000.00", "9600.00"));
        RiskOverviewVO overview = calculator.buildOverview(positions);
        assertEquals(1, overview.getStats().getUnavailableCount());
        assertEquals(1, overview.getStats().getTakeProfitCount());
    }

    private void assertLevel(RiskLevelVO level, String triggerLabel, String ratioLabel,
                             String cumulative, String cumulativePercent, String remainPercent) {
        assertEquals(triggerLabel, level.getTriggerLabel());
        assertEquals(ratioLabel, level.getSellRatioLabel());
        assertEquals(0, new BigDecimal(cumulative).compareTo(level.getCumulativeSellRatio()),
                "档位 " + triggerLabel + " 的累计卖出比例应为 " + cumulative);
        assertEquals(0, new BigDecimal(cumulativePercent).compareTo(
                level.getCumulativeSellRatio().multiply(BigDecimal.valueOf(100)).stripTrailingZeros()),
                "档位 " + triggerLabel + " 的累计卖出百分比应为 " + cumulativePercent);
        assertEquals(0, new BigDecimal(remainPercent).compareTo(
                level.getRemainRatioAfter().multiply(BigDecimal.valueOf(100)).stripTrailingZeros()),
                "档位 " + triggerLabel + " 的剩余仓位应为 " + remainPercent);
    }
}
