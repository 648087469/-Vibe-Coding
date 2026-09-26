-- ========================================================
-- 投资分析模块 - 数据库表结构（MySQL，可重复执行）
-- ========================================================

-- 指数日线行情表（三大指数 365 天指数数值）
CREATE TABLE IF NOT EXISTS `index_daily` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `index_code`   VARCHAR(16)   NOT NULL COMMENT '指数代码，如 000001/399001/399006',
    `index_name`   VARCHAR(64)   NOT NULL COMMENT '指数名称',
    `trade_date`   DATE          NOT NULL COMMENT '交易日',
    `open_price`   DECIMAL(14,3)          DEFAULT NULL COMMENT '开盘点位',
    `close_price`  DECIMAL(14,3) NOT NULL COMMENT '收盘点位',
    `high_price`   DECIMAL(14,3)          DEFAULT NULL COMMENT '最高点位',
    `low_price`    DECIMAL(14,3)          DEFAULT NULL COMMENT '最低点位',
    `volume`       BIGINT                 DEFAULT NULL COMMENT '成交量（手）',
    `amount`       DECIMAL(24,2)          DEFAULT NULL COMMENT '成交额（元）',
    `data_source`  VARCHAR(32)            DEFAULT NULL COMMENT '数据来源：EASTMONEY/TENCENT/SINA',
    `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_index_code_trade_date` (`index_code`, `trade_date`),
    KEY `idx_trade_date` (`trade_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '指数日线行情表';

-- 胜率计算结果表（每次分析结果落库，便于追溯历史）
CREATE TABLE IF NOT EXISTS `index_analysis_result` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `index_code`         VARCHAR(16)   NOT NULL COMMENT '指数代码',
    `index_name`         VARCHAR(64)   NOT NULL COMMENT '指数名称',
    `latest_trade_date`  DATE          NOT NULL COMMENT '最新交易日',
    `latest_close`       DECIMAL(14,3) NOT NULL COMMENT '最新收盘点位',
    `period_start`       DATE                   DEFAULT NULL COMMENT '取样窗口开始日期',
    `period_end`         DATE                   DEFAULT NULL COMMENT '取样窗口结束日期',
    `sample_count`       INT                    DEFAULT 0 COMMENT '窗口内样本数量',
    `min_close`          DECIMAL(14,3)          DEFAULT NULL COMMENT '窗口内最低点位',
    `max_close`          DECIMAL(14,3)          DEFAULT NULL COMMENT '窗口内最高点位',
    `base_win_rate`      DECIMAL(10,4)          DEFAULT NULL COMMENT '基础胜率（按点位高低位置换算）',
    `compare_trade_date` DATE                   DEFAULT NULL COMMENT '对比基准交易日（7 天前）',
    `compare_close`      DECIMAL(14,3)          DEFAULT NULL COMMENT '对比基准收盘点位',
    `change_percent`     DECIMAL(10,4)          DEFAULT NULL COMMENT '最新交易日对比 7 天前的涨跌幅（%）',
    `change_adjust`      DECIMAL(10,4)          DEFAULT NULL COMMENT '涨跌幅调整项（涨跌幅 * 10）',
    `raw_win_rate`       DECIMAL(10,4)          DEFAULT NULL COMMENT '未裁剪的最终胜率',
    `final_win_rate`     DECIMAL(10,4)          DEFAULT NULL COMMENT '最终胜率（%）',
    `data_source`        VARCHAR(32)            DEFAULT NULL COMMENT '本次计算所用数据来源',
    `analyzed_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code_trade_date` (`index_code`, `latest_trade_date`),
    KEY `idx_analyzed_at` (`analyzed_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '指数胜率分析结果表';

-- 说明：投资方案模块仅用于投资前的测算参考，计算结果不写入数据库，因此不需要建表。

-- ========================================================
-- 持仓管理模块 - 数据库表结构（MySQL，可重复执行）
-- ========================================================

-- 持仓主表：一个持仓 = 用户输入的持仓名称 + 关联指数（关联指数用于取成本点位与最新点位）
CREATE TABLE IF NOT EXISTS `holding_position` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `position_name` VARCHAR(64)  NOT NULL COMMENT '持仓名称（用户输入）',
    `index_code`    VARCHAR(16)  NOT NULL COMMENT '关联指数代码，如 000001/399001/399006',
    `index_name`    VARCHAR(64)  NOT NULL COMMENT '关联指数名称',
    `remark`        VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_name_index` (`position_name`, `index_code`),
    KEY `idx_index_code` (`index_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '持仓管理-持仓主表';

-- 买入记录表：每笔买入保存「买入日期、买入金额、成本（该交易日指数收盘点位）、买入时的预测胜率」
CREATE TABLE IF NOT EXISTS `holding_buy_record` (
    `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `position_id`           BIGINT        NOT NULL COMMENT '所属持仓 ID',
    `buy_date`              DATE          NOT NULL COMMENT '用户输入的买入日期',
    `cost_trade_date`       DATE          NOT NULL COMMENT '取用成本对应的交易日（买入日非交易日时向前取最近交易日）',
    `cost_price`            DECIMAL(14,3) NOT NULL COMMENT '买入成本：该交易日指数收盘点位',
    `buy_amount`            DECIMAL(18,2) NOT NULL COMMENT '买入金额（元）',
    `buy_win_rate`          DECIMAL(10,4)          DEFAULT NULL COMMENT '买入时的预测胜率（%）',
    `win_rate_period_start` DATE                   DEFAULT NULL COMMENT '预测胜率取样窗口开始日期',
    `win_rate_period_end`   DATE                   DEFAULT NULL COMMENT '预测胜率取样窗口结束日期',
    `win_rate_sample_count` INT                    DEFAULT 0 COMMENT '预测胜率取样窗口内交易日数量',
    `win_rate_formula`      VARCHAR(512)           DEFAULT NULL COMMENT '预测胜率算式（便于核对）',
    `win_rate_note`         VARCHAR(255)           DEFAULT NULL COMMENT '备注：成本取数、数据不足等原因说明',
    `created_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_position_id` (`position_id`),
    KEY `idx_buy_date` (`buy_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '持仓管理-买入记录表';
