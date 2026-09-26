package com.investment.analysis.controller;

import com.investment.analysis.common.ApiResponse;
import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.config.HoldingProperties;
import com.investment.analysis.model.HoldingBuyRequest;
import com.investment.analysis.model.HoldingOverviewVO;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.service.HoldingService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 持仓管理模块 REST 接口。
 * <p>录入「持仓名称 + 具体日期 + 买入金额」，系统落库买入成本与买入时的预测胜率，
 * 并按最新点位判断盈亏、统计真实胜率与高预测胜率买入的真实胜率。</p>
 */
@RestController
@CrossOrigin
@RequestMapping("/api/holding")
public class HoldingController {

    private final HoldingService holdingService;
    private final HoldingProperties holdingProperties;
    private final AnalysisProperties analysisProperties;

    public HoldingController(HoldingService holdingService,
                             HoldingProperties holdingProperties,
                             AnalysisProperties analysisProperties) {
        this.holdingService = holdingService;
        this.holdingProperties = holdingProperties;
        this.analysisProperties = analysisProperties;
    }

    /**
     * 页面初始化参数：可关联的指数、预测胜率阈值、取样窗口、日期范围与规则说明。
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> config() {
        List<Map<String, Object>> indices = new ArrayList<Map<String, Object>>();
        for (IndexDefinition definition : IndexDefinition.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", definition.getCode());
            item.put("name", definition.getName());
            item.put("market", definition.getMarket());
            indices.add(item);
        }
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("indices", indices);
        config.put("highWinRateThreshold", holdingProperties.getHighWinRateThreshold());
        config.put("windowDays", analysisProperties.getWindowDays());
        config.put("minWinRateSamples", holdingProperties.getMinWinRateSamples());
        config.put("maxBuyAmount", holdingProperties.getMaxBuyAmount());
        config.put("maxPositionNameLength", holdingProperties.getMaxPositionNameLength());
        config.put("today", LocalDate.now().toString());
        config.put("winRateRule", "买入时预测胜率 = 以买入日期（含）为终点、最近 "
                + analysisProperties.getWindowDays() + " 天行情按投资分析模块规则算出的最终胜率；"
                + "持仓预测胜率 = 每笔买入的预测胜率之和 ÷ 买入次数");
        config.put("profitRule", "盈亏 = 按关联指数最新点位对比买入成本（买入日指数收盘点位）计算，"
                + "最新点位高于成本即计入真实胜率的「胜」");
        return ApiResponse.ok(config);
    }

    /**
     * 持仓总览：持仓列表 + 每笔买入明细 + 汇总统计 + 最新行情。
     */
    @GetMapping("/overview")
    public ApiResponse<HoldingOverviewVO> overview() {
        return ApiResponse.ok(holdingService.overview(false));
    }

    /**
     * 新增买入（同名同指数的持仓自动合并）。
     */
    @PostMapping("/buy")
    public ApiResponse<HoldingOverviewVO> addBuy(@RequestBody HoldingBuyRequest request) {
        return ApiResponse.ok("买入已记录", holdingService.addBuy(request));
    }

    /**
     * 强制刷新三大指数最新点位后返回总览。
     */
    @PostMapping("/refresh")
    public ApiResponse<HoldingOverviewVO> refresh() {
        return ApiResponse.ok("最新点位已刷新", holdingService.refreshQuotes());
    }

    /**
     * 删除某个持仓及其全部买入记录。
     */
    @DeleteMapping("/position/{id}")
    public ApiResponse<HoldingOverviewVO> deletePosition(@PathVariable("id") Long id) {
        return ApiResponse.ok("持仓已删除", holdingService.deletePosition(id));
    }

    /**
     * 删除单笔买入记录。
     */
    @DeleteMapping("/record/{id}")
    public ApiResponse<HoldingOverviewVO> deleteRecord(@PathVariable("id") Long id) {
        return ApiResponse.ok("买入记录已删除", holdingService.deleteRecord(id));
    }
}
