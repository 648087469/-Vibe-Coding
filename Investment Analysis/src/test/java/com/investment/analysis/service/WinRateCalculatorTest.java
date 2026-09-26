package com.investment.analysis.service;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.model.IndexWinRateVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 胜率计算公式单元测试。
 */
class WinRateCalculatorTest {

    private final AnalysisProperties properties = new AnalysisProperties();
    private final WinRateCalculator calculator = new WinRateCalculator(properties);

    private IndexDaily bar(String date, String close) {
        IndexDaily daily = new IndexDaily();
        daily.setIndexCode("000001");
        daily.setIndexName("上证指数");
        daily.setTradeDate(LocalDate.parse(date));
        daily.setClosePrice(new BigDecimal(close));
        return daily;
    }

    /**
     * 构造 20 个连续交易日：首日 100（窗口最低），次日 200（窗口最高），其余为 150。
     */
    private List<IndexDaily> buildSeries(String compareDateClose) {
        List<IndexDaily> series = new ArrayList<IndexDaily>();
        LocalDate start = LocalDate.parse("2026-01-01");
        for (int i = 0; i < 20; i++) {
            LocalDate date = start.plusDays(i);
            String close = "150";
            if (i == 0) {
                close = "100";
            } else if (i == 1) {
                close = "200";
            }
            if (compareDateClose != null && date.equals(LocalDate.parse("2026-01-13"))) {
                close = compareDateClose;
            }
            if (i == 19) {
                close = "150";
            }
            series.add(bar(date.toString(), close));
        }
        return series;
    }

    @Test
    @DisplayName("最低点 90%、最高点 10%、中间线性均分：位于区间中点时基础胜率为 50%")
    void shouldReturnHalfWinRateAtMiddlePosition() {
        IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, buildSeries(null));

        assertEquals(0, new BigDecimal("50.0000").compareTo(vo.getBaseWinRate()),
                "窗口最低 100、最高 200、最新 150 时基础胜率应为 50%");
        assertEquals(0, new BigDecimal("0.0000").compareTo(vo.getChangePercent()),
                "对比基准与最新值同为 150，涨跌幅应为 0");
        assertEquals(0, new BigDecimal("50.0000").compareTo(vo.getWinRate()),
                "最终胜率 = 50 + 0 × 10 = 50%");
        assertEquals(LocalDate.parse("2026-01-01"), vo.getMinCloseDate());
        assertEquals(LocalDate.parse("2026-01-02"), vo.getMaxCloseDate());
        assertEquals(LocalDate.parse("2026-01-13"), vo.getCompareTradeDate(),
                "7 天前（自然日回溯）应取到 2026-01-13");
    }

    @Test
    @DisplayName("最新值等于窗口最低值时，基础胜率为 90%")
    void shouldReturnNinetyWhenLatestEqualsLowest() {
        // 首日为最低点，同时作为最新交易日
        List<IndexDaily> series = new ArrayList<IndexDaily>();
        series.add(bar("2026-01-01", "100"));
        series.add(bar("2026-01-08", "300"));
        IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, series);

        assertEquals(LocalDate.parse("2026-01-08"), vo.getLatestTradeDate());
        assertEquals(0, new BigDecimal("10.0000").compareTo(vo.getBaseWinRate()),
                "最新值等于最高值 300 时基础胜率应为 10%");

        List<IndexDaily> reversed = new ArrayList<IndexDaily>();
        reversed.add(bar("2026-01-01", "300"));
        reversed.add(bar("2026-01-08", "100"));
        IndexWinRateVO second = calculator.calculate(IndexDefinition.SHANGHAI, reversed);
        assertEquals(0, new BigDecimal("90.0000").compareTo(second.getBaseWinRate()),
                "最新值等于最低值 100 时基础胜率应为 90%");
    }

    @Test
    @DisplayName("指数上涨时下调胜率：涨跌幅 +20% → 调整 -200 → 结果裁剪为 0%")
    void shouldSubtractWinRateWhenIndexRises() {
        // 对比基准 125，最新 150 → 涨跌幅 +20% → 反向调整 -200 → 基础 50 - 200 = -150 → 裁剪为 0
        IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, buildSeries("125"));

        assertEquals(0, new BigDecimal("20.0000").compareTo(vo.getChangePercent()));
        assertEquals(0, new BigDecimal("-200.0000").compareTo(vo.getChangeAdjust()),
                "指数上涨时调整项应为负数");
        assertEquals(0, new BigDecimal("-150.0000").compareTo(vo.getRawWinRate()));
        assertEquals(0, new BigDecimal("0.0000").compareTo(vo.getWinRate()),
                "低于 0% 的胜率应被裁剪为 0%");
    }

    @Test
    @DisplayName("指数下跌时上调胜率：涨跌幅 -25% → 调整 +250 → 结果裁剪为 100%")
    void shouldAddWinRateWhenIndexFalls() {
        // 对比基准 200，最新 150 → 涨跌幅 -25% → 反向调整 +250 → 基础 50 + 250 = 300 → 裁剪为 100
        IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, buildSeries("200"));

        assertEquals(0, new BigDecimal("-25.0000").compareTo(vo.getChangePercent()));
        assertEquals(0, new BigDecimal("250.0000").compareTo(vo.getChangeAdjust()),
                "指数下跌时调整项应为正数");
        assertEquals(0, new BigDecimal("300.0000").compareTo(vo.getRawWinRate()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(vo.getWinRate()),
                "超过 100% 的胜率应被裁剪为 100%");
    }

    @Test
    @DisplayName("关闭裁剪时返回未裁剪的原始胜率")
    void shouldKeepRawWinRateWhenClampDisabled() {
        properties.setClampWinRate(false);
        try {
            IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, buildSeries("125"));
            assertEquals(0, new BigDecimal("-150.0000").compareTo(vo.getWinRate()));
        } finally {
            properties.setClampWinRate(true);
        }
    }

    @Test
    @DisplayName("切换为同向模式时恢复「涨则加、跌则减」")
    void shouldSupportDirectDirection() {
        properties.setChangeDirection("DIRECT");
        properties.setClampWinRate(false);
        try {
            IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, buildSeries("125"));
            assertEquals(0, new BigDecimal("200.0000").compareTo(vo.getChangeAdjust()));
            assertEquals(0, new BigDecimal("250.0000").compareTo(vo.getWinRate()));
        } finally {
            properties.setChangeDirection("INVERSE");
            properties.setClampWinRate(true);
        }
    }

    @Test
    @DisplayName("窗口内指数走平（最高值 = 最低值）时应返回高低点中间胜率 50%")
    void shouldHandleFlatSeries() {
        List<IndexDaily> series = new ArrayList<IndexDaily>();
        for (int i = 0; i < 10; i++) {
            series.add(bar(LocalDate.parse("2026-01-01").plusDays(i).toString(), "1000"));
        }
        IndexWinRateVO vo = calculator.calculate(IndexDefinition.SHANGHAI, series);

        assertEquals(0, new BigDecimal("50.0000").compareTo(vo.getBaseWinRate()));
        assertTrue(vo.getWinRate().compareTo(BigDecimal.ZERO) >= 0);
    }
}
