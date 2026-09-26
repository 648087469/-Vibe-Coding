package com.investment.analysis.service;

import com.investment.analysis.model.HoldingOverviewVO;
import com.investment.analysis.model.RiskOverviewVO;
import org.springframework.stereotype.Service;

/**
 * 风险评估服务：基于持仓管理模块的持仓与最新点位，逐个持仓给出分档止盈/止损建议。
 */
@Service
public class RiskService {

    private final HoldingService holdingService;
    private final RiskCalculator riskCalculator;

    public RiskService(HoldingService holdingService, RiskCalculator riskCalculator) {
        this.holdingService = holdingService;
        this.riskCalculator = riskCalculator;
    }

    /**
     * 风险评估总览：一个持仓 = 一个方案建议。
     *
     * @param forceRefresh 是否强制刷新最新点位后再评估
     */
    public RiskOverviewVO overview(boolean forceRefresh) {
        HoldingOverviewVO holding = holdingService.overview(forceRefresh);
        RiskOverviewVO overview = riskCalculator.buildOverview(holding.getPositions());
        overview.setQuoteSummary(holding.getQuoteSummary());
        return overview;
    }
}
