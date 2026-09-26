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
 * 东方财富行情数据源（优先级最高）。
 * 接口：https://push2his.eastmoney.com/api/qt/stock/kline/get
 */
@Component
public class EastmoneyIndexProvider implements IndexDataProvider {

    private static final String SOURCE = "EASTMONEY";
    private static final String TEMPLATE =
            "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s"
                    + "&ut=fa5fd1943c7b386f172d6893dbfba10b"
                    + "&fields1=f1,f2,f3,f4,f5,f6"
                    + "&fields2=f51,f52,f53,f54,f55,f56,f57"
                    + "&klt=101&fqt=1&beg=%s&end=%s&lmt=1000000";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter PARSE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AnalysisProperties properties;

    public EastmoneyIndexProvider(AnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public String displayName() {
        return "东方财富";
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public List<IndexDailyData> fetch(IndexDefinition definition, LocalDate start, LocalDate end) {
        String url = String.format(TEMPLATE, definition.getEastmoneySecId(),
                start.format(DATE), end.format(DATE));
        String body = HttpSupport.get(url, "https://quote.eastmoney.com/",
                properties.getFetch().getTimeoutMillis(),
                properties.getFetch().getRetryTimes(),
                properties.getFetch().getRetryIntervalMillis());

        JSONObject root = JSON.parseObject(body);
        if (root == null) {
            throw new IndexDataException("东方财富返回内容无法解析");
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new IndexDataException("东方财富未返回 " + definition.getName() + " 的数据");
        }
        JSONArray klines = data.getJSONArray("klines");
        if (klines == null || klines.isEmpty()) {
            throw new IndexDataException("东方财富返回的 K 线数据为空");
        }

        List<IndexDailyData> result = new ArrayList<IndexDailyData>(klines.size());
        for (int i = 0; i < klines.size(); i++) {
            String line = klines.getString(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 6) {
                continue;
            }
            IndexDailyData item = new IndexDailyData();
            item.setIndexCode(definition.getCode());
            item.setIndexName(definition.getName());
            item.setTradeDate(LocalDate.parse(parts[0], PARSE_DATE));
            item.setOpenPrice(toDecimal(parts[1]));
            item.setClosePrice(toDecimal(parts[2]));
            item.setHighPrice(toDecimal(parts[3]));
            item.setLowPrice(toDecimal(parts[4]));
            item.setVolume(toLong(parts[5]));
            item.setAmount(parts.length > 6 ? toDecimal(parts[6]) : null);
            item.setDataSource(SOURCE);
            result.add(item);
        }
        if (result.isEmpty()) {
            throw new IndexDataException("东方财富未解析出有效数据");
        }
        return result;
    }

    private BigDecimal toDecimal(String text) {
        if (text == null || text.trim().isEmpty() || "-".equals(text.trim())) {
            return null;
        }
        return new BigDecimal(text.trim());
    }

    private Long toLong(String text) {
        if (text == null || text.trim().isEmpty() || "-".equals(text.trim())) {
            return null;
        }
        try {
            return new BigDecimal(text.trim()).longValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
