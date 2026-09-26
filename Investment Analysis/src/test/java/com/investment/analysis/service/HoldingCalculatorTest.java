package com.investment.analysis.service;

import com.investment.analysis.config.HoldingProperties;
import com.investment.analysis.entity.HoldingBuyRecord;
import com.investment.analysis.entity.HoldingPosition;
import com.investment.analysis.model.HoldingBuyRecordVO;
import com.investment.analysis.model.HoldingPositionVO;
import com.investment.analysis.model.HoldingStatsVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 持仓盈亏与胜率计算单元测试。
 * <p>规则：成本 = 买入日指数收盘点位；盈亏按最新点位计算；
 * 真实胜率 = 盈利笔数 ÷ 买入次数；预测胜率 = 每笔买入预测胜率之和 ÷ 买入次数。</p>
 */
class HoldingCalculatorTest {

    private final HoldingProperties properties = new HoldingProperties();
    private final HoldingCalculator calculator = new HoldingCalculator(properties);

    private HoldingPosition position(long id, String name, String indexCode) {
        HoldingPosition position = new HoldingPosition();
        position.setId(id);
        position.setPositionName(name);
        position.setIndexCode(indexCode);
        position.setIndexName("上证指数");
        return position;
    }

    private HoldingBuyRecord record(long id, long positionId, String buyDate, String amount,
                                    String costPrice, String buyWinRate) {
        HoldingBuyRecord record = new HoldingBuyRecord();
        record.setId(id);
        record.setPositionId(positionId);
        record.setBuyDate(LocalDate.parse(buyDate));
        record.setCostTradeDate(LocalDate.parse(buyDate));
        record.setBuyAmount(new BigDecimal(amount));
        record.setCostPrice(new BigDecimal(costPrice));
        if (buyWinRate != null) {
            record.setBuyWinRate(new BigDecimal(buyWinRate));
        }
        return record;
    }

    @Test
    @DisplayName("两笔盈利买入：加权平均成本、市值、盈亏、真实胜率与预测胜率均按规则计算")
    void shouldAggregateProfitablePosition() {
        List<HoldingBuyRecord> records = Arrays.asList(
                record(1L, 10L, "2026-01-05", "10000", "3000.000", "70"),
                record(2L, 10L, "2026-03-05", "10000", "3300.000", "80"));

        HoldingPositionVO vo = calculator.buildPosition(
                position(10L, "上证定投", "000001"), records, new BigDecimal("3600.000"),
                LocalDate.parse("2026-09-24"));

        assertEquals(2, vo.getBuyCount());
        assertEquals(0, new BigDecimal("20000.00").compareTo(vo.getTotalAmount()));
        assertEquals(0, new BigDecimal("3142.857").compareTo(vo.getAvgCostPrice()));
        assertEquals(0, new BigDecimal("22909.09").compareTo(vo.getMarketValue()));
        assertEquals(0, new BigDecimal("2909.09").compareTo(vo.getProfitAmount()));
        assertEquals(0, new BigDecimal("14.5455").compareTo(vo.getProfitPercent()));
        assertTrue(vo.isProfit());

        // 每笔买入的盈亏
        assertEquals(0, new BigDecimal("2000.00").compareTo(vo.getRecords().get(0).getProfitAmount()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(vo.getRecords().get(0).getChangePercent()));
        assertEquals(0, new BigDecimal("909.09").compareTo(vo.getRecords().get(1).getProfitAmount()));
        assertTrue(vo.getRecords().get(1).isWin());

        // 真实胜率 = 2 ÷ 2；预测胜率 = (70 + 80) ÷ 2
        assertEquals(0, new BigDecimal("100.0000").compareTo(vo.getRealWinRate()));
        assertEquals(0, new BigDecimal("75.0000").compareTo(vo.getPredictedWinRate()));
        assertEquals(2, vo.getWinCount());
        assertEquals(0, vo.getLossCount());

        // 预测胜率 >= 70% 的买入：2 笔，真实胜率 100%
        assertEquals(2, vo.getHighWinRateCount());
        assertEquals(0, new BigDecimal("100.0000").compareTo(vo.getHighWinRateRealWinRate()));
        assertEquals(0, new BigDecimal("75.0000").compareTo(vo.getHighWinRatePredictedWinRate()));
    }

    @Test
    @DisplayName("最新点位低于成本时该笔买入判定为亏损，真实胜率按盈利笔数统计")
    void shouldMarkLossWhenLatestBelowCost() {
        List<HoldingBuyRecord> records = Arrays.asList(
                record(1L, 20L, "2026-01-05", "10000", "3300.000", "80"),
                record(2L, 20L, "2026-02-05", "5000", "2800.000", "40"));

        HoldingPositionVO vo = calculator.buildPosition(
                position(20L, "创业板试探", "399006"), records, new BigDecimal("2900.000"),
                LocalDate.parse("2026-09-24"));

        assertFalse(vo.getRecords().get(0).isWin());
        assertEquals("亏损", vo.getRecords().get(0).getResultLabel());
        assertTrue(vo.getRecords().get(1).isWin());
        assertEquals(1, vo.getWinCount());
        assertEquals(1, vo.getLossCount());
        // 真实胜率 = 1 ÷ 2
        assertEquals(0, new BigDecimal("50.0000").compareTo(vo.getRealWinRate()));
        // 预测胜率 = (80 + 40) ÷ 2
        assertEquals(0, new BigDecimal("60.0000").compareTo(vo.getPredictedWinRate()));
        // 只有第 1 笔达到 70% 阈值，且当前亏损
        assertEquals(1, vo.getHighWinRateCount());
        assertEquals(0, new BigDecimal("0.0000").compareTo(vo.getHighWinRateRealWinRate()));
        assertFalse(vo.getRecords().get(0).isHighWinRateHit());
    }

    @Test
    @DisplayName("汇总统计：全部持仓的真实胜率、预测胜率与预测胜率 70 以上的真实胜率")
    void shouldSummarizeAllPositions() {
        List<HoldingBuyRecord> first = Arrays.asList(
                record(1L, 30L, "2026-01-05", "10000", "3000.000", "75"),
                record(2L, 30L, "2026-02-05", "10000", "3200.000", "65"));
        List<HoldingBuyRecord> second = Arrays.asList(
                record(3L, 31L, "2026-03-05", "20000", "3600.000", "90"),
                record(4L, 31L, "2026-04-05", "20000", "2600.000", "50"));

        List<HoldingPositionVO> positions = new ArrayList<HoldingPositionVO>();
        positions.add(calculator.buildPosition(position(30L, "持仓A", "000001"), first,
                new BigDecimal("3300.000"), LocalDate.parse("2026-09-24")));
        positions.add(calculator.buildPosition(position(31L, "持仓B", "399001"), second,
                new BigDecimal("3300.000"), LocalDate.parse("2026-09-24")));

        List<HoldingBuyRecordVO> allRecords = new ArrayList<HoldingBuyRecordVO>();
        for (HoldingPositionVO item : positions) {
            allRecords.addAll(item.getRecords());
        }

        HoldingStatsVO stats = calculator.buildStats(positions, allRecords);

        assertEquals(2, stats.getPositionCount());
        assertEquals(4, stats.getBuyCount());
        assertEquals(0, new BigDecimal("60000.00").compareTo(stats.getTotalAmount()));
        // 4 笔中 3 笔盈利（成本 3600 的持仓B 一笔亏损）
        assertEquals(3, stats.getWinCount());
        assertEquals(1, stats.getLossCount());
        assertEquals(0, new BigDecimal("75.0000").compareTo(stats.getRealWinRate()));
        // 预测胜率 = (75 + 65 + 90 + 50) ÷ 4
        assertEquals(0, new BigDecimal("70.0000").compareTo(stats.getPredictedWinRate()));
        assertEquals(4, stats.getPredictedRecordCount());
        // 预测胜率 >= 70% 的买入：第 1 笔（75，盈利）与第 3 笔（90，亏损）→ 真实胜率 50%
        assertEquals(2, stats.getHighWinRateCount());
        assertEquals(0, new BigDecimal("50.0000").compareTo(stats.getHighWinRateRealWinRate()));
        assertEquals(0, new BigDecimal("82.5000").compareTo(stats.getHighWinRatePredictedWinRate()));
        // 预测胜率 < 70% 的买入：65（盈利）与 50（盈利）→ 真实胜率 100%
        assertEquals(2, stats.getLowWinRateCount());
        assertEquals(0, new BigDecimal("100.0000").compareTo(stats.getLowWinRateRealWinRate()));
    }

    @Test
    @DisplayName("行情不可用时盈亏字段为空，仍保留买入金额与预测胜率")
    void shouldHandleUnavailableQuote() {
        List<HoldingBuyRecord> records = Arrays.asList(
                record(1L, 40L, "2026-01-05", "10000", "3000.000", "72"));

        HoldingPositionVO vo = calculator.buildPosition(position(40L, "无行情持仓", "000001"),
                records, null, null);

        assertEquals(1, vo.getBuyCount());
        assertEquals(0, new BigDecimal("10000.00").compareTo(vo.getTotalAmount()));
        assertEquals(null, vo.getMarketValue());
        assertEquals(null, vo.getProfitPercent());
        assertEquals("行情不可用", vo.getRecords().get(0).getResultLabel());
        assertEquals(0, new BigDecimal("72.0000").compareTo(vo.getPredictedWinRate()));
    }

    @Test
    @DisplayName("高预测胜率阈值按 70% 判定，取等号算达标")
    void shouldJudgeHighWinRateByThreshold() {
        assertTrue(calculator.isHighWinRate(new BigDecimal("70")));
        assertTrue(calculator.isHighWinRate(new BigDecimal("70.0001")));
        assertFalse(calculator.isHighWinRate(new BigDecimal("69.9999")));
        assertFalse(calculator.isHighWinRate(null));
        assertEquals(0, new BigDecimal("70.00").compareTo(calculator.getHighWinRateThreshold()));
    }
}
