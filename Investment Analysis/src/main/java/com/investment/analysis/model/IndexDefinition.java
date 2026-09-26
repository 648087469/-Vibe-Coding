package com.investment.analysis.model;

/**
 * 投资分析模块关注的三大指数定义。
 * <p>集中维护各数据源所需的代码映射，新增指数只需在此追加枚举。</p>
 */
public enum IndexDefinition {

    /** 上证指数 */
    SHANGHAI("000001", "上证指数", "上交所", 1,
            "1.000001", "sh000001", "sh000001"),

    /** 深证成指 */
    SHENZHEN("399001", "深证成指", "深交所", 2,
            "0.399001", "sz399001", "sz399001"),

    /** 创业板指 */
    CHINEXT("399006", "创业板指", "深交所", 3,
            "0.399006", "sz399006", "sz399006");

    private final String code;
    private final String name;
    private final String market;
    private final int sortOrder;
    private final String eastmoneySecId;
    private final String tencentSymbol;
    private final String sinaSymbol;

    IndexDefinition(String code, String name, String market, int sortOrder,
                    String eastmoneySecId, String tencentSymbol, String sinaSymbol) {
        this.code = code;
        this.name = name;
        this.market = market;
        this.sortOrder = sortOrder;
        this.eastmoneySecId = eastmoneySecId;
        this.tencentSymbol = tencentSymbol;
        this.sinaSymbol = sinaSymbol;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getMarket() {
        return market;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getEastmoneySecId() {
        return eastmoneySecId;
    }

    public String getTencentSymbol() {
        return tencentSymbol;
    }

    public String getSinaSymbol() {
        return sinaSymbol;
    }

    /**
     * 按代码查找指数定义，找不到抛出 IllegalArgumentException。
     */
    public static IndexDefinition ofCode(String code) {
        for (IndexDefinition definition : values()) {
            if (definition.code.equals(code)) {
                return definition;
            }
        }
        throw new IllegalArgumentException("暂不支持的指数代码：" + code);
    }
}
