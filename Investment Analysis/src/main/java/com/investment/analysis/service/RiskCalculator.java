package com.investment.analysis.service;

import com.investment.analysis.config.RiskProperties;
import com.investment.analysis.model.HoldingPositionVO;
import com.investment.analysis.model.RiskLevelVO;
import com.investment.analysis.model.RiskOverviewVO;
import com.investment.analysis.model.RiskPlanVO;
import com.investment.analysis.model.RiskStatsVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 风险评估计算器（纯计算，无 IO）：按持仓当前盈亏比例给出分档卖出建议。
 * <pre>
 * 止盈阶梯（默认，卖出比例为「触发时剩余仓位」的比例）：
 *   盈利 &gt; 3%  → 卖出剩余仓位的 1/4
 *   盈利 &gt; 5%  → 再卖出剩余仓位的 1/2
 *   盈利每再涨 1 个点（6%、7%、8%、9%）→ 每次卖出剩余仓位的 1/4
 *   盈利 10%    → 全部卖出（清仓）
 * 止损阶梯（亏损方向，规则完全对称）：
 *   亏损 &gt; 3%  → 卖出剩余仓位的 1/4
 *   亏损 &gt; 5%  → 再卖出剩余仓位的 1/2
 *   亏损每再扩大 1 个点（6%、7%、8%、9%）→ 每次卖出剩余仓位的 1/4
 *   亏损 10%    → 全部卖出（清仓）
 * 各档位依次执行：剩余仓位 = 1 - Σ(各档卖出量)，
 * 每档卖出金额（按当前市值折算）= 当前市值 × 该档卖出量占初始仓位的比例。
 * </pre>
 */
@Component
public class RiskCalculator {

    private static final int SCALE_RATE = 4;
    private static final int SCALE_MONEY = 2;
    /** 仓位比例保留位数：阶梯逐档相乘后需要足够精度，避免 1/4 连乘产生累计误差 */
    private static final int SCALE_RATIO = 12;
    /** 方案层「占比（%）」保留位数，与阶梯明细保持一致 */
    private static final int SCALE_PERCENT = 6;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final double EPS = 0.000001D;

    private final RiskProperties properties;

    public RiskCalculator(RiskProperties properties) {
        this.properties = properties;
    }

    /**
     * 针对全部持仓生成风险评估方案列表与汇总统计。
     */
    public RiskOverviewVO buildOverview(List<HoldingPositionVO> positions) {
        List<RiskPlanVO> plans = new ArrayList<RiskPlanVO>();
        if (positions != null) {
            for (HoldingPositionVO position : positions) {
                plans.add(buildPlan(position));
            }
        }
        RiskOverviewVO overview = new RiskOverviewVO();
        overview.setGeneratedAt(LocalDateTime.now());
        overview.setPlans(plans);
        overview.setStats(buildStats(plans));
        overview.setTakeProfitRule(describeLevels(takeProfitLevels(), true));
        overview.setStopLossRule(describeLevels(stopLossLevels(), false));
        overview.setCalcRule("评估口径：盈亏比例 = (关联指数最新点位 − 加权平均成本) ÷ 加权平均成本 × 100%；"
                + "各档「卖出比例」均为触发时剩余仓位的比例，逐档执行后得到累计卖出与剩余仓位；"
                + "建议卖出金额按当前市值折算（未扣除此前已卖出部分，实际执行请以账户剩余仓位为准）");
        return overview;
    }

    /**
     * 单个持仓的方案建议。
     */
    public RiskPlanVO buildPlan(HoldingPositionVO position) {
        RiskPlanVO plan = new RiskPlanVO();
        plan.setPositionId(position.getId());
        plan.setPositionName(position.getPositionName());
        plan.setIndexCode(position.getIndexCode());
        plan.setIndexName(position.getIndexName());
        plan.setTotalAmount(position.getTotalAmount());
        plan.setMarketValue(position.getMarketValue());
        plan.setAvgCostPrice(position.getAvgCostPrice());
        plan.setLatestClose(position.getLatestClose());
        plan.setLatestTradeDate(position.getLatestTradeDate());
        plan.setProfitAmount(position.getProfitAmount());
        plan.setProfitPercent(position.getProfitPercent());

        BigDecimal marketValue = position.getMarketValue();
        BigDecimal profitPercent = position.getProfitPercent();
        if (profitPercent == null || marketValue == null) {
            plan.setQuoteAvailable(false);
            plan.setRiskLevel("无法评估");
            plan.setRiskLevelTag("plain");
            plan.setActionLabel("无法评估");
            plan.setSuggestion("缺少最新点位，暂时无法评估该持仓的止盈/止损建议"
                    + (position.getLatestTradeDate() == null ? "（关联指数行情不可用）" : ""));
            plan.setOppositeHint(oppositeHint(false));
            plan.setTriggeredSummary("暂无");
            return plan;
        }

        plan.setQuoteAvailable(true);
        boolean profitSide = profitPercent.compareTo(BigDecimal.ZERO) >= 0;
        List<RiskProperties.Level> definitions = profitSide ? takeProfitLevels() : stopLossLevels();
        List<RiskLevelVO> levels = buildLadder(definitions, profitSide, profitPercent, marketValue);
        plan.setLevels(levels);

        int currentIndex = -1;
        int triggeredCount = 0;
        List<String> triggeredTexts = new ArrayList<String>();
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).isTriggered()) {
                currentIndex = i;
                triggeredCount++;
                triggeredTexts.add(levels.get(i).getTriggerLabel() + " 卖出 "
                        + levels.get(i).getSellRatioLabel());
            }
        }
        if (currentIndex >= 0) {
            levels.get(currentIndex).setCurrent(true);
        }
        plan.setTriggeredCount(triggeredCount);
        plan.setTriggeredSummary(triggeredTexts.isEmpty() ? "暂无（未触发任何档位）" : join(triggeredTexts, " → "));
        String riskLevel = resolveRiskLevel(profitPercent);
        plan.setRiskLevel(riskLevel);
        plan.setRiskLevelTag(riskLevelTag(riskLevel));

        if (currentIndex >= 0) {
            RiskLevelVO current = levels.get(currentIndex);
            boolean clearAll = isClearAll(current.getSellRatio());
            plan.setActionLabel(clearAll ? "全部卖出" : "卖出 " + current.getSellRatioLabel());
            plan.setActionAmount(current.getAmountAtCurrentValue());
            plan.setActionRatioOfInitial(percent(current.getSellRatioOfInitial()));
            plan.setRemainRatioAfterAction(percent(current.getRemainRatioAfter()));
            plan.setSuggestion(buildTriggeredSuggestion(current, clearAll, profitSide));
        } else {
            RiskLevelVO next = levels.isEmpty() ? null : levels.get(0);
            plan.setActionLabel("持有观察");
            plan.setActionAmount(null);
            plan.setActionRatioOfInitial(BigDecimal.ZERO.setScale(SCALE_PERCENT, RoundingMode.HALF_UP));
            plan.setRemainRatioAfterAction(BigDecimal.valueOf(100)
                    .setScale(SCALE_PERCENT, RoundingMode.HALF_UP));
            plan.setSuggestion(buildWatchSuggestion(next, profitPercent, profitSide));
        }

        int nextIndex = currentIndex + 1;
        if (nextIndex < levels.size()) {
            RiskLevelVO next = levels.get(nextIndex);
            plan.setNextTriggerPercent(next.getTriggerPercent());
            plan.setNextTriggerLabel(next.getTriggerLabel());
            plan.setGapToNextPercent(round(next.getTriggerPercent()
                    .subtract(profitPercent).abs(), SCALE_RATE));
            plan.setNextActionLabel(isClearAll(next.getSellRatio())
                    ? "全部卖出" : "卖出当时剩余仓位的 " + next.getSellRatioLabel());
            plan.setNextActionAmount(next.getAmountAtCurrentValue());
        }
        plan.setOppositeHint(oppositeHint(profitSide));
        return plan;
    }

    /**
     * 生成某一方向的完整阶梯：逐档计算卖出量占初始仓位的比例、累计卖出与剩余仓位。
     */
    private List<RiskLevelVO> buildLadder(List<RiskProperties.Level> definitions,
                                          boolean profitSide,
                                          BigDecimal profitPercent,
                                          BigDecimal marketValue) {
        List<RiskLevelVO> levels = new ArrayList<RiskLevelVO>();
        BigDecimal remain = BigDecimal.ONE;
        BigDecimal cumulative = BigDecimal.ZERO;
        for (RiskProperties.Level definition : definitions) {
            BigDecimal trigger = BigDecimal.valueOf(definition.getTriggerPercent())
                    .setScale(SCALE_RATE, RoundingMode.HALF_UP);
            BigDecimal sellRatio = clampRatio(BigDecimal.valueOf(definition.getSellRatio()));

            BigDecimal remainBefore = remain;
            BigDecimal sellOfInitial = remainBefore.multiply(sellRatio);
            cumulative = cumulative.add(sellOfInitial);
            remain = remainBefore.subtract(sellOfInitial);

            boolean triggered = profitSide
                    ? profitPercent.compareTo(trigger) >= 0
                    : profitPercent.compareTo(trigger) <= 0;

            RiskLevelVO level = new RiskLevelVO();
            level.setSide(profitSide ? "PROFIT" : "LOSS");
            level.setSideLabel(profitSide ? "止盈阶梯" : "止损阶梯");
            level.setTriggerPercent(trigger);
            level.setTriggerLabel(signedPercent(trigger));
            level.setSellRatio(sellRatio);
            level.setSellRatioLabel(ratioLabel(sellRatio));
            level.setRemainRatioBefore(round(remainBefore, SCALE_RATIO));
            level.setSellRatioOfInitial(round(sellOfInitial, SCALE_RATIO));
            level.setCumulativeSellRatio(round(cumulative, SCALE_RATIO));
            level.setRemainRatioAfter(round(remain, SCALE_RATIO));
            level.setAmountAtCurrentValue(marketValue == null
                    ? null : round(marketValue.multiply(sellOfInitial), SCALE_MONEY));
            level.setTriggered(triggered);
            level.setStatus(triggered ? "已触发" : "未触发");
            level.setRemark(buildLevelRemark(trigger, sellRatio, sellOfInitial, profitSide));
            levels.add(level);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
        return levels;
    }

    private String buildLevelRemark(BigDecimal trigger, BigDecimal sellRatio,
                                    BigDecimal sellOfInitial, boolean profitSide) {
        String direction = profitSide ? "盈利超过 " : "亏损超过 ";
        String action = isClearAll(sellRatio)
                ? "全部卖出（清仓）"
                : "卖出当时剩余仓位的 " + ratioLabel(sellRatio);
        return direction + trigger.abs().stripTrailingZeros().toPlainString() + "% 时，" + action
                + "，相当于初始仓位的 " + percentText(sellOfInitial, SCALE_RATE);
    }

    private String buildTriggeredSuggestion(RiskLevelVO current, boolean clearAll, boolean profitSide) {
        String amount = current.getAmountAtCurrentValue() == null
                ? "" : "，按当前市值折合约 " + current.getAmountAtCurrentValue().toPlainString() + " 元";
        if (clearAll) {
            return "已到 " + current.getTriggerLabel() + " 档：建议全部卖出该持仓剩余仓位（"
                    + percentText(current.getRemainRatioBefore()) + " 的仓位" + amount + "），锁定"
                    + (profitSide ? "利润" : "剩余本金");
        }
        return "已触发 " + current.getTriggerLabel() + " 档：建议卖出当时剩余仓位的 "
                + current.getSellRatioLabel() + amount + "，执行后剩余仓位 "
                + percentText(current.getRemainRatioAfter());
    }

    private String buildWatchSuggestion(RiskLevelVO next, BigDecimal profitPercent, boolean profitSide) {
        if (next == null) {
            return "阶梯规则为空，暂无法给出建议";
        }
        BigDecimal gap = round(next.getTriggerPercent().subtract(profitPercent).abs(), SCALE_RATE);
        return "未触发任何档位：建议继续持有观察；距 " + next.getTriggerLabel() + " 档还差 "
                + gap.stripTrailingZeros().toPlainString() + " 个百分点（触发后"
                + (isClearAll(next.getSellRatio()) ? "全部卖出" : "卖出剩余仓位的 " + next.getSellRatioLabel())
                + "）";
    }

    private RiskStatsVO buildStats(List<RiskPlanVO> plans) {
        RiskStatsVO stats = new RiskStatsVO();
        stats.setPlanCount(plans.size());
        stats.setTakeProfitTrigger(firstTrigger(true));
        stats.setStopLossTrigger(firstTrigger(false));
        stats.setTakeProfitClearTrigger(lastTrigger(true));
        stats.setStopLossClearTrigger(lastTrigger(false));

        BigDecimal suggested = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal marketValue = BigDecimal.ZERO;
        int takeProfit = 0;
        int stopLoss = 0;
        int watch = 0;
        int liquidation = 0;
        int unavailable = 0;

        for (RiskPlanVO plan : plans) {
            if (!plan.isQuoteAvailable() || plan.getProfitPercent() == null) {
                unavailable++;
                continue;
            }
            BigDecimal percent = plan.getProfitPercent();
            if (plan.getMarketValue() != null) {
                marketValue = marketValue.add(plan.getMarketValue());
            }
            if (plan.getTotalAmount() != null) {
                totalAmount = totalAmount.add(plan.getTotalAmount());
            }
            if (plan.getActionAmount() != null) {
                suggested = suggested.add(plan.getActionAmount());
            }
            boolean clearAll = "全部卖出".equals(plan.getActionLabel());
            if (clearAll) {
                liquidation++;
            }
            if (percent.compareTo(stats.getTakeProfitTrigger()) >= 0) {
                takeProfit++;
            } else if (percent.compareTo(stats.getStopLossTrigger()) <= 0) {
                stopLoss++;
            } else {
                watch++;
            }
        }

        stats.setTakeProfitCount(takeProfit);
        stats.setStopLossCount(stopLoss);
        stats.setWatchCount(watch);
        stats.setLiquidationCount(liquidation);
        stats.setUnavailableCount(unavailable);
        stats.setSuggestedSellAmount(suggested.setScale(SCALE_MONEY, RoundingMode.HALF_UP));
        stats.setTotalAmount(totalAmount.setScale(SCALE_MONEY, RoundingMode.HALF_UP));
        stats.setMarketValue(marketValue.setScale(SCALE_MONEY, RoundingMode.HALF_UP));
        BigDecimal profit = marketValue.subtract(totalAmount).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        stats.setProfitAmount(profit);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            stats.setProfitPercent(round(profit.divide(totalAmount, SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED), SCALE_RATE));
        }
        return stats;
    }

    private String resolveRiskLevel(BigDecimal profitPercent) {
        BigDecimal lastProfit = lastTrigger(true);
        BigDecimal firstLoss = firstTrigger(false);
        BigDecimal lastLoss = lastTrigger(false);
        if (lastProfit != null && profitPercent.compareTo(lastProfit) >= 0) {
            return "高位·建议清仓";
        }
        if (profitPercent.compareTo(firstTrigger(true)) >= 0) {
            return "止盈区";
        }
        if (lastLoss != null && profitPercent.compareTo(lastLoss) <= 0) {
            return "深亏·建议清仓";
        }
        if (profitPercent.compareTo(firstLoss) <= 0) {
            return "止损区";
        }
        return "持有观察";
    }

    private String riskLevelTag(String riskLevel) {
        if ("止盈区".equals(riskLevel)) {
            return "mid";
        }
        if ("止损区".equals(riskLevel) || "深亏·建议清仓".equals(riskLevel)
                || "高位·建议清仓".equals(riskLevel)) {
            return "bad";
        }
        return "plain";
    }

    private String oppositeHint(boolean profitSide) {
        List<RiskProperties.Level> opposite = profitSide ? stopLossLevels() : takeProfitLevels();
        if (opposite.isEmpty()) {
            return "";
        }
        RiskProperties.Level first = opposite.get(0);
        RiskProperties.Level last = opposite.get(opposite.size() - 1);
        String direction = profitSide ? "若转为亏损" : "若转为盈利";
        String ladder = profitSide ? "止损阶梯" : "止盈阶梯";
        return direction + "，则按" + ladder + "执行："
                + signedPercent(BigDecimal.valueOf(first.getTriggerPercent())) + " 卖出剩余仓位的 "
                + ratioLabel(BigDecimal.valueOf(first.getSellRatio())) + " …… "
                + signedPercent(BigDecimal.valueOf(last.getTriggerPercent())) + " 全部卖出";
    }

    /** 阶梯规则说明文本 */
    public String describeLevels(List<RiskProperties.Level> levels, boolean profitSide) {
        List<String> texts = new ArrayList<String>();
        for (RiskProperties.Level level : levels) {
            BigDecimal trigger = BigDecimal.valueOf(level.getTriggerPercent());
            BigDecimal ratio = BigDecimal.valueOf(level.getSellRatio());
            texts.add(signedPercent(trigger) + (profitSide ? "（盈利超过 "
                    + trigger.abs().stripTrailingZeros().toPlainString() + "%）" : "（亏损超过 "
                    + trigger.abs().stripTrailingZeros().toPlainString() + "%）")
                    + "：" + (isClearAll(ratio) ? "全部卖出" : "卖出剩余仓位的 " + ratioLabel(ratio)));
        }
        return join(texts, "；") + "。";
    }

    /**
     * 阶梯预览（用于规则面板）：不代入具体持仓，给出各档卖出比例、累计卖出与剩余仓位。
     */
    public List<RiskLevelVO> previewLadder(boolean profitSide) {
        List<RiskProperties.Level> definitions = profitSide ? takeProfitLevels() : stopLossLevels();
        // 传入一个不会触发任何档位的盈亏比例，仅用于展示阶梯结构
        BigDecimal neutral = profitSide
                ? BigDecimal.valueOf(-999D) : BigDecimal.valueOf(999D);
        return buildLadder(definitions, profitSide, neutral, null);
    }

    private List<RiskProperties.Level> takeProfitLevels() {
        List<RiskProperties.Level> levels = new ArrayList<RiskProperties.Level>(
                properties.getTakeProfitLevels() == null
                        ? new ArrayList<RiskProperties.Level>() : properties.getTakeProfitLevels());
        levels.sort(Comparator.comparingDouble(RiskProperties.Level::getTriggerPercent));
        return levels;
    }

    private List<RiskProperties.Level> stopLossLevels() {
        List<RiskProperties.Level> levels = new ArrayList<RiskProperties.Level>(
                properties.getStopLossLevels() == null
                        ? new ArrayList<RiskProperties.Level>() : properties.getStopLossLevels());
        // 亏损方向按「-3% → -5% → …… → -10%」由浅到深排列
        levels.sort(Comparator.comparingDouble(RiskProperties.Level::getTriggerPercent).reversed());
        return levels;
    }

    private BigDecimal firstTrigger(boolean profitSide) {
        List<RiskProperties.Level> sorted = profitSide ? takeProfitLevels() : stopLossLevels();
        return sorted.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(sorted.get(0).getTriggerPercent()).setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    private BigDecimal lastTrigger(boolean profitSide) {
        List<RiskProperties.Level> sorted = profitSide ? takeProfitLevels() : stopLossLevels();
        return sorted.isEmpty() ? null
                : BigDecimal.valueOf(sorted.get(sorted.size() - 1).getTriggerPercent())
                .setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    private boolean isClearAll(BigDecimal sellRatio) {
        return sellRatio != null && sellRatio.compareTo(BigDecimal.ONE) >= 0;
    }

    private BigDecimal clampRatio(BigDecimal ratio) {
        if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE_RATE, RoundingMode.HALF_UP);
        }
        if (ratio.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(SCALE_RATE, RoundingMode.HALF_UP);
        }
        return ratio.setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }

    /** 比例文本：1/4、1/2、3/4、2/3、全部，其余按百分比显示 */
    public String ratioLabel(BigDecimal ratio) {
        if (ratio == null) {
            return "--";
        }
        double value = ratio.doubleValue();
        if (Math.abs(value - 1D) < EPS) {
            return "全部";
        }
        if (Math.abs(value - 0.75D) < EPS) {
            return "3/4";
        }
        if (Math.abs(value - 2D / 3D) < EPS) {
            return "2/3";
        }
        if (Math.abs(value - 0.5D) < EPS) {
            return "1/2";
        }
        if (Math.abs(value - 1D / 3D) < EPS) {
            return "1/3";
        }
        if (Math.abs(value - 0.25D) < EPS) {
            return "1/4";
        }
        return percentText(ratio);
    }

    private String signedPercent(BigDecimal value) {
        BigDecimal abs = value.abs().stripTrailingZeros();
        String text = abs.scale() < 0 ? abs.setScale(0).toPlainString() : abs.toPlainString();
        return (value.compareTo(BigDecimal.ZERO) < 0 ? "-" : "+") + text + "%";
    }

    private String percentText(BigDecimal ratio) {
        BigDecimal percent = ratio.multiply(ONE_HUNDRED).stripTrailingZeros();
        String text = percent.scale() < 0 ? percent.setScale(0).toPlainString() : percent.toPlainString();
        return text + "%";
    }

    /** 指定小数位的百分比文本，用于说明文案（避免出现过长的小数） */
    private String percentText(BigDecimal ratio, int scale) {
        BigDecimal percent = ratio.multiply(ONE_HUNDRED)
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        String text = percent.scale() < 0 ? percent.setScale(0).toPlainString() : percent.toPlainString();
        return text + "%";
    }

    private BigDecimal percent(BigDecimal ratio) {
        return round(ratio.multiply(ONE_HUNDRED), SCALE_PERCENT);
    }

    private String join(List<String> texts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String text : texts) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
