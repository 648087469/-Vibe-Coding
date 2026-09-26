package com.investment.analysis.service;

import com.investment.analysis.config.PlanProperties;
import com.investment.analysis.model.InvestmentPlanVO;
import com.investment.analysis.model.PlanInstallmentVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 投资方案生成规则单元测试。
 * <p>规则：共 n 次投资，每次投资金额 = 此前已经累计的投资金额。</p>
 */
class InvestmentPlanServiceTest {

    private final PlanProperties properties = new PlanProperties();
    private final InvestmentPlanService service = new InvestmentPlanService(properties);

    private void assertAmount(InvestmentPlanVO plan, int period, String expected) {
        PlanInstallmentVO item = plan.getInstallments().get(period - 1);
        assertEquals(0, new BigDecimal(expected).compareTo(item.getAmount()),
                "第 " + period + " 次投资金额应为 " + expected);
    }

    @Test
    @DisplayName("默认 4 次投资：10 万拆成 1.25 万 / 1.25 万 / 2.5 万 / 5 万，合计正好 10 万")
    void shouldGenerateDefaultFourTimesPlan() {
        InvestmentPlanVO plan = service.calculate(new BigDecimal("100000"), null);

        assertEquals(4, plan.getTimes());
        assertEquals(0, new BigDecimal("12500").compareTo(plan.getBaseAmount()));
        assertAmount(plan, 1, "12500");
        assertAmount(plan, 2, "12500");
        assertAmount(plan, 3, "25000");
        assertAmount(plan, 4, "50000");
        assertEquals(0, new BigDecimal("100000").compareTo(plan.getUsedAmount()),
                "4 次投资使用的总体金额应等于可用总金额");
        assertEquals(0, new BigDecimal("8").compareTo(plan.getGrowthMultiple()));
        assertEquals(0, BigDecimal.ZERO.compareTo(plan.getLastAdjustment()));
    }

    @Test
    @DisplayName("每一次投资金额都等于此前各期累计投入金额")
    void eachAmountShouldEqualPreviousCumulative() {
        InvestmentPlanVO plan = service.calculate(new BigDecimal("100000"), 5);

        List<PlanInstallmentVO> items = plan.getInstallments();
        for (int i = 1; i < items.size(); i++) {
            BigDecimal previousCumulative = items.get(i - 1).getCumulativeAmount();
            assertEquals(0, items.get(i).getAmount().compareTo(previousCumulative),
                    "第 " + items.get(i).getPeriodNo() + " 次投资金额应等于此前累计投入 "
                            + previousCumulative.toPlainString() + " 元");
        }
        assertEquals(0, new BigDecimal("16").compareTo(plan.getGrowthMultiple()));
    }

    @Test
    @DisplayName("2 次投资：每次各占一半")
    void shouldGenerateTwoTimesPlan() {
        InvestmentPlanVO plan = service.calculate(new BigDecimal("100000"), 2);

        assertEquals(2, plan.getTimes());
        assertAmount(plan, 1, "50000");
        assertAmount(plan, 2, "50000");
        assertEquals(0, new BigDecimal("100000").compareTo(plan.getUsedAmount()));
    }

    @Test
    @DisplayName("1 次投资：可用总金额一次性投入")
    void shouldGenerateSingleTimePlan() {
        InvestmentPlanVO plan = service.calculate(new BigDecimal("100000"), 1);

        assertEquals(1, plan.getTimes());
        assertAmount(plan, 1, "100000");
        assertEquals(0, new BigDecimal("100000").compareTo(plan.getUsedAmount()));
        assertEquals(0, new BigDecimal("1").compareTo(plan.getGrowthMultiple()));
    }

    @Test
    @DisplayName("金额无法整除时，末次做尾差调整，使用总额仍精确等于输入金额")
    void shouldKeepUsedAmountExactWhenNotDivisible() {
        InvestmentPlanVO plan = service.calculate(new BigDecimal("100000.01"), 4);

        assertEquals(0, new BigDecimal("100000.01").compareTo(plan.getUsedAmount()),
                "使用总额应精确等于输入的可用总金额");
        BigDecimal sum = BigDecimal.ZERO;
        for (PlanInstallmentVO item : plan.getInstallments()) {
            sum = sum.add(item.getAmount());
        }
        assertEquals(0, new BigDecimal("100000.01").compareTo(sum));
        assertTrue(plan.getLastAdjustment().abs().compareTo(new BigDecimal("0.05")) <= 0,
                "尾差应控制在分位级别，" + "实际 " + plan.getLastAdjustment().toPlainString());
    }

    @Test
    @DisplayName("参数校验：金额必须大于 0，投资次数必须在允许范围内")
    void shouldRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(BigDecimal.ZERO, 4));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(new BigDecimal("-100"), 4));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(new BigDecimal("100000"), 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(new BigDecimal("100000"), 99));
    }
}
