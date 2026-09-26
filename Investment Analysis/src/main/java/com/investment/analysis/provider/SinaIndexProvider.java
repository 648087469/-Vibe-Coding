package com.investment.analysis.provider;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.investment.analysis.common.IndexDataException;
import com.investment.analysis.config.AnalysisProperties;
import com.investment.analysis.model.IndexDailyData;
import com.investment.analysis.model.IndexDefinition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 新浪财经行情数据源（兜底）。
 * 接口：https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData
 */
@Component
public class SinaIndexProvider implements IndexDataProvider {

    private static final String SOURCE = "SINA";
    private static final String TEMPLATE =
            "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData"
                    + "?symbol=%s&scale=240&ma=no&datalen=%d";
    private static final DateTimeFormatter PARSE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalysisProperties properties;

    public SinaIndexProvider(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public String displayName() {
        return "新浪财经";
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public List<IndexDailyData> fetch(IndexDefinition definition, LocalDate start, LocalDate end) {
        int window = Math.max(properties.getWindowDays(), 260) + 30;
        String url = String.format(TEMPLATE, definition.getSinaSymbol(), window);
        String body = HttpSupport.get(url, "https://finance.sina.com.cn/",
                properties.getFetch().getTimeoutMillis(),
                properties.getFetch().getRetryTimes(),
                properties.getFetch().getRetryIntervalMillis());
        if (body == null || body.trim().isEmpty() || "null".equals(body.trim())) {
            throw new IndexDataException("新浪财经返回内容为空");
        }

        JSONArray rows;
        try {
            rows = JSON.parseArray(body);
        } catch (Exception e) {
            throw new IndexDataException("新浪财经返回内容无法解析", e);
        }
        if (rows == null || rows.isEmpty()) {
            throw new IndexDataException("新浪财经未返回 " + definition.getName() + " 的数据");
        }

        List<IndexDailyData> result = new ArrayList<IndexDailyData>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row == null || row.getString("day") == null) {
                continue;
            }
            LocalDate tradeDate = LocalDate.parse(row.getString("day").trim(), PARSE_DATE);
            if (tradeDate.isBefore(start) || tradeDate.isAfter(end)) {
                continue;
            }
            IndexDailyData item = new IndexDailyData();
            item.setIndexCode(definition.getCode());
            item.setIndexName(definition.getName());
            item.setTradeDate(tradeDate);
            item.setOpenPrice(toDecimal(row.getString("open")));
            item.setClosePrice(toDecimal(row.getString("close")));
            item.setHighPrice(toDecimal(row.getString("high")));
            item.setLowPrice(toDecimal(row.getString("low")));
            item.setVolume(toLong(row.getString("volume")));
            item.setDataSource(SOURCE);
            if (item.getClosePrice() != null) {
                result.add(item);
            }
        }
        if (result.isEmpty()) {
            throw new IndexDataException("新浪财经未解析出有效数据");
        }
        return result;
    }

    private BigDecimal toDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    private Long toLong(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim()).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
