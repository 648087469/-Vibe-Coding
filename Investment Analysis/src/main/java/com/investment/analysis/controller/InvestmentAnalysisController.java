package com.investment.analysis.controller;

import com.investment.analysis.common.ApiResponse;
import com.investment.analysis.entity.IndexAnalysisResult;
import com.investment.analysis.entity.IndexDaily;
import com.investment.analysis.mapper.IndexDailyMapper;
import com.investment.analysis.model.IndexDefinition;
import com.investment.analysis.model.TrendPointVO;
import com.investment.analysis.model.WinRateSummaryVO;
import com.investment.analysis.service.InvestmentAnalysisService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 投资分析模块 REST 接口。
 */
@RestController
@CrossOrigin
@RequestMapping("/api/analysis")
public class InvestmentAnalysisController {

    private final InvestmentAnalysisService analysisService;
    private final IndexDailyMapper indexDailyMapper;

    public InvestmentAnalysisController(InvestmentAnalysisService analysisService,
                                       IndexDailyMapper indexDailyMapper) {
        this.analysisService = analysisService;
        this.indexDailyMapper = indexDailyMapper;
    }

    /**
     * 投资分析模块覆盖的指数列表。
     */
    @GetMapping("/indices")
    public ApiResponse<List<Map<String, Object>>> indices() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (IndexDefinition definition : IndexDefinition.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", definition.getCode());
            item.put("name", definition.getName());
            item.put("market", definition.getMarket());
            item.put("order", definition.getSortOrder());
            list.add(item);
        }
        return ApiResponse.ok(list);
    }

    /**
     * 三大指数胜率汇总（页面表格 + 柱形图数据源）。
     *
     * @param refresh true 时强制走接口刷新后再计算
     */
    @GetMapping("/win-rate")
    public ApiResponse<WinRateSummaryVO> winRate(
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        return ApiResponse.ok(analysisService.analyze(refresh));
    }

    /**
     * 强制刷新：重新调用外部接口拉取 365 天数据并写入数据库后重算。
     */
    @PostMapping("/refresh")
    public ApiResponse<WinRateSummaryVO> refresh() {
        return ApiResponse.ok("数据已刷新", analysisService.analyze(true));
    }

    /**
     * 指数走势数据（折线图）。
     */
    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVO>> trend(
            @RequestParam("code") String code,
            @RequestParam(name = "days", defaultValue = "365") int days) {
        return ApiResponse.ok(analysisService.trend(code, days));
    }

    /**
     * 指数原始日线数据（表格展示 / 数据核对）。
     */
    @GetMapping("/records")
    public ApiResponse<List<IndexDaily>> records(
            @RequestParam("code") String code,
            @RequestParam(name = "days", defaultValue = "365") int days) {
        IndexDefinition definition = IndexDefinition.ofCode(code);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(2, days) - 1L);
        return ApiResponse.ok(indexDailyMapper.selectRange(definition.getCode(), start, end));
    }

    /**
     * 历史分析结果。
     */
    @GetMapping("/history")
    public ApiResponse<List<IndexAnalysisResult>> history(
            @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return ApiResponse.ok(analysisService.history(limit));
    }

    /**
     * 最近一次分析快照（数据库中已落库的结果）。
     */
    @GetMapping("/latest-snapshot")
    public ApiResponse<List<IndexAnalysisResult>> latestSnapshot() {
        return ApiResponse.ok(analysisService.latestSnapshot());
    }
}
