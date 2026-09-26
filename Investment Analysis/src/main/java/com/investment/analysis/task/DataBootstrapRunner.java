package com.investment.analysis.task;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.service.InvestmentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动预热：库中无数据或数据过期时，自动拉取三大指数 365 天数据并落库。
 * 该过程失败不会影响应用启动，页面仍可通过 /api/analysis/refresh 手动刷新。
 */
@Component
public class DataBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataBootstrapRunner.class);

    private final InvestmentAnalysisService analysisService;
    private final AnalysisProperties properties;

    public DataBootstrapRunner(InvestmentAnalysisService analysisService, AnalysisProperties properties) {
        this.analysisService = analysisService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getFetch().isAutoRefreshOnStartup()) {
            return;
        }
        try {
            long begin = System.currentTimeMillis();
            analysisService.analyze(false);
            log.info("启动数据预热完成，耗时 {} ms", System.currentTimeMillis() - begin);
        } catch (Exception e) {
            log.warn("启动数据预热失败（不影响启动，可稍后点击页面“刷新数据”重试）：{}", e.getMessage());
        }
    }
}
