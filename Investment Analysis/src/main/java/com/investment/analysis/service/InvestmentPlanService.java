package com.investment.analysis.service;

import com.investment.analysis.config.PlanProperties;
import com.investment.analysis.model.InvestmentPlanRequest;
import com.investment.analysis.model.InvestmentPlanVO;
import com.investment.analysis.model.PlanInstallmentVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 投资方案生成服务。
 * <p>核心规则：共进行 n 次投资（n 默认 4，可配置），
 * <b>每次投资金额 = 此前已经累计的投资金额</b>。</p>
 * <pre>
 * 设首次投入为 B，则各期金额依次为：
 *   第 1 次：B
 *   第 2 次：B                     （= 第 1 次累计）
 *   第 3 次：B + B   = 2B          （= 前 2 次累计）
 *   第 4 次：B + B + 2B = 4B       （= 前 3 次累计）
 *   ...
 *   第 k 次：2^(k-2) * B
 * 因此 n 次投资累计使用金额 = B * 2^(n-1)，
 * 由「累计使用金额 = 用户输入的可用总金额 T」反推可得首次投入 B = T / 2^(n-1)。
 * </pre>
 * <p>该模块仅用于投资前的测算参考，计算结果不写入数据库。</p>
 */
@Service
public class InvestmentPlanService {

    private final PlanProperties planProperties;

    public InvestmentPlanService(PlanProperties planProperties) {
        this.planProperties = planProperties;
    }

    /**
     * 根据可用投资总金额与投资次数生成投资方案（纯测算，不落库）。
     */
    public InvestmentPlanVO generate(InvestmentPlanRequest request) {
        BigDecimal totalAmount = request == null ? null : request.getTotalAmount();
        Integer times = request == null ? null : request.getTimes();
        return calculate(totalAmount, times);
    }

    /**
     * 纯计算：生成方案明细但不落库，便于单元测试与复用。
     *
     * @param totalAmount 可用投资总金额
     * @param times       投资次数，为空时取配置默认值
     */
    public InvestmentPlanVO calculate(BigDecimal totalAmount, Integer times) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("请输入大于 0 的可用投资总金额");
        }

        int investTimes = times == null ? planProperties.getDefaultTimes() : times;
        if (investTimes < planProperties.getMinTimes() || investTimes > planProperties.getMaxTimes()) {
            throw new IllegalArgumentException("投资次数需在 " + planProperties.getMinTimes()
                    + " ~ " + planProperties.getMaxTimes() + " 之间");
        }

        int scale = Math.max(2, planProperties.getAmountScale());
        BigDecimal total = totalAmount.setScale(scale, RoundingMode.DOWN);

        // 分母：2^(n-1)，即各期金额相对首次投入的倍数之和
        BigDecimal divisor = BigDecimal.valueOf(2).pow(investTimes - 1);
        // 首次投入金额 = 可用总金额 / 2^(n-1)，计算时保留更高精度，展示时取整到分
        BigDecimal baseExact = total.divide(divisor, scale + 6, RoundingMode.HALF_UP);
        BigDecimal baseAmount = baseExact.setScale(scale, RoundingMode.HALF_UP);

        List<PlanInstallmentVO> installments = new ArrayList<PlanInstallmentVO>(investTimes);
        BigDecimal cumulative = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal growth = BigDecimal.valueOf(2).pow(Math.max(0, investTimes - 1));

        for (int period = 1; period <= investTimes; period++) {
            BigDecimal amount;
            if (period < investTimes) {
                // 第 1 期为基础金额；第 k 期（k>=2）金额 = 2^(k-2) * 基础金额
                BigDecimal multiple = period == 1
                        ? BigDecimal.ONE
                        : BigDecimal.valueOf(2).pow(period - 2);
                amount = baseExact.multiply(multiple).setScale(scale, RoundingMode.HALF_UP);
            } else {
                // 末期用「总额 - 此前累计」计算，保证总额精确等于用户输入的可用金额
                amount = total.subtract(cumulative).setScale(scale, RoundingMode.HALF_UP);
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                amount = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
            }

            BigDecimal previousCumulative = cumulative;
            cumulative = cumulative.add(amount).setScale(scale, RoundingMode.HALF_UP);

            PlanInstallmentVO item = new PlanInstallmentVO();
            item.setPeriodNo(period);
            item.setAmount(amount);
            item.setCumulativeAmount(cumulative);
            item.setRatioOfTotal(ratio(amount, total));
            item.setRatioCumulative(ratio(cumulative, total));
            item.setRemark(buildInstallmentRemark(period, investTimes, previousCumulative, amount, total));
            installments.add(item);
        }

        InvestmentPlanVO vo = new InvestmentPlanVO();
        vo.setTotalAmount(total);
        vo.setTimes(investTimes);
        vo.setBaseAmount(baseAmount);
        vo.setUsedAmount(cumulative);
        vo.setGrowthMultiple(growth);
        vo.setInstallments(installments);
        vo.setGeneratedAt(LocalDateTime.now());
        vo.setRule(buildRule(investTimes, baseAmount, installments, cumulative, growth));

        // 末期尾差：末次金额与「按规则推算的金额」之间的差额（仅分位级）
        BigDecimal expectedLast = expectedLastAmount(baseExact, investTimes, scale);
        BigDecimal lastAmount = installments.get(installments.size() - 1).getAmount();
        vo.setLastAdjustment(lastAmount.subtract(expectedLast).setScale(scale, RoundingMode.HALF_UP));
        return vo;
    }

    private BigDecimal expectedLastAmount(BigDecimal baseExact, int times, int scale) {
        if (times <= 1) {
            return baseExact.setScale(scale, RoundingMode.HALF_UP);
        }
        return baseExact.multiply(BigDecimal.valueOf(2).pow(times - 2)).setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return part.divide(total, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private String buildInstallmentRemark(int period, int times, BigDecimal previousCumulative,
                                          BigDecimal amount, BigDecimal total) {
        if (times == 1) {
            return "仅投资 1 次，可用总金额一次性投入";
        }
        if (period == 1) {
            return "首次基础投入 = 可用总金额 ÷ 2^" + (times - 1) + " = " + plain(amount) + " 元";
        }
        if (period == times) {
            BigDecimal expected = previousCumulative;
            BigDecimal diff = amount.subtract(expected);
            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                return "= 前 " + (period - 1) + " 次累计投入 " + plain(previousCumulative)
                        + " 元，含尾差调整 " + (diff.signum() > 0 ? "+" : "") + plain(diff) + " 元";
            }
        }
        return "= 前 " + (period - 1) + " 次累计投入金额（" + plain(previousCumulative) + " 元）";
    }

    private String buildRule(int times, BigDecimal baseAmount, List<PlanInstallmentVO> installments,
                             BigDecimal usedAmount, BigDecimal growth) {
        StringBuilder builder = new StringBuilder();
        builder.append("共进行 ").append(times).append(" 次投资：第 1 次投入基础金额 ")
                .append(plain(baseAmount)).append(" 元");
        if (times > 1) {
            builder.append("；此后每一次的投资金额 = 此前各期已经累计的投资金额，故各次金额依次为 ")
                    .append(joinAmounts(installments)).append(" 元");
        }
        builder.append("。").append(times).append(" 次累计使用 ")
                .append(plain(usedAmount)).append(" 元，正好等于可用投资总金额；末期较首期放大 ")
                .append(plain(growth)).append(" 倍。");
        return builder.toString();
    }

    private String joinAmounts(List<PlanInstallmentVO> installments) {
        List<String> texts = new ArrayList<String>();
        int size = installments.size();
        for (int i = 0; i < size; i++) {
            if (size > 8 && i >= 4 && i < size - 2) {
                if (i == 4) {
                    texts.add("…");
                }
                continue;
            }
            texts.add(plain(installments.get(i).getAmount()));
        }
        StringBuilder builder = new StringBuilder();
        for (String text : texts) {
            if (builder.length() > 0) {
                builder.append(" → ");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private String plain(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    public PlanProperties getPlanProperties() {
        return planProperties;
    }
}
