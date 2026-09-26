package com.investment.analysis.controller;

import com.investment.analysis.common.ApiResponse;
import com.investment.analysis.model.RiskLevelVO;
import com.investment.analysis.model.RiskOverviewVO;
import com.investment.analysis.service.RiskCalculator;
import com.investment.analysis.service.RiskService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险评估模块 REST 接口。
 * <p>针对持仓管理中的每个持仓给出分档止盈/止损建议：
 * 盈利超过 3% 卖出 1/4、超过 5% 再卖出 1/2、此后每涨 1 个点卖出剩余 1/4、到 10% 全部卖出；
 * 亏损方向规则对称。</p>
 */
@RestController
@CrossOrigin
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskService riskService;
    private final RiskCalculator riskCalculator;

    public RiskController(RiskService riskService, RiskCalculator riskCalculator) {
        this.riskService = riskService;
        this.riskCalculator = riskCalculator;
    }

    /**
     * 规则配置：止盈/止损阶梯预览与文案说明。
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        List<RiskLevelVO> profitLadder = riskCalculator.previewLadder(true);
        List<RiskLevelVO> lossLadder = riskCalculator.previewLadder(false);
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("takeProfitLadder", profitLadder);
        config.put("stopLossLadder", lossLadder);
        config.put("takeProfitRule", ladderRuleText(profitLadder, true));
        config.put("stopLossRule", ladderRuleText(lossLadder, false));
        config.put("sellRatioMeaning", "各档「卖出比例」均指触发时剩余仓位的比例，逐档执行后累计卖出为所占初始仓位比例");
        config.put("calcRule", "盈亏比例 =（关联指数最新点位 − 加权平均成本）÷ 加权平均成本 × 100%；"
                + "建议金额按当前市值折算，实际执行请以账户剩余仓位为准");
        return ApiResponse.ok(config);
    }

    /**
     * 风险评估总览：每个持仓一个方案建议（含完整阶梯明细）。
     */
    @GetMapping("/overview")
    public ApiResponse<RiskOverviewVO> overview() {
        return ApiResponse.ok(riskService.overview(false));
    }

    /**
     * 刷新最新点位后重新评估。
     */
    @PostMapping("/refresh")
    public ApiResponse<RiskOverviewVO> refresh() {
        return ApiResponse.ok("最新点位已刷新", riskService.overview(true));
    }

    private String ladderRuleText(List<RiskLevelVO> ladder, boolean profitSide) {
        StringBuilder builder = new StringBuilder();
        for (RiskLevelVO level : ladder) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append(level.getTriggerLabel()).append(profitSide ? "（盈利超过 " : "（亏损超过 ")
                    .append(level.getTriggerPercent().abs().stripTrailingZeros().toPlainString())
                    .append("%）：")
                    .append("全部".equals(level.getSellRatioLabel())
                            ? "全部卖出" : "卖出剩余仓位的 " + level.getSellRatioLabel());
        }
        return builder.append("。").toString();
    }
}
