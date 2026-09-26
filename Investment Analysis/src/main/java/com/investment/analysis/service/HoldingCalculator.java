package com.investment.analysis.service;

import com.investment.analysis.config.HoldingProperties;
import com.investment.analysis.entity.HoldingBuyRecord;
import com.investment.analysis.entity.HoldingPosition;
import com.investment.analysis.model.HoldingBuyRecordVO;
import com.investment.analysis.model.HoldingPositionVO;
import com.investment.analysis.model.HoldingStatsVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 持仓盈亏与胜率计算器（纯计算，无 IO）。
 * <pre>
 * 1) 买入成本：该笔买入日期对应交易日的指数收盘点位（下单时已落库的 cost_price）；
 * 2) 单笔盈亏：涨跌幅 = (最新点位 - 成本点位) ÷ 成本点位 × 100，
 *    盈亏金额 = 买入金额 × 涨跌幅 ÷ 100；
 * 3) 持仓成本：加权平均成本 = 累计买入金额 ÷ 累计份额（份额 = 每笔金额 ÷ 该笔成本点位）；
 *    持仓市值 = 累计份额 × 最新点位，持仓盈亏 = 市值 - 累计买入金额；
 * 4) 真实胜率：按最新点位判断当前盈利的买入笔数 ÷ 买入次数；
 * 5) 预测胜率：每笔买入「买入时算出的预测胜率」之和 ÷ 买入次数；
 * 6) 高预测胜率买入：只统计买入时预测胜率 >= 阈值的笔数及其真实胜率。
 * </pre>
 */
@Component
public class HoldingCalculator {

    private static final int SCALE_RATE = 4;
    private static final int SCALE_MONEY = 2;
    private static final int SCALE_PRICE = 3;
    private static final int SCALE_UNIT = 8;

    private final HoldingProperties properties;

    public HoldingCalculator(HoldingProperties properties) {
        this.properties = properties;
    }

    /**
     * 组装单个持仓的展示对象（含每笔买入明细与胜率统计）。
     *
     * @param position        持仓主表记录
     * @param buyRecords      该持仓下的买入记录（可为空列表）
     * @param latestClose     关联指数最新点位，行情不可用时为 null
     * @param latestTradeDate 关联指数最新交易日，行情不可用时为 null
     */
    public HoldingPositionVO buildPosition(HoldingPosition position,
                                           List<HoldingBuyRecord> buyRecords,
                                           BigDecimal latestClose,
                                           LocalDate latestTradeDate) {
        List<HoldingBuyRecord> records = buyRecords == null
                ? new ArrayList<HoldingBuyRecord>() : new ArrayList<HoldingBuyRecord>(buyRecords);
        records.sort(Comparator.comparing(HoldingBuyRecord::getBuyDate,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        HoldingPositionVO vo = new HoldingPositionVO();
        vo.setId(position.getId());
        vo.setPositionName(position.getPositionName());
        vo.setIndexCode(position.getIndexCode());
        vo.setIndexName(position.getIndexName());
        vo.setRemark(position.getRemark());
        vo.setLatestClose(round(latestClose, SCALE_PRICE));
        vo.setLatestTradeDate(latestTradeDate);
        vo.setBuyCount(records.size());

        List<HoldingBuyRecordVO> recordVOs = new ArrayList<HoldingBuyRecordVO>(records.size());
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalUnit = BigDecimal.ZERO;
        BigDecimal winRateSum = BigDecimal.ZERO;
        BigDecimal highWinRateSum = BigDecimal.ZERO;
        int winCount = 0;
        int lossCount = 0;
        int highCount = 0;
        int highWinCount = 0;

        for (HoldingBuyRecord record : records) {
            HoldingBuyRecordVO recordVO = buildRecord(record, position.getPositionName(),
                    latestClose, latestTradeDate);
            recordVOs.add(recordVO);

            BigDecimal amount = record.getBuyAmount() == null ? BigDecimal.ZERO : record.getBuyAmount();
            totalAmount = totalAmount.add(amount);
            if (record.getCostPrice() != null && record.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                totalUnit = totalUnit.add(amount.divide(record.getCostPrice(), SCALE_UNIT, RoundingMode.HALF_UP));
            }

            if (record.getBuyWinRate() != null) {
                winRateSum = winRateSum.add(record.getBuyWinRate());
                if (isHighWinRate(record.getBuyWinRate())) {
                    highCount++;
                    highWinRateSum = highWinRateSum.add(record.getBuyWinRate());
                    if (recordVO.isWin()) {
                        highWinCount++;
                    }
                }
            }

            if (recordVO.isWin()) {
                winCount++;
            } else {
                lossCount++;
            }
        }

        vo.setRecords(recordVOs);
        vo.setTotalAmount(totalAmount.setScale(SCALE_MONEY, RoundingMode.HALF_UP));

        // 加权平均成本与市值：份额 = 金额 ÷ 成本点位
        BigDecimal marketValue = null;
        BigDecimal profitAmount = null;
        BigDecimal avgCost = null;
        if (totalUnit.compareTo(BigDecimal.ZERO) > 0) {
            avgCost = totalAmount.divide(totalUnit, SCALE_PRICE, RoundingMode.HALF_UP);
            if (latestClose != null) {
                marketValue = totalUnit.multiply(latestClose).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                profitAmount = marketValue.subtract(totalAmount).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            }
        }
        vo.setAvgCostPrice(avgCost);
        vo.setMarketValue(marketValue);
        vo.setProfitAmount(profitAmount);
        if (marketValue != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            vo.setProfitPercent(round(profitAmount
                    .divide(totalAmount, SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
        }
        vo.setProfit(profitAmount != null && profitAmount.compareTo(BigDecimal.ZERO) > 0);
        if (avgCost != null && latestClose != null && avgCost.compareTo(BigDecimal.ZERO) > 0) {
            vo.setChangePercent(round(latestClose.subtract(avgCost)
                    .divide(avgCost, SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
        }

        if (!records.isEmpty()) {
            vo.setFirstBuyDate(records.get(0).getBuyDate());
            vo.setLastBuyDate(records.get(records.size() - 1).getBuyDate());
            // 预测胜率 = 每笔买入的预测胜率之和 ÷ 买入次数（未算出预测胜率的买入按 0 参与平均）
            vo.setPredictedWinRate(round(winRateSum.divide(
                    BigDecimal.valueOf(records.size()), SCALE_RATE, RoundingMode.HALF_UP), SCALE_RATE));
            vo.setRealWinRate(round(BigDecimal.valueOf(winCount)
                    .divide(BigDecimal.valueOf(records.size()), SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
        }
        vo.setWinCount(winCount);
        vo.setLossCount(lossCount);
        vo.setHighWinRateCount(highCount);
        if (highCount > 0) {
            vo.setHighWinRateRealWinRate(round(BigDecimal.valueOf(highWinCount)
                    .divide(BigDecimal.valueOf(highCount), SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
            vo.setHighWinRatePredictedWinRate(round(highWinRateSum.divide(
                    BigDecimal.valueOf(highCount), SCALE_RATE, RoundingMode.HALF_UP), SCALE_RATE));
        }
        return vo;
    }

    /**
     * 组装单笔买入记录：成本、买入时预测胜率、按最新点位计算的盈亏。
     */
    public HoldingBuyRecordVO buildRecord(HoldingBuyRecord record,
                                          String positionName,
                                          BigDecimal latestClose,
                                          LocalDate latestTradeDate) {
        HoldingBuyRecordVO vo = new HoldingBuyRecordVO();
        vo.setId(record.getId());
        vo.setPositionId(record.getPositionId());
        vo.setPositionName(positionName);
        vo.setBuyDate(record.getBuyDate());
        vo.setCostTradeDate(record.getCostTradeDate());
        vo.setBuyAmount(record.getBuyAmount());
        vo.setCostPrice(record.getCostPrice());
        vo.setBuyWinRate(record.getBuyWinRate());
        vo.setWinRatePeriodStart(record.getWinRatePeriodStart());
        vo.setWinRatePeriodEnd(record.getWinRatePeriodEnd());
        vo.setWinRateSampleCount(record.getWinRateSampleCount());
        vo.setWinRateFormula(record.getWinRateFormula());
        vo.setWinRateNote(record.getWinRateNote());
        vo.setLatestClose(round(latestClose, SCALE_PRICE));
        vo.setLatestTradeDate(latestTradeDate);

        BigDecimal costPrice = record.getCostPrice();
        BigDecimal amount = record.getBuyAmount() == null ? BigDecimal.ZERO : record.getBuyAmount();
        if (costPrice != null && costPrice.compareTo(BigDecimal.ZERO) > 0 && latestClose != null) {
            BigDecimal changePercent = round(latestClose.subtract(costPrice)
                    .divide(costPrice, SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE);
            BigDecimal profitAmount = round(changePercent.multiply(amount)
                    .divide(BigDecimal.valueOf(100), SCALE_MONEY + 2, RoundingMode.HALF_UP), SCALE_MONEY);
            vo.setChangePercent(changePercent);
            vo.setProfitAmount(profitAmount);
            vo.setWin(profitAmount.compareTo(BigDecimal.ZERO) > 0);
            vo.setResultLabel(profitAmount.compareTo(BigDecimal.ZERO) > 0
                    ? "盈利" : (profitAmount.compareTo(BigDecimal.ZERO) < 0 ? "亏损" : "持平"));
        } else {
            vo.setWin(false);
            vo.setResultLabel("行情不可用");
        }
        vo.setHighWinRateHit(vo.isWin() && record.getBuyWinRate() != null
                && isHighWinRate(record.getBuyWinRate()));
        return vo;
    }

    /**
     * 汇总统计：覆盖全部持仓的投入、市值、盈亏、真实胜率、预测胜率与高预测胜率买入表现。
     */
    public HoldingStatsVO buildStats(List<HoldingPositionVO> positions, List<HoldingBuyRecordVO> records) {
        HoldingStatsVO stats = new HoldingStatsVO();
        stats.setPositionCount(positions == null ? 0 : positions.size());
        stats.setHighWinRateThreshold(BigDecimal.valueOf(properties.getHighWinRateThreshold())
                .setScale(2, RoundingMode.HALF_UP));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal winRateSum = BigDecimal.ZERO;
        BigDecimal highWinRateSum = BigDecimal.ZERO;
        int buyCount = 0;
        int winCount = 0;
        int lossCount = 0;
        int predictedCount = 0;
        int highCount = 0;
        int highWinCount = 0;
        int lowCount = 0;
        int lowWinCount = 0;

        if (records != null) {
            for (HoldingBuyRecordVO record : records) {
                buyCount++;
                BigDecimal amount = record.getBuyAmount() == null ? BigDecimal.ZERO : record.getBuyAmount();
                totalAmount = totalAmount.add(amount);
                // 市值 = 买入金额 + 当前盈亏；行情不可用时按买入金额（不盈不亏）计入
                marketValue = marketValue.add(record.getProfitAmount() == null
                        ? amount : amount.add(record.getProfitAmount()));
                if (record.isWin()) {
                    winCount++;
                } else {
                    lossCount++;
                }
                BigDecimal winRate = record.getBuyWinRate();
                if (winRate != null) {
                    predictedCount++;
                    winRateSum = winRateSum.add(winRate);
                    if (isHighWinRate(winRate)) {
                        highCount++;
                        highWinRateSum = highWinRateSum.add(winRate);
                        if (record.isWin()) {
                            highWinCount++;
                        }
                    } else {
                        lowCount++;
                        if (record.isWin()) {
                            lowWinCount++;
                        }
                    }
                }
            }
        }

        stats.setBuyCount(buyCount);
        stats.setTotalAmount(totalAmount.setScale(SCALE_MONEY, RoundingMode.HALF_UP));
        stats.setMarketValue(marketValue.setScale(SCALE_MONEY, RoundingMode.HALF_UP));
        BigDecimal profit = marketValue.subtract(totalAmount).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        stats.setProfitAmount(profit);
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            stats.setProfitPercent(round(profit
                    .divide(totalAmount, SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
        }
        stats.setWinCount(winCount);
        stats.setLossCount(lossCount);
        stats.setPredictedRecordCount(predictedCount);
        if (buyCount > 0) {
            // 真实胜率 = 当前盈利的买入笔数 ÷ 买入总次数
            stats.setRealWinRate(round(BigDecimal.valueOf(winCount)
                    .divide(BigDecimal.valueOf(buyCount), SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
            // 预测胜率 = 每笔买入的预测胜率之和 ÷ 买入总次数
            stats.setPredictedWinRate(round(winRateSum.divide(
                    BigDecimal.valueOf(buyCount), SCALE_RATE, RoundingMode.HALF_UP), SCALE_RATE));
        }
        stats.setHighWinRateCount(highCount);
        if (highCount > 0) {
            stats.setHighWinRateRealWinRate(round(BigDecimal.valueOf(highWinCount)
                    .divide(BigDecimal.valueOf(highCount), SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
            stats.setHighWinRatePredictedWinRate(round(highWinRateSum.divide(
                    BigDecimal.valueOf(highCount), SCALE_RATE, RoundingMode.HALF_UP), SCALE_RATE));
        }
        stats.setLowWinRateCount(lowCount);
        if (lowCount > 0) {
            stats.setLowWinRateRealWinRate(round(BigDecimal.valueOf(lowWinCount)
                    .divide(BigDecimal.valueOf(lowCount), SCALE_RATE + 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)), SCALE_RATE));
        }
        return stats;
    }

    /** 预测胜率是否达到高胜率阈值 */
    public boolean isHighWinRate(BigDecimal winRate) {
        return winRate != null
                && winRate.compareTo(BigDecimal.valueOf(properties.getHighWinRateThreshold())) >= 0;
    }

    public BigDecimal getHighWinRateThreshold() {
        return BigDecimal.valueOf(properties.getHighWinRateThreshold()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
