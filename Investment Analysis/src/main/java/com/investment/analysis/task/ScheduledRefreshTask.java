package com.investment.analysis.task;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.service.InvestmentAnalysisService;
import com.investment.analysis.service.IndexDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：每个交易日收盘后自动补全当日行情数据（默认周一至周五 18:30）。
 */
@Component
@ConditionalOnProperty(prefix = "analysis.fetch", name = "scheduled-enabled",
        havingValue = "true", matchIfMissing = true)
public class ScheduledRefreshTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledRefreshTask.class);

    private final InvestmentAnalysisService analysisService;
    private final IndexDataService indexDataService;
    private final AnalysisProperties properties;

    public ScheduledRefreshTask(InvestmentAnalysisService analysisService,
                                IndexDataService indexDataService,
                                AnalysisProperties properties) {
        this.analysisService = analysisService;
        this.indexDataService = indexDataService;
        this.properties = properties;
    }

    @Scheduled(cron = "${analysis.fetch.schedule-cron:0 30 18 * * MON-FRI}")
    public void refreshDailyData() {
        try {
            log.info("定时任务开始：刷新三大指数行情数据");
            analysisService.analyze(true);
            log.info("定时任务完成：三大指数行情数据已更新，窗口 {} 天",
                    properties.getWindowDays());
        } catch (Exception e) {
            log.warn("定时任务刷新失败：{}", e.getMessage());
        }
    }
}
