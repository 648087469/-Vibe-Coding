package com.investment.analysis.service;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.model.IndexWinRateVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 投资胜率计算器（纯计算，无 IO）。
 * <p>计算规则：</p>
 * <pre>
 * 1) 取最近 365 天的指数数值（收盘点位），得到窗口最低值 min、最高值 max；
 * 2) 基础胜率：位于最低点记 90%，位于最高点记 10%，区间内按位置线性均分：
 *    基础胜率 = 10 + (90 - 10) * (max - 最新值) / (max - min)
 * 3) 最新一天对比 7 天前的涨跌幅 changePercent（%），上涨为正、下跌为负；
 * 4) 涨跌幅反向作用于胜率：指数上涨 -> 下调胜率，指数下跌 -> 上调胜率，
 *    最终胜率 = 基础胜率 - changePercent * 10（默认裁剪到 [0, 100]）。
 * </pre>
 */
@Component
public class WinRateCalculator {

    private static final int SCALE_RATE = 4;
    private static final int SCALE_RATIO = 6;

    private final AnalysisProperties properties;

    public WinRateCalculator(AnalysisProperties properties) {
        this.properties = properties;
    }

    /**
     * @param definition 指数定义
     * @param series     按交易日升序排列的日线序列（非空）
     */
    public IndexWinRateVO calculate(IndexDefinition definition, List<IndexDaily> series) {
        if (series == null || series.isEmpty()) {
            throw new IllegalArgumentException("指数 " + definition.getName() + " 无可用数据，无法计算胜率");
        }

        IndexDaily latest = series.get(series.size() - 1);
        IndexDaily lowest = series.get(0);
        IndexDaily highest = series.get(0);
        for (IndexDaily item : series) {
            if (item.getClosePrice().compareTo(lowest.getClosePrice()) < 0) {
                lowest = item;
            }
            if (item.getClosePrice().compareTo(highest.getClosePrice()) > 0) {
                highest = item;
            }
        }

        BigDecimal latestClose = latest.getClosePrice();
        BigDecimal minClose = lowest.getClosePrice();
        BigDecimal maxClose = highest.getClosePrice();
        BigDecimal range = maxClose.subtract(minClose);

        BigDecimal positionRatio;
        BigDecimal baseWinRate;
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            // 窗口内指数完全走平：最低值与最高值重合，取高低点中间胜率
            positionRatio = BigDecimal.ZERO.setScale(SCALE_RATIO, RoundingMode.HALF_UP);
            baseWinRate = round(BigDecimal.valueOf(properties.getMinWinRate())
                    .add(BigDecimal.valueOf(properties.getMaxWinRate()))
                    .divide(BigDecimal.valueOf(2), SCALE_RATE, RoundingMode.HALF_UP));
        } else {
            positionRatio = maxClose.subtract(latestClose)
                    .divide(range, SCALE_RATIO, RoundingMode.HALF_UP);
            BigDecimal span = BigDecimal.valueOf(properties.getMinWinRate())
                    .subtract(BigDecimal.valueOf(properties.getMaxWinRate()));
            baseWinRate = round(BigDecimal.valueOf(properties.getMaxWinRate())
                    .add(span.multiply(positionRatio)));
        }

        IndexDaily compare = pickCompareBar(series);
        BigDecimal changePercent = BigDecimal.ZERO.setScale(SCALE_RATE, RoundingMode.HALF_UP);
        BigDecimal compareClose = compare == null ? null : compare.getClosePrice();
        if (compareClose != null && compareClose.compareTo(BigDecimal.ZERO) != 0) {
            changePercent = round(latestClose.subtract(compareClose)
                    .divide(compareClose, SCALE_RATIO, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        // 涨跌幅百分比 × 换算系数，再按方向作用于胜率：
        // 反向（默认）时取负号 —— 指数涨了扣胜率、跌了加胜率
        BigDecimal changeAdjust = round(changePercent
                .multiply(BigDecimal.valueOf(properties.getChangeMultiplier()))
                .multiply(properties.isInverseChange() ? BigDecimal.valueOf(-1) : BigDecimal.ONE));
        BigDecimal rawWinRate = round(baseWinRate.add(changeAdjust));
        BigDecimal finalWinRate = properties.isClampWinRate() ? clamp(rawWinRate) : rawWinRate;

        IndexWinRateVO vo = new IndexWinRateVO();
        vo.setCode(definition.getCode());
        vo.setName(definition.getName());
        vo.setMarket(definition.getMarket());
        vo.setLatestTradeDate(latest.getTradeDate());
        vo.setLatestClose(round(latestClose));
        vo.setPeriodStart(series.get(0).getTradeDate());
        vo.setPeriodEnd(latest.getTradeDate());
        vo.setSampleCount(series.size());
        vo.setMinClose(round(minClose));
        vo.setMinCloseDate(lowest.getTradeDate());
        vo.setMaxClose(round(maxClose));
        vo.setMaxCloseDate(highest.getTradeDate());
        vo.setPositionRatio(positionRatio);
        vo.setFromMinPercent(percentFrom(minClose, latestClose));
        vo.setFromMaxPercent(percentFrom(maxClose, latestClose));
        vo.setBaseWinRate(baseWinRate);
        vo.setCompareTradeDate(compare == null ? null : compare.getTradeDate());
        vo.setCompareClose(compareClose == null ? null : round(compareClose));
        vo.setChangePercent(changePercent);
        vo.setChangeAdjust(changeAdjust);
        vo.setRawWinRate(rawWinRate);
        vo.setWinRate(finalWinRate);
        vo.setBaseFormula(buildBaseFormula(latestClose, minClose, maxClose, baseWinRate, range));
        vo.setChangeFormula(buildChangeFormula(changePercent, changeAdjust));
        vo.setFinalFormula(buildFinalFormula(baseWinRate, changeAdjust, rawWinRate, finalWinRate));
        vo.setValueLevel(resolveValueLevel(finalWinRate));
        return vo;
    }

    /**
     * 基础胜率算式（代入实际点位，便于核对「最低点 90%、最高点 10%、中间线性均分」）。
     */
    private String buildBaseFormula(BigDecimal latestClose, BigDecimal minClose, BigDecimal maxClose,
                                    BigDecimal baseWinRate, BigDecimal range) {
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return "窗口内最高值 = 最低值（" + text(maxClose) + "），取高低点中间胜率 " + text(baseWinRate) + "%";
        }
        return text(properties.getMaxWinRate()) + "% + ("
                + text(properties.getMinWinRate()) + "% - " + text(properties.getMaxWinRate()) + "%) × ("
                + text(maxClose) + " - " + text(latestClose) + ") ÷ ("
                + text(maxClose) + " - " + text(minClose) + ") = " + text(baseWinRate) + "%";
    }

    /**
     * 涨跌幅换算算式：使用「百分比」而非「点数」乘以系数，并按方向作用于胜率。
     */
    private String buildChangeFormula(BigDecimal changePercent, BigDecimal changeAdjust) {
        int direction = changePercent.compareTo(BigDecimal.ZERO);
        String trend = direction > 0 ? "上涨" : (direction < 0 ? "下跌" : "持平");
        BigDecimal magnitude = changePercent.multiply(BigDecimal.valueOf(properties.getChangeMultiplier()));
        StringBuilder formula = new StringBuilder();
        formula.append("7日涨跌幅 ").append(text(changePercent)).append("%（").append(trend).append("）")
                .append(" × ").append(text(properties.getChangeMultiplier()))
                .append(" = ").append(text(round(magnitude)));
        if (properties.isInverseChange()) {
            formula.append("；反向作用于胜率（涨则减、跌则加） → 调整项 ")
                    .append(text(changeAdjust));
        } else {
            formula.append("；同向作用于胜率 → 调整项 ").append(text(changeAdjust));
        }
        return formula.toString();
    }

    private String buildFinalFormula(BigDecimal baseWinRate, BigDecimal changeAdjust,
                                     BigDecimal rawWinRate, BigDecimal finalWinRate) {
        String operator = changeAdjust.compareTo(BigDecimal.ZERO) >= 0 ? " + " : " - ";
        String formula = "基础胜率 " + text(baseWinRate) + "%" + operator
                + "涨跌幅调整 " + text(changeAdjust.abs()) + " = " + text(rawWinRate) + "%";
        if (rawWinRate.compareTo(finalWinRate) != 0) {
            formula = formula + "，超出 0% ~ 100% 区间，裁剪为 " + text(finalWinRate) + "%";
        }
        return formula;
    }

    private String text(BigDecimal value) {
        return value == null ? "--" : value.stripTrailingZeros().toPlainString();
    }

    private String text(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * 选择「7 天前」的对比基准。
     * DATE 模式：最新交易日回溯 7 个自然日后，取日期不超过该日的最接近交易日；
     * BAR 模式：直接取倒数第 8 根 K 线（7 个交易日前）。
     */
    private IndexDaily pickCompareBar(List<IndexDaily> series) {
        if (series.size() < 2) {
            return null;
        }
        int window = Math.max(1, properties.getChangeWindowDays());
        if ("BAR".equalsIgnoreCase(properties.getChangeWindowMode())) {
            int index = series.size() - 1 - window;
            return series.get(Math.max(0, index));
        }
        LocalDate target = series.get(series.size() - 1).getTradeDate().minusDays(window);
        IndexDaily candidate = series.get(0);
        for (IndexDaily item : series) {
            if (!item.getTradeDate().isAfter(target)) {
                candidate = item;
            } else {
                break;
            }
        }
        return candidate;
    }

    private BigDecimal percentFrom(BigDecimal base, BigDecimal value) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return round(value.subtract(base)
                .divide(base, SCALE_RATIO, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal clamp(BigDecimal value) {
        BigDecimal low = BigDecimal.ZERO.setScale(SCALE_RATE, RoundingMode.HALF_UP);
        BigDecimal high = BigDecimal.valueOf(100).setScale(SCALE_RATE, RoundingMode.HALF_UP);
        if (value.compareTo(low) < 0) {
            return low;
        }
        if (value.compareTo(high) > 0) {
            return high;
        }
        return value;
    }

    private String resolveValueLevel(BigDecimal winRate) {
        double value = winRate.doubleValue();
        if (value >= 70D) {
            return "低位·机会区间";
        }
        if (value >= 55D) {
            return "偏低位";
        }
        if (value >= 45D) {
            return "中性区间";
        }
        if (value >= 30D) {
            return "偏高位";
        }
        return "高位·风险区间";
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE_RATE, RoundingMode.HALF_UP);
    }
}
