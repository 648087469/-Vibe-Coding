package com.investment.analysis.controller;

import com.investment.analysis.common.ApiResponse;
import com.investment.analysis.config.PlanProperties;
import com.investment.analysis.model.InvestmentPlanRequest;
import com.investment.analysis.model.InvestmentPlanVO;
import com.investment.analysis.service.InvestmentPlanService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 投资方案模块 REST 接口。
 * <p>该模块仅用于投资前的测算参考，结果不写入数据库，因此不提供历史记录接口。</p>
 */
@RestController
@CrossOrigin
@RequestMapping("/api/plan")
public class InvestmentPlanController {

    private final InvestmentPlanService planService;
    private final PlanProperties planProperties;

    public InvestmentPlanController(InvestmentPlanService planService, PlanProperties planProperties) {
        this.planService = planService;
        this.planProperties = planProperties;
    }

    /**
     * 页面初始化参数（默认投资次数、允许范围、默认金额）。
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("defaultTimes", planProperties.getDefaultTimes());
        config.put("minTimes", planProperties.getMinTimes());
        config.put("maxTimes", planProperties.getMaxTimes());
        config.put("defaultTotalAmount", planProperties.getDefaultTotalAmount());
        config.put("rule", "共进行 n 次投资，每次投资金额 = 此前已经累计的投资金额");
        config.put("storage", "NONE");
        return ApiResponse.ok(config);
    }

    /**
     * 生成投资方案：入参为可用投资总金额与投资次数。
     */
    @PostMapping("/generate")
    public ApiResponse<InvestmentPlanVO> generate(@RequestBody InvestmentPlanRequest request) {
        return ApiResponse.ok("方案已生成", planService.generate(request));
    }
}
