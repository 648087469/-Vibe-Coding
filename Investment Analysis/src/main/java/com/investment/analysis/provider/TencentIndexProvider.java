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
 * 腾讯财经行情数据源（第二优先级）。
 * 接口：https://web.ifzq.gtimg.cn/appstock/app/fqkline/get
 */
@Component
public class TencentIndexProvider implements IndexDataProvider {

    private static final String SOURCE = "TENCENT";
    private static final String TEMPLATE =
            "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=%s,day,%s,%s,720,qfq";
    private static final DateTimeFormatter PARSE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalysisProperties properties;

    public TencentIndexProvider(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public String displayName() {
        return "腾讯财经";
    }

    @Override
    public int order() {
        return 2;
    }

    @Override
    public List<IndexDailyData> fetch(IndexDefinition definition, LocalDate start, LocalDate end) {
        String url = String.format(TEMPLATE, definition.getTencentSymbol(),
                start.format(PARSE_DATE), end.format(PARSE_DATE));
        String body = HttpSupport.get(url, "https://gu.qq.com/",
                properties.getFetch().getTimeoutMillis(),
                properties.getFetch().getRetryTimes(),
                properties.getFetch().getRetryIntervalMillis());

        JSONObject root = JSON.parseObject(body);
        if (root == null || root.getJSONObject("data") == null) {
            throw new IndexDataException("腾讯财经返回内容无法解析");
        }
        JSONObject node = root.getJSONObject("data").getJSONObject(definition.getTencentSymbol());
        if (node == null) {
            throw new IndexDataException("腾讯财经未返回 " + definition.getName() + " 的数据");
        }
        JSONArray rows = node.getJSONArray("qfqday");
        if (rows == null || rows.isEmpty()) {
            rows = node.getJSONArray("day");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IndexDataException("腾讯财经返回的 K 线数据为空");
        }

        List<IndexDailyData> result = new ArrayList<IndexDailyData>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            JSONArray row = rows.getJSONArray(i);
            if (row == null || row.size() < 5) {
                continue;
            }
            // 腾讯格式：[日期, 开盘, 收盘, 最高, 最低, 成交量]
            IndexDailyData item = new IndexDailyData();
            item.setIndexCode(definition.getCode());
            item.setIndexName(definition.getName());
            item.setTradeDate(LocalDate.parse(row.getString(0), PARSE_DATE));
            item.setOpenPrice(toDecimal(row.getString(1)));
            item.setClosePrice(toDecimal(row.getString(2)));
            item.setHighPrice(toDecimal(row.getString(3)));
            item.setLowPrice(toDecimal(row.getString(4)));
            item.setVolume(row.size() > 5 ? toLong(row.getString(5)) : null);
            item.setDataSource(SOURCE);
            if (item.getClosePrice() != null) {
                result.add(item);
            }
        }
        if (result.isEmpty()) {
            throw new IndexDataException("腾讯财经未解析出有效数据");
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
