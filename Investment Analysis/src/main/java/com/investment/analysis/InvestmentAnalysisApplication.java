package com.investment.analysis;

import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.config.HoldingProperties;
import com.investment.analysis.config.PlanProperties;
import com.investment.analysis.config.RiskProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网页版投资分析系统 - 投资分析模块启动类。
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AnalysisProperties.class, PlanProperties.class, HoldingProperties.class,
        RiskProperties.class})
@MapperScan("com.investment.analysis.mapper")
public class InvestmentAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentAnalysisApplication.class, args);
    }
}
