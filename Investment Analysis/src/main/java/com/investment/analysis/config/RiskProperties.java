package com.investment.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险评估模块参数，对应 application.yml 中的 risk.* 配置。
 * <p>阶梯规则：触发点为持仓盈亏比例（%），卖出比例为「当时剩余仓位」的比例，
 * 各档位依次执行后得到累计卖出比例与剩余仓位。</p>
 */
@ConfigurationProperties(prefix = "risk")
public class RiskProperties {

    /** 止盈阶梯（默认：+3% 卖 1/4、+5% 卖 1/2、+6%~+9% 每档卖剩余 1/4、+10% 全部卖出） */
    private List<Level> takeProfitLevels = defaultTakeProfitLevels();

    /** 止损阶梯（亏损方向，与止盈阶梯对称：-3% 卖 1/4、-5% 卖 1/2、-6%~-9% 每档卖剩余 1/4、-10% 全部卖出） */
    private List<Level> stopLossLevels = defaultStopLossLevels();

    /** 金额保留小数位 */
    private int amountScale = 2;

    private static List<Level> defaultTakeProfitLevels() {
        List<Level> levels = new ArrayList<Level>();
        levels.add(new Level(3D, 0.25D));
        levels.add(new Level(5D, 0.5D));
        levels.add(new Level(6D, 0.25D));
        levels.add(new Level(7D, 0.25D));
        levels.add(new Level(8D, 0.25D));
        levels.add(new Level(9D, 0.25D));
        levels.add(new Level(10D, 1D));
        return levels;
    }

    private static List<Level> defaultStopLossLevels() {
        List<Level> levels = new ArrayList<Level>();
        levels.add(new Level(-3D, 0.25D));
        levels.add(new Level(-5D, 0.5D));
        levels.add(new Level(-6D, 0.25D));
        levels.add(new Level(-7D, 0.25D));
        levels.add(new Level(-8D, 0.25D));
        levels.add(new Level(-9D, 0.25D));
        levels.add(new Level(-10D, 1D));
        return levels;
    }

    public List<Level> getTakeProfitLevels() {
        return takeProfitLevels;
    }

    public void setTakeProfitLevels(List<Level> takeProfitLevels) {
        this.takeProfitLevels = takeProfitLevels;
    }

    public List<Level> getStopLossLevels() {
        return stopLossLevels;
    }

    public void setStopLossLevels(List<Level> stopLossLevels) {
        this.stopLossLevels = stopLossLevels;
    }

    public int getAmountScale() {
        return amountScale;
    }

    public void setAmountScale(int amountScale) {
        this.amountScale = amountScale;
    }

    /**
     * 单个阶梯档位。
     */
    public static class Level {

        /** 触发点：持仓盈亏比例（%），止盈为正数、止损为负数 */
        private double triggerPercent;

        /** 卖出比例：占「触发时剩余仓位」的比例，1 表示全部卖出 */
        private double sellRatio;

        public Level() {
        }

        public Level(double triggerPercent, double sellRatio) {
            this.triggerPercent = triggerPercent;
            this.sellRatio = sellRatio;
        }

        public double getTriggerPercent() {
            return triggerPercent;
        }

        public void setTriggerPercent(double triggerPercent) {
            this.triggerPercent = triggerPercent;
        }

        public double getSellRatio() {
            return sellRatio;
        }

        public void setSellRatio(double sellRatio) {
            this.sellRatio = sellRatio;
        }
    }
}
