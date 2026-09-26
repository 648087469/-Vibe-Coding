(function () {
    'use strict';

    var API = {
        winRate: '/api/analysis/win-rate',
        refresh: '/api/analysis/refresh',
        trend: '/api/analysis/trend',
        planConfig: '/api/plan/config',
        planGenerate: '/api/plan/generate',
        holdingConfig: '/api/holding/config',
        holdingOverview: '/api/holding/overview',
        holdingBuy: '/api/holding/buy',
        holdingRefresh: '/api/holding/refresh',
        holdingPosition: '/api/holding/position/',
        holdingRecord: '/api/holding/record/',
        riskConfig: '/api/risk/config',
        riskOverview: '/api/risk/overview',
        riskRefresh: '/api/risk/refresh'
    };

    var MODULE_TITLES = {
        analysis: '投资分析',
        plan: '投资方案',
        holding: '持仓管理',
        risk: '风险评估'
    };

    var TIER = {
        good: { color: '#12a150', light: '#8fd6b0', tag: 'good' },
        mid: { color: '#d98512', light: '#f3d19a', tag: 'mid' },
        bad: { color: '#dc2f2f', light: '#f0a9a9', tag: 'bad' }
    };

    var ACCENTS = ['#2f6bff', '#7c5cff', '#12a150'];

    var state = {
        summary: null,
        activeTrendCode: null,
        trendSeries: []
    };

    var els = {
        cards: document.getElementById('cards'),
        tableBody: document.getElementById('winRateBody'),
        barChart: document.getElementById('barChart'),
        trendChart: document.getElementById('trendChart'),
        trendTabs: document.getElementById('trendTabs'),
        tooltip: document.getElementById('trendTooltip'),
        sourceValue: document.getElementById('sourceValue'),
        generatedAt: document.getElementById('generatedAt'),
        formulaBox: document.getElementById('formulaBox'),
        chartDesc: document.getElementById('chartDesc'),
        tableDesc: document.getElementById('tableDesc'),
        calcGrid: document.getElementById('calcGrid'),
        refreshBtn: document.getElementById('refreshBtn'),
        loadingBox: document.getElementById('loadingBox'),
        alertBox: document.getElementById('alertBox'),
        footWindow: document.getElementById('footWindow'),
        footCompare: document.getElementById('footCompare'),
        footTimes: document.getElementById('footTimes'),

        planAlert: document.getElementById('planAlert'),
        planCards: document.getElementById('planCards'),
        planChart: document.getElementById('planChart'),
        planChartDesc: document.getElementById('planChartDesc'),
        planTableBody: document.getElementById('planTableBody'),
        planTableDesc: document.getElementById('planTableDesc'),
        planRuleBox: document.getElementById('planRuleBox'),
        planTotalAmount: document.getElementById('planTotalAmount'),
        planTimesInput: document.getElementById('planTimesInput'),
        planQuickAmounts: document.getElementById('planQuickAmounts'),
        planGenerateBtn: document.getElementById('planGenerateBtn'),
        planTimesChip: document.getElementById('planTimesChip'),
        planUsedChip: document.getElementById('planUsedChip'),

        holdingAlert: document.getElementById('holdingAlert'),
        holdingLoading: document.getElementById('holdingLoading'),
        holdingCards: document.getElementById('holdingCards'),
        holdingChart: document.getElementById('holdingChart'),
        holdingChartDesc: document.getElementById('holdingChartDesc'),
        holdingTableBody: document.getElementById('holdingTableBody'),
        holdingTableDesc: document.getElementById('holdingTableDesc'),
        holdingRecordBody: document.getElementById('holdingRecordBody'),
        holdingRecordDesc: document.getElementById('holdingRecordDesc'),
        holdingName: document.getElementById('holdingName'),
        holdingIndex: document.getElementById('holdingIndex'),
        holdingDate: document.getElementById('holdingDate'),
        holdingAmount: document.getElementById('holdingAmount'),
        holdingQuick: document.getElementById('holdingQuick'),
        holdingSubmitBtn: document.getElementById('holdingSubmitBtn'),
        holdingRefreshBtn: document.getElementById('holdingRefreshBtn'),
        holdingBuyChip: document.getElementById('holdingBuyChip'),
        holdingQuoteChip: document.getElementById('holdingQuoteChip'),
        holdingRuleBox: document.getElementById('holdingRuleBox'),
        holdingDateHint: document.getElementById('holdingDateHint'),
        holdingAmountHint: document.getElementById('holdingAmountHint'),
        holdingFormHint: document.getElementById('holdingFormHint'),

        riskAlert: document.getElementById('riskAlert'),
        riskLoading: document.getElementById('riskLoading'),
        riskCards: document.getElementById('riskCards'),
        riskList: document.getElementById('riskList'),
        riskListDesc: document.getElementById('riskListDesc'),
        riskTableBody: document.getElementById('riskTableBody'),
        riskTableDesc: document.getElementById('riskTableDesc'),
        riskRuleBox: document.getElementById('riskRuleBox'),
        riskProfitLadder: document.getElementById('riskProfitLadder'),
        riskLossLadder: document.getElementById('riskLossLadder'),
        riskPlanChip: document.getElementById('riskPlanChip'),
        riskSellChip: document.getElementById('riskSellChip'),
        riskRefreshBtn: document.getElementById('riskRefreshBtn')
    };

    var planState = {
        initialized: false,
        config: { defaultTimes: 4, minTimes: 1, maxTimes: 20, defaultTotalAmount: '100000' },
        plan: null
    };

    var holdingState = {
        initialized: false,
        autoPaneApplied: false,
        config: { indices: [], highWinRateThreshold: 70, windowDays: 365, minWinRateSamples: 20,
            maxBuyAmount: 100000000, today: '' },
        overview: null
    };

    var riskState = {
        initialized: false,
        config: null,
        overview: null
    };

    function tierOf(winRate) {
        if (winRate >= 70) {
            return TIER.good;
        }
        if (winRate >= 45) {
            return TIER.mid;
        }
        return TIER.bad;
    }

    function fmt(value, digits) {
        if (value === null || value === undefined || value === '') {
            return '--';
        }
        var num = Number(value);
        if (isNaN(num)) {
            return '--';
        }
        return num.toFixed(digits === undefined ? 2 : digits);
    }

    function fmtSigned(value, digits) {
        if (value === null || value === undefined) {
            return '--';
        }
        var num = Number(value);
        var text = fmt(Math.abs(num), digits === undefined ? 2 : digits);
        if (num > 0) {
            return '+' + text;
        }
        if (num < 0) {
            return '-' + text;
        }
        return text;
    }

    function fmtDateTime(value) {
        if (!value) {
            return '--';
        }
        return String(value).replace('T', ' ').substring(0, 19);
    }

    /** 金额千分位格式化 */
    function fmtMoney(value, digits) {
        if (value === null || value === undefined || value === '') {
            return '--';
        }
        var num = Number(value);
        if (isNaN(num)) {
            return '--';
        }
        var fixed = num.toFixed(digits === undefined ? 2 : digits);
        var parts = fixed.split('.');
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return parts.join('.');
    }

    function esc(text) {
        return String(text === null || text === undefined ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function request(url, options) {
        return fetch(url, options || {method: 'GET'})
            .then(function (response) {
                return response.json();
            })
            .then(function (body) {
                if (!body || body.success !== true) {
                    throw new Error((body && body.message) || '接口返回异常');
                }
                return body.data;
            });
    }

    function showLoading(text) {
        els.loadingBox.hidden = false;
        els.loadingBox.lastElementChild.textContent = text || '正在加载三大指数数据…';
    }

    function hideLoading() {
        els.loadingBox.hidden = true;
    }

    function showError(message) {
        els.alertBox.hidden = false;
        els.alertBox.textContent = '加载失败：' + message;
    }

    function clearError() {
        els.alertBox.hidden = true;
        els.alertBox.textContent = '';
    }

    /* ---------------- 指标卡片 ---------------- */
    function renderCards(items) {
        var html = items.map(function (item, index) {
            var tier = tierOf(Number(item.winRate));
            var change = Number(item.changePercent);
            var changeClass = change > 0 ? 'up' : (change < 0 ? 'down' : '');
            var accent = ACCENTS[index % ACCENTS.length];
            return '' +
                '<div class="card" style="--accent:' + accent + '">' +
                '  <div class="card-head">' +
                '    <span class="card-name">' + esc(item.name) + '</span>' +
                '    <span class="card-code">' + esc(item.code) + '</span>' +
                '  </div>' +
                '  <div class="card-value">' +
                '    <span class="num" style="color:' + tier.color + '">' + fmt(item.winRate) + '</span>' +
                '    <span class="unit">%</span>' +
                '    <span class="level">' + esc(item.valueLevel) + '</span>' +
                '  </div>' +
                '  <div class="card-meta">' +
                '    <span class="k">最新点位</span><span class="v">' + fmt(item.latestClose) + '</span>' +
                '    <span class="k">最新交易日</span><span class="v">' + esc(item.latestTradeDate) + '</span>' +
                '    <span class="k">基础胜率</span><span class="v">' + fmt(item.baseWinRate) + '%</span>' +
                '    <span class="k">7 日涨跌幅</span><span class="v ' + changeClass + '">' +
                fmtSigned(item.changePercent) + '%</span>' +
                '    <span class="k">胜率调整</span><span class="v" style="color:' +
                (Number(item.changeAdjust) >= 0 ? '#12a150' : '#dc2f2f') + '">' +
                fmtSigned(item.changeAdjust) + '</span>' +
                '    <span class="k">365 天最低</span><span class="v">' + fmt(item.minClose) + '</span>' +
                '    <span class="k">365 天最高</span><span class="v">' + fmt(item.maxClose) + '</span>' +
                '    <span class="k">数据来源</span><span class="v">' + esc(item.dataSourceLabel) + '</span>' +
                '    <span class="k">样本数量</span><span class="v">' + item.sampleCount + ' 个交易日</span>' +
                '  </div>' +
                '</div>';
        }).join('');
        els.cards.innerHTML = html;
    }

    /* ---------------- 明细表格 ---------------- */
    function renderTable(items) {
        els.tableBody.innerHTML = items.map(function (item, index) {
            var winRate = Number(item.winRate);
            var tier = tierOf(winRate);
            var change = Number(item.changePercent);
            var changeClass = change > 0 ? 'up' : (change < 0 ? 'down' : '');
            var accent = ACCENTS[index % ACCENTS.length];
            return '' +
                '<tr>' +
                '  <td><div class="index-cell" style="--accent:' + accent + '">' +
                '      <i class="index-badge"></i><div><div class="name">' + esc(item.name) + '</div>' +
                '      <div class="code">' + esc(item.code) + ' · ' + esc(item.latestTradeDate) +
                '</div></div>' +
                '  </div></td>' +
                '  <td class="num">' + fmt(item.latestClose) + '</td>' +
                '  <td class="num">' + fmt(item.minClose) +
                '<div class="code">' + esc(item.minCloseDate) + '</div></td>' +
                '  <td class="num">' + fmt(item.maxClose) +
                '<div class="code">' + esc(item.maxCloseDate) + '</div></td>' +
                '  <td class="num">' + fmt(item.baseWinRate) + '%</td>' +
                '  <td class="num ' + changeClass + '">' + fmtSigned(item.changePercent) + '%' +
                '<div class="code">对比 ' + esc((item.compareTradeDate || '--').slice(5)) + '</div></td>' +
                '  <td class="num" style="color:' + (Number(item.changeAdjust) >= 0 ? '#12a150' : '#dc2f2f') + '">' +
                fmtSigned(item.changeAdjust) +
                '<div class="code">' + fmtSigned(item.changePercent) + '% × 10 反向</div></td>' +
                '  <td><div class="win-cell">' +
                '      <span class="win-num" style="color:' + tier.color + '">' + fmt(winRate) + '%</span>' +
                '      <span class="bar-track"><span class="bar-fill" style="width:' +
                Math.max(0, Math.min(100, winRate)) + '%;background:' + tier.color + '"></span></span>' +
                '      <span class="code">' + fmt(item.baseWinRate) + ' + ' +
                fmtSigned(item.changeAdjust) + '</span>' +
                '  </div></td>' +
                '  <td><span class="tag ' + tier.tag + '">' + esc(item.valueLevel) + '</span></td>' +
                '  <td><span class="tag plain">' + esc(item.dataSourceLabel) +
                (item.fromDatabase ? ' · 库' : ' · 接口') + '</span></td>' +
                '</tr>';
        }).join('');
    }

    /* ---------------- 柱形图 ---------------- */
    function renderBarChart(items) {
        var W = 980;
        var H = 400;
        var padL = 66;
        var padR = 30;
        var padT = 34;
        var padB = 104;
        var plotW = W - padL - padR;
        var plotH = H - padT - padB;

        function yPos(value) {
            return padT + plotH * (1 - Math.max(0, Math.min(100, value)) / 100);
        }

        var parts = [];
        parts.push('<defs>');
        parts.push('<linearGradient id="plotBg" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#fcfdff"/><stop offset="100%" stop-color="#f6f9fd"/></linearGradient>');
        parts.push('</defs>');
        parts.push('<rect x="' + padL + '" y="' + padT + '" width="' + plotW + '" height="' + plotH +
            '" fill="url(#plotBg)" rx="10"/>');

        // Y 轴网格
        for (var v = 0; v <= 100; v += 10) {
            var y = yPos(v);
            parts.push('<line x1="' + padL + '" y1="' + y + '" x2="' + (W - padR) + '" y2="' + y +
                '" stroke="' + (v === 0 ? '#d8dfea' : '#eef1f7') + '" stroke-width="1"/>');
            parts.push('<text x="' + (padL - 12) + '" y="' + (y + 4) + '" text-anchor="end" ' +
                'font-size="12" fill="#8695ab">' + v + '%</text>');
        }

        // 90% / 10% 参考线（规则对应的两个端点胜率）
        var guideTop = [
            {value: 90, color: '#12a150', text: '最低点位 → 胜率 90%'},
            {value: 10, color: '#dc2f2f', text: '最高点位 → 胜率 10%'}
        ];
        guideTop.forEach(function (guide) {
            var y = yPos(guide.value);
            parts.push('<line x1="' + padL + '" y1="' + y + '" x2="' + (W - padR) + '" y2="' + y +
                '" stroke="' + guide.color + '" stroke-width="1.4" stroke-dasharray="7 5" opacity="0.65"/>');
            parts.push('<text x="' + (W - padR) + '" y="' + (y - 7) + '" text-anchor="end" font-size="11.5" ' +
                'fill="' + guide.color + '">' + esc(guide.text) + '</text>');
        });

        // 柱形
        var slot = plotW / items.length;
        var barW = Math.min(104, slot * 0.44);
        var baseline = yPos(0);

        items.forEach(function (item, index) {
            var winRate = Number(item.winRate);
            var base = Number(item.baseWinRate);
            var tier = tierOf(winRate);
            var centerX = padL + slot * index + slot / 2;
            var barX = centerX - barW / 2;
            var barY = yPos(winRate);
            var barH = baseline - barY;

            parts.push('<rect class="bar" x="' + barX + '" y="' + barY + '" width="' + barW +
                '" height="' + Math.max(1, barH) + '" rx="9" fill="' + tier.color +
                '" style="animation-delay:' + (index * 0.12) + 's"/>');

            // 基础胜率刻度线
            var baseY = yPos(base);
            parts.push('<line x1="' + (barX - 12) + '" y1="' + baseY + '" x2="' + (barX + barW + 12) +
                '" y2="' + baseY + '" stroke="#0f172a" stroke-width="1.6" opacity="0.55"/>');
            parts.push('<text x="' + (barX + barW + 16) + '" y="' + (baseY + 4) + '" font-size="11" ' +
                'fill="#475569">基础 ' + fmt(base) + '%</text>');

            // 数值标签
            parts.push('<text x="' + centerX + '" y="' + (barY - 14) + '" text-anchor="middle" ' +
                'font-size="22" font-weight="700" fill="' + tier.color + '">' + fmt(winRate) + '%</text>');

            // 调整项：涨跌幅「百分比」× 10，并反向作用于胜率（涨则减、跌则加）
            var adjust = Number(item.changeAdjust);
            var pct = Number(item.changePercent);
            var trendText = pct > 0 ? '涨' : (pct < 0 ? '跌' : '平');
            var adjustText = '涨跌幅 ' + fmtSigned(pct) + '%（' + trendText + '）→ 调整 ' + fmtSigned(adjust);
            parts.push('<text x="' + centerX + '" y="' + (barY - 36) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="' + (adjust >= 0 ? '#12a150' : '#dc2f2f') + '">' +
                esc(adjustText) + '<title>' + esc(item.changeFormula || '') + '</title></text>');

            // X 轴标签
            parts.push('<text x="' + centerX + '" y="' + (baseline + 26) + '" text-anchor="middle" ' +
                'font-size="14" font-weight="700" fill="#0f172a">' + esc(item.name) + '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 44) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#8695ab">' + esc(item.code) + ' · ' + esc(item.latestTradeDate) + '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 63) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#475569">最新 ' + fmt(item.latestClose) + '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 80) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#475569">区间 ' + fmt(item.minClose) + ' ~ ' + fmt(item.maxClose) + '</text>');
        });

        els.barChart.innerHTML = parts.join('');
    }

    /* ---------------- 计算过程 ---------------- */
    function renderCalcProcess(items) {
        els.calcGrid.innerHTML = items.map(function (item, index) {
            var accent = ACCENTS[index % ACCENTS.length];
            return '' +
                '<div class="calc-card" style="--accent:' + accent + '">' +
                '  <div class="calc-title">' + esc(item.name) + ' <span>' + esc(item.code) + '</span></div>' +
                '  <ol class="calc-steps">' +
                '    <li><span class="step-name">1. 基础胜率</span>' +
                '        <code>' + esc(item.baseFormula || '--') + '</code></li>' +
                '    <li><span class="step-name">2. 涨跌幅调整</span>' +
                '        <code>' + esc(item.changeFormula || '--') + '</code></li>' +
                '    <li><span class="step-name">3. 最终胜率</span>' +
                '        <code>' + esc(item.finalFormula || '--') + '</code></li>' +
                '  </ol>' +
                '</div>';
        }).join('');
    }

    /* ---------------- 走势折线图 ---------------- */
    function renderTrendTabs(items) {
        els.trendTabs.innerHTML = items.map(function (item) {
            var active = item.code === state.activeTrendCode ? ' active' : '';
            return '<button type="button" class="tab' + active + '" data-code="' + esc(item.code) + '">' +
                esc(item.name) + '</button>';
        }).join('');
        Array.prototype.forEach.call(els.trendTabs.querySelectorAll('.tab'), function (button) {
            button.addEventListener('click', function () {
                loadTrend(button.getAttribute('data-code'));
            });
        });
    }

    function renderTrendChart(points, meta) {
        var W = 980;
        var H = 380;
        var padL = 74;
        var padR = 32;
        var padT = 30;
        var padB = 54;
        var plotW = W - padL - padR;
        var plotH = H - padT - padB;

        if (!points || points.length === 0) {
            els.trendChart.innerHTML = '<text x="' + (W / 2) + '" y="' + (H / 2) +
                '" text-anchor="middle" fill="#8695ab" font-size="14">暂无走势数据</text>';
            return;
        }

        var closes = points.map(function (p) {
            return Number(p.close);
        });
        var max = Math.max.apply(null, closes);
        var min = Math.min.apply(null, closes);
        var span = max - min || 1;
        var padding = span * 0.08;
        var top = max + padding;
        var bottom = min - padding;

        function xPos(index) {
            return padL + (points.length === 1 ? plotW / 2 : plotW * index / (points.length - 1));
        }

        function yPos(value) {
            return padT + plotH * (1 - (value - bottom) / (top - bottom));
        }

        var parts = [];
        parts.push('<defs><linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#2f6bff" stop-opacity="0.28"/>' +
            '<stop offset="100%" stop-color="#2f6bff" stop-opacity="0.02"/></linearGradient></defs>');

        // 网格与 Y 轴
        for (var g = 0; g <= 4; g++) {
            var value = bottom + (top - bottom) * g / 4;
            var y = yPos(value);
            parts.push('<line x1="' + padL + '" y1="' + y + '" x2="' + (W - padR) + '" y2="' + y +
                '" stroke="#eef1f7" stroke-width="1"/>');
            parts.push('<text x="' + (padL - 12) + '" y="' + (y + 4) + '" text-anchor="end" font-size="11.5" ' +
                'fill="#8695ab">' + fmt(value, 0) + '</text>');
        }

        var linePoints = [];
        var areaPath = ['M' + xPos(0) + ',' + yPos(closes[0])];
        for (var i = 0; i < closes.length; i++) {
            linePoints.push(xPos(i) + ',' + yPos(closes[i]));
            areaPath.push('L' + xPos(i) + ',' + yPos(closes[i]));
        }
        areaPath.push('L' + xPos(closes.length - 1) + ',' + (padT + plotH));
        areaPath.push('L' + xPos(0) + ',' + (padT + plotH) + 'Z');

        parts.push('<path d="' + areaPath.join(' ') + '" fill="url(#areaFill)"/>');
        parts.push('<polyline class="trend-line" points="' + linePoints.join(' ') +
            '" fill="none" stroke="#2f6bff" stroke-width="2.2" stroke-linejoin="round" ' +
            'stroke-linecap="round"/>');

        // 最高点 / 最低点标注
        var maxIndex = closes.indexOf(max);
        var minIndex = closes.indexOf(min);
        [
            {index: maxIndex, value: max, color: '#dc2f2f', label: '最高 ' + fmt(max), dy: -14},
            {index: minIndex, value: min, color: '#12a150', label: '最低 ' + fmt(min), dy: 22}
        ].forEach(function (marker) {
            var cx = xPos(marker.index);
            var cy = yPos(marker.value);
            parts.push('<circle cx="' + cx + '" cy="' + cy + '" r="5" fill="#fff" stroke="' +
                marker.color + '" stroke-width="2.6"/>');
            parts.push('<text x="' + cx + '" y="' + (cy + marker.dy) + '" text-anchor="middle" ' +
                'font-size="11.5" font-weight="600" fill="' + marker.color + '">' +
                esc(marker.label) + '</text>');
        });

        // X 轴日期标签
        var ticks = 5;
        for (var t = 0; t < ticks; t++) {
            var idx = Math.round((points.length - 1) * t / (ticks - 1));
            parts.push('<text x="' + xPos(idx) + '" y="' + (padT + plotH + 24) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#8695ab">' + esc(points[idx].tradeDate) + '</text>');
        }

        // 悬浮提示热区
        parts.push('<rect id="trendHit" x="' + padL + '" y="' + padT + '" width="' + plotW +
            '" height="' + plotH + '" fill="transparent" class="bar-hit"/>');
        els.trendChart.innerHTML = parts.join('');
        animateTrendLine();
        bindTrendHover(points, xPos, yPos, meta);
    }

    /**
     * 依据折线实际长度做“从左到右绘制”的动画，
     * 避免使用固定 dasharray 导致长曲线出现断裂。
     */
    function animateTrendLine() {
        var line = els.trendChart.querySelector('.trend-line');
        if (!line || typeof line.getTotalLength !== 'function') {
            return;
        }
        var length = Math.ceil(line.getTotalLength()) + 4;
        line.style.strokeDasharray = length;
        line.style.strokeDashoffset = length;
        window.requestAnimationFrame(function () {
            line.style.strokeDashoffset = 0;
        });
    }

    function bindTrendHover(points, xPos, yPos, meta) {
        var hit = document.getElementById('trendHit');
        if (!hit) {
            return;
        }
        hit.addEventListener('mousemove', function (event) {
            var rect = els.trendChart.getBoundingClientRect();
            var localX = (event.clientX - rect.left) / (rect.width / 980);
            var ratio = (localX - xPos(0)) / ((xPos(points.length - 1) - xPos(0)) || 1);
            var index = Math.max(0, Math.min(points.length - 1, Math.round(ratio * (points.length - 1))));
            var point = points[index];
            els.tooltip.hidden = false;
            els.tooltip.innerHTML = '<b>' + esc(point.tradeDate) + '</b><br>' +
                esc(meta ? meta.name : '') + ' 收盘：' + fmt(point.close);
            els.tooltip.style.left = (xPos(index) * (rect.width / 980)) + 'px';
            els.tooltip.style.top = (yPos(Number(point.close)) * (rect.height / 380)) + 'px';
        });
        hit.addEventListener('mouseleave', function () {
            els.tooltip.hidden = true;
        });
    }

    function loadTrend(code) {
        if (!code) {
            return;
        }
        state.activeTrendCode = code;
        renderTrendTabs(state.summary ? state.summary.items : []);
        var meta = (state.summary ? state.summary.items : []).filter(function (item) {
            return item.code === code;
        })[0];
        els.trendChart.innerHTML = '<text x="490" y="190" text-anchor="middle" fill="#8695ab" ' +
            'font-size="14">加载走势数据…</text>';
        request(API.trend + '?code=' + encodeURIComponent(code) + '&days=365')
            .then(function (points) {
                state.trendSeries = points;
                renderTrendChart(points, meta);
            })
            .catch(function (error) {
                els.trendChart.innerHTML = '<text x="490" y="190" text-anchor="middle" fill="#dc2f2f" ' +
                    'font-size="14">走势数据加载失败：' + esc(error.message) + '</text>';
            });
    }

    /* ---------------- 主流程 ---------------- */
    function render(summary) {
        state.summary = summary;
        var items = summary.items || [];
        els.sourceValue.textContent = summary.dataSourceSummary +
            (summary.allFromDatabase ? '（优先命中）' : '');
        els.generatedAt.textContent = fmtDateTime(summary.generatedAt);
        els.formulaBox.textContent = summary.formula;
        els.footWindow.textContent = summary.windowDays + ' 天';
        els.footCompare.textContent = summary.changeWindowDays + ' 天前';
        els.chartDesc.textContent = '柱高为最终胜率，横线为该指数的基础胜率；柱顶上方为「7日涨跌幅(%) × 10」并按反向规则调整（涨则减、跌则加）。' +
            '三大指数平均胜率 ' + fmt(summary.averageWinRate) + '%，' +
            '其中 ' + summary.bullishCount + ' 个处于 50% 以上';
        if (items.length) {
            els.tableDesc.textContent = '取样窗口 ' + items[0].periodStart + ' ~ ' + items[0].periodEnd +
                '（每个指数 ' + items[0].sampleCount + ' 个交易日）；基础胜率由最新点位在 365 天区间中的位置决定，' +
                '再按反向规则叠加「7日涨跌幅(%) × 10」：指数上涨扣减胜率，指数下跌增加胜率';
        }

        renderCards(items);
        renderTable(items);
        renderBarChart(items);
        renderCalcProcess(items);
        setTabCount('analysis', 'detail', items.length);

        if (!state.activeTrendCode || !items.some(function (i) {
                return i.code === state.activeTrendCode;
            })) {
            state.activeTrendCode = items.length ? items[items.length - 1].code : null;
        }
        renderTrendTabs(items);
        loadTrend(state.activeTrendCode);
    }

    function load(refresh) {
        clearError();
        showLoading(refresh ? '正在调用行情接口刷新数据…' : '正在加载三大指数数据…');
        els.refreshBtn.disabled = true;
        request(refresh ? API.refresh : API.winRate, refresh ? {method: 'POST'} : {method: 'GET'})
            .then(function (summary) {
                render(summary);
            })
            .catch(function (error) {
                showError(error.message);
            })
            .then(function () {
                hideLoading();
                els.refreshBtn.disabled = false;
            });
    }

    els.refreshBtn.addEventListener('click', function () {
        load(true);
    });

    /* ==================== 投资方案模块 ==================== */

    /** 图表标签用的紧凑金额：1.3万 / 2,500 / 781.25 */
    function fmtCompact(value) {
        var num = Number(value);
        if (isNaN(num)) {
            return '--';
        }
        if (Math.abs(num) >= 10000) {
            return (num / 10000).toFixed(num % 10000 === 0 ? 0 : 1) + '万';
        }
        if (Math.abs(num) >= 1000) {
            return String(Math.round(num));
        }
        return num.toFixed(2);
    }

    function clearPlanError() {
        els.planAlert.hidden = true;
        els.planAlert.textContent = '';
    }

    function showPlanError(message) {
        els.planAlert.hidden = false;
        els.planAlert.textContent = message;
    }

    function renderPlanCards(plan) {
        var items = plan.installments;
        var first = items[0];
        var last = items[items.length - 1];
        var cards = [
            {
                accent: '#2f6bff', label: '可用投资总金额',
                value: fmtMoney(plan.totalAmount), unit: '元',
                note: '按 ' + plan.times + ' 次投资分配，方案会把这笔钱精确用完'
            },
            {
                accent: '#12a150', label: '方案使用总额',
                value: fmtMoney(plan.usedAmount), unit: '元',
                note: '各期投入之和，等于可用总金额'
            },
            {
                accent: '#7c5cff', label: '首次投入金额',
                value: fmtMoney(plan.baseAmount), unit: '元',
                note: '= 总金额 ÷ 2^' + (plan.times - 1) + '，占总金额 ' + fmt(first.ratioOfTotal) + '%'
            },
            {
                accent: '#d98512', label: '末期放大倍数',
                value: fmt(plan.growthMultiple, 0), unit: '倍',
                note: '第 ' + plan.times + ' 次一次投入 ' + fmtMoney(last.amount) + ' 元（占 ' +
                    fmt(last.ratioOfTotal) + '%）'
            }
        ];
        els.planCards.innerHTML = cards.map(function (card) {
            return '' +
                '<div class="card" style="--accent:' + card.accent + '">' +
                '  <div class="card-head"><span class="card-name">' + esc(card.label) + '</span></div>' +
                '  <div class="card-value">' +
                '    <span class="num" style="color:' + card.accent + '">' + esc(card.value) + '</span>' +
                '    <span class="unit">' + esc(card.unit) + '</span>' +
                '  </div>' +
                '  <div class="metric-note">' + esc(card.note) + '</div>' +
                '</div>';
        }).join('');
    }

    function renderPlanChart(plan) {
        var items = plan.installments;
        var W = 980;
        var H = 400;
        var padL = 92;
        var padR = 34;
        var padT = 38;
        var padB = 86;
        var plotW = W - padL - padR;
        var plotH = H - padT - padB;
        var total = Number(plan.totalAmount) || 1;
        var max = total * 1.12;
        var baseline = padT + plotH;

        function yPos(value) {
            return padT + plotH * (1 - Math.max(0, value) / max);
        }

        var parts = [];
        parts.push('<defs>' +
            '<linearGradient id="planPlotBg" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#fcfdff"/><stop offset="100%" stop-color="#f6f9fd"/>' +
            '</linearGradient>' +
            '<linearGradient id="planBarFill" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#3fc98c"/><stop offset="100%" stop-color="#12a150"/>' +
            '</linearGradient></defs>');
        parts.push('<rect x="' + padL + '" y="' + padT + '" width="' + plotW + '" height="' + plotH +
            '" fill="url(#planPlotBg)" rx="10"/>');

        // Y 轴网格
        for (var g = 0; g <= 5; g++) {
            var value = max * g / 5;
            var y = yPos(value);
            parts.push('<line x1="' + padL + '" y1="' + y + '" x2="' + (W - padR) + '" y2="' + y +
                '" stroke="' + (g === 0 ? '#d8dfea' : '#eef1f7') + '" stroke-width="1"/>');
            parts.push('<text x="' + (padL - 12) + '" y="' + (y + 4) + '" text-anchor="end" ' +
                'font-size="11.5" fill="#8695ab">' + esc(fmtCompact(value)) + '</text>');
        }

        // 可用总金额参考线
        var totalY = yPos(total);
        parts.push('<line x1="' + padL + '" y1="' + totalY + '" x2="' + (W - padR) + '" y2="' + totalY +
            '" stroke="#2f6bff" stroke-width="1.4" stroke-dasharray="7 5" opacity="0.7"/>');
        parts.push('<text x="' + (W - padR) + '" y="' + (totalY - 8) + '" text-anchor="end" ' +
            'font-size="11.5" fill="#2f6bff">可用投资总金额 ' + esc(fmtMoney(total)) + ' 元</text>');

        var slot = plotW / items.length;
        var barW = Math.min(74, slot * 0.46);
        var showAllLabels = items.length <= 12;
        var cumulativePoints = [];

        items.forEach(function (item, index) {
            var centerX = padL + slot * index + slot / 2;
            var amount = Number(item.amount);
            var barY = yPos(amount);
            var barH = baseline - barY;

            parts.push('<rect class="bar" x="' + (centerX - barW / 2) + '" y="' + barY +
                '" width="' + barW + '" height="' + Math.max(1, barH) + '" rx="8" fill="url(#planBarFill)" ' +
                'style="animation-delay:' + (index * 0.08) + 's"><title>第 ' + item.periodNo + ' 次投入 ' +
                esc(fmtMoney(amount)) + ' 元</title></rect>');
            parts.push('<text x="' + centerX + '" y="' + (barY - 10) + '" text-anchor="middle" ' +
                'font-size="12" font-weight="700" fill="#0d7a3d">' + esc(fmtCompact(amount)) + '</text>');

            cumulativePoints.push((centerX) + ',' + yPos(Number(item.cumulativeAmount)));
            parts.push('<circle cx="' + centerX + '" cy="' + yPos(Number(item.cumulativeAmount)) +
                '" r="3.6" fill="#fff" stroke="#d98512" stroke-width="2.2"/>');

            if (showAllLabels || index % 2 === 0 || index === items.length - 1) {
                parts.push('<text x="' + centerX + '" y="' + (baseline + 24) + '" text-anchor="middle" ' +
                    'font-size="11.5" fill="#475569">第 ' + item.periodNo + ' 次</text>');
                parts.push('<text x="' + centerX + '" y="' + (baseline + 42) + '" text-anchor="middle" ' +
                    'font-size="11" fill="#8695ab">累计 ' + esc(fmtCompact(item.cumulativeAmount)) + '</text>');
            }
        });

        parts.push('<polyline points="' + cumulativePoints.join(' ') +
            '" fill="none" stroke="#d98512" stroke-width="2.2" stroke-linejoin="round"/>');
        els.planChart.innerHTML = parts.join('');
    }

    function renderPlanTable(plan) {
        els.planTableBody.innerHTML = plan.installments.map(function (item) {
            var accent = item.periodNo === plan.times ? '#d98512' : '#2f6bff';
            return '' +
                '<tr>' +
                '  <td><div class="index-cell" style="--accent:' + accent + '">' +
                '      <i class="index-badge"></i><div><div class="name">第 ' + item.periodNo + ' 次</div>' +
                '      <div class="code">占总额 ' + fmt(item.ratioOfTotal) + '%</div></div></div></td>' +
                '  <td class="num" style="font-weight:700">' + fmtMoney(item.amount) + '</td>' +
                '  <td class="num">' + fmt(item.ratioOfTotal) + '%</td>' +
                '  <td class="num">' + fmtMoney(item.cumulativeAmount) + '</td>' +
                '  <td class="num">' + fmt(item.ratioCumulative) + '%</td>' +
                '  <td style="color:#475569">' + esc(item.remark || '') + '</td>' +
                '</tr>';
        }).join('');
    }

    function renderPlanSummary(plan) {
        els.planRuleBox.textContent = plan.rule;
        els.planTableDesc.textContent = '共 ' + plan.times + ' 次投资，每次投入金额 = 此前各期累计投入金额；' +
            '方案使用总额 ' + fmtMoney(plan.usedAmount) + ' 元，可用总金额 ' +
            fmtMoney(plan.totalAmount) + ' 元，两者差额 ' + fmtMoney(plan.lastAdjustment) +
            ' 元（仅分位四舍五入尾差）';
        els.planChartDesc.textContent = '柱形为每一期投入金额，橙色折线为累计投入金额；' +
            '首期 ' + fmtMoney(plan.baseAmount) + ' 元，末期 ' +
            fmtMoney(plan.installments[plan.installments.length - 1].amount) + ' 元';
        els.planTimesChip.textContent = plan.times + ' 次';
        els.planUsedChip.textContent = fmtMoney(plan.usedAmount) + ' 元';
        els.footTimes.textContent = plan.times + ' 次';
        setTabCount('plan', 'detail', plan.times);
    }

    function generatePlan() {
        clearPlanError();
        var total = Number(els.planTotalAmount.value);
        var times = Number(els.planTimesInput.value);
        var config = planState.config;

        if (!total || total <= 0) {
            showPlanError('请输入大于 0 的可用投资总金额');
            return;
        }
        if (!times || times < config.minTimes || times > config.maxTimes) {
            showPlanError('投资次数需在 ' + config.minTimes + ' ~ ' + config.maxTimes + ' 之间');
            return;
        }

        els.planGenerateBtn.disabled = true;
        els.planGenerateBtn.innerHTML = '<span class="btn-icon">⟳</span>生成中…';
        request(API.planGenerate, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({totalAmount: total, times: times})
        })
            .then(function (plan) {
                planState.plan = plan;
                renderPlanCards(plan);
                renderPlanChart(plan);
                renderPlanTable(plan);
                renderPlanSummary(plan);
            })
            .catch(function (error) {
                showPlanError('方案生成失败：' + error.message);
            })
            .then(function () {
                els.planGenerateBtn.disabled = false;
                els.planGenerateBtn.innerHTML = '<span class="btn-icon">⟳</span>生成方案';
            });
    }

    function bindPlanEvents() {
        els.planGenerateBtn.addEventListener('click', generatePlan);
        els.planQuickAmounts.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-amount]');
            if (!button) {
                return;
            }
            els.planTotalAmount.value = button.getAttribute('data-amount');
            generatePlan();
        });
        [els.planTotalAmount, els.planTimesInput].forEach(function (input) {
            input.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') {
                    generatePlan();
                }
            });
            input.addEventListener('change', generatePlan);
        });
    }

    /** 首次进入投资方案模块时初始化（懒加载） */
    function initPlanModule() {
        if (planState.initialized) {
            return;
        }
        planState.initialized = true;
        bindPlanEvents();
        request(API.planConfig)
            .then(function (config) {
                planState.config = config;
                els.planTimesInput.min = config.minTimes;
                els.planTimesInput.max = config.maxTimes;
                els.planTotalAmount.value = config.defaultTotalAmount;
                els.planTimesInput.value = config.defaultTimes;
                els.footTimes.textContent = config.defaultTimes + ' 次';
            })
            .catch(function () {
                // 配置读取失败时使用页面默认值
            })
            .then(function () {
                generatePlan();
            });
    }

    /* ==================== 持仓管理模块 ==================== */

    var HOLDING_ACCENTS = ['#2f6bff', '#7c5cff', '#12a150', '#d98512', '#dc2f2f', '#0e9aa7'];

    function fmtSignedMoney(value) {
        if (value === null || value === undefined || value === '') {
            return '--';
        }
        var num = Number(value);
        if (isNaN(num)) {
            return '--';
        }
        var text = fmtMoney(Math.abs(num));
        if (num > 0) {
            return '+' + text;
        }
        if (num < 0) {
            return '-' + text;
        }
        return text;
    }

    function profitColor(value) {
        var num = Number(value);
        if (num > 0) {
            return '#12a150';
        }
        if (num < 0) {
            return '#dc2f2f';
        }
        return '#475569';
    }

    function clearHoldingError() {
        els.holdingAlert.hidden = true;
        els.holdingAlert.textContent = '';
    }

    function showHoldingError(message) {
        els.holdingAlert.hidden = false;
        els.holdingAlert.textContent = message;
    }

    function showHoldingNotice(message) {
        els.holdingAlert.hidden = false;
        els.holdingAlert.textContent = message;
        els.holdingAlert.style.background = '#e8f7ee';
        els.holdingAlert.style.borderColor = '#c3e7d2';
        els.holdingAlert.style.color = '#0d7a3d';
        window.setTimeout(function () {
            els.holdingAlert.hidden = true;
            els.holdingAlert.style.background = '';
            els.holdingAlert.style.borderColor = '';
            els.holdingAlert.style.color = '';
        }, 4000);
    }

    function showHoldingLoading(text) {
        if (!els.holdingLoading) {
            return;
        }
        els.holdingLoading.hidden = false;
        els.holdingLoading.lastElementChild.textContent = text || '正在装载持仓与最新点位…';
    }

    function hideHoldingLoading() {
        if (els.holdingLoading) {
            els.holdingLoading.hidden = true;
        }
    }

    /* ---------------- 汇总指标卡 ---------------- */
    function renderHoldingCards(stats) {
        var threshold = fmt(Number(stats.highWinRateThreshold), 0);
        var hasRecord = stats.buyCount > 0;
        var hasPredicted = stats.predictedRecordCount > 0;
        var cards = [
            {
                accent: '#2f6bff', label: '持仓数量 / 买入总次数',
                value: stats.positionCount, unit: '个持仓',
                note: '共记录 ' + stats.buyCount + ' 次买入'
            },
            {
                accent: '#7c5cff', label: '累计投入金额',
                value: fmtMoney(stats.totalAmount), unit: '元',
                note: '按最新点位折算市值 ' + fmtMoney(stats.marketValue) + ' 元'
            },
            {
                accent: profitColor(stats.profitAmount), label: '累计盈亏（最新点位 vs 买入成本）',
                value: fmtSignedMoney(stats.profitAmount), unit: '元',
                note: hasRecord
                    ? '盈亏比例 ' + fmtSigned(stats.profitPercent) + '%，最新点位高于成本即为盈利'
                    : '录入买入后，按关联指数最新点位与买入成本计算整体盈亏'
            },
            {
                accent: '#12a150', label: '真实胜率（全部持仓买入汇总）',
                value: hasRecord ? fmt(stats.realWinRate) + '%' : '--', unit: '',
                note: '盈利 ' + stats.winCount + ' 笔 ÷ 买入 ' + stats.buyCount + ' 次'
            },
            {
                accent: '#d98512', label: '预测胜率（每笔预测胜率之和 ÷ 次数）',
                value: hasPredicted ? fmt(stats.predictedWinRate) + '%' : '--', unit: '',
                note: '已算出预测胜率的买入 ' + stats.predictedRecordCount + ' 笔，合计后除以买入次数'
            },
            {
                accent: '#dc2f2f', label: '预测胜率 ≥ ' + threshold + '% 买入的真实胜率',
                value: stats.highWinRateCount > 0 ? fmt(stats.highWinRateRealWinRate) + '%' : '--',
                unit: '',
                note: '该区间买入 ' + stats.highWinRateCount + ' 笔，平均预测胜率 '
                    + fmt(stats.highWinRatePredictedWinRate) + '%'
            }
        ];
        els.holdingCards.innerHTML = cards.map(function (card) {
            return '' +
                '<div class="card" style="--accent:' + card.accent + '">' +
                '  <div class="card-head"><span class="card-name">' + esc(card.label) + '</span></div>' +
                '  <div class="card-value">' +
                '    <span class="num" style="color:' + card.accent + '">' + esc(card.value) + '</span>' +
                (card.unit ? '<span class="unit">' + esc(card.unit) + '</span>' : '') +
                '  </div>' +
                '  <div class="metric-note">' + esc(card.note) + '</div>' +
                '</div>';
        }).join('');
    }

    /* ---------------- 买入次数柱形图 ---------------- */
    function renderHoldingChart(positions) {
        var W = 980;
        var H = 420;
        var padL = 72;
        var padR = 74;
        var padT = 46;
        var padB = 112;
        var plotW = W - padL - padR;
        var plotH = H - padT - padB;

        if (!positions || positions.length === 0) {
            els.holdingChart.innerHTML = '<text x="' + (W / 2) + '" y="' + (H / 2) +
                '" text-anchor="middle" fill="#8695ab" font-size="14">暂无持仓，请在上方「录入买入」中新增一笔</text>';
            return;
        }

        var maxCount = 1;
        positions.forEach(function (item) {
            maxCount = Math.max(maxCount, Number(item.buyCount) || 0);
        });

        function yCount(value) {
            return padT + plotH * (1 - Math.max(0, value) / maxCount);
        }

        function yRate(value) {
            return padT + plotH * (1 - Math.max(0, Math.min(100, value)) / 100);
        }

        var parts = [];
        parts.push('<defs>' +
            '<linearGradient id="holdingPlotBg" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#fcfdff"/><stop offset="100%" stop-color="#f6f9fd"/>' +
            '</linearGradient>' +
            '<linearGradient id="holdingBarFill" x1="0" y1="0" x2="0" y2="1">' +
            '<stop offset="0%" stop-color="#6d9bff"/><stop offset="100%" stop-color="#2f6bff"/>' +
            '</linearGradient></defs>');
        parts.push('<rect x="' + padL + '" y="' + padT + '" width="' + plotW + '" height="' + plotH +
            '" fill="url(#holdingPlotBg)" rx="10"/>');

        // 左轴：买入次数
        var steps = Math.min(maxCount, 5);
        for (var g = 0; g <= steps; g++) {
            var value = maxCount * g / steps;
            var y = yCount(value);
            parts.push('<line x1="' + padL + '" y1="' + y + '" x2="' + (W - padR) + '" y2="' + y +
                '" stroke="' + (g === 0 ? '#d8dfea' : '#eef1f7') + '" stroke-width="1"/>');
            parts.push('<text x="' + (padL - 12) + '" y="' + (y + 4) + '" text-anchor="end" ' +
                'font-size="11.5" fill="#8695ab">' + fmt(value, value % 1 === 0 ? 0 : 1) + ' 次</text>');
        }

        // 右轴：预测胜率（%）
        for (var r = 0; r <= 100; r += 25) {
            parts.push('<text x="' + (W - padR + 12) + '" y="' + (yRate(r) + 4) + '" text-anchor="start" ' +
                'font-size="11.5" fill="#a4650c">' + r + '%</text>');
        }
        parts.push('<line x1="' + padL + '" y1="' + yRate(70) + '" x2="' + (W - padR) + '" y2="' + yRate(70) +
            '" stroke="#d98512" stroke-width="1.4" stroke-dasharray="7 5" opacity="0.65"/>');
        parts.push('<text x="' + (W - padR) + '" y="' + (yRate(70) - 7) + '" text-anchor="end" ' +
            'font-size="11.5" fill="#a4650c">预测胜率 70% 参考线</text>');

        var slot = plotW / positions.length;
        var barW = Math.min(96, slot * 0.42);
        var baseline = yCount(0);
        var ratePoints = [];

        positions.forEach(function (item, index) {
            var count = Number(item.buyCount) || 0;
            var centerX = padL + slot * index + slot / 2;
            var barY = yCount(count);
            var barH = Math.max(2, baseline - barY);

            parts.push('<rect class="bar" x="' + (centerX - barW / 2) + '" y="' + barY +
                '" width="' + barW + '" height="' + barH + '" rx="9" fill="url(#holdingBarFill)" ' +
                'style="animation-delay:' + (index * 0.1) + 's"><title>' + esc(item.positionName) + ' 共买入 ' +
                count + ' 次</title></rect>');
            parts.push('<text x="' + centerX + '" y="' + (barY - 12) + '" text-anchor="middle" ' +
                'font-size="19" font-weight="800" fill="#2f6bff">' + count + ' 次</text>');

            var rate = Number(item.predictedWinRate);
            if (!isNaN(rate)) {
                var rateY = yRate(rate);
                // 与柱顶「N 次」标签距离过近时，把胜率标签放到圆点下方，避免重叠
                var labelAbove = Math.abs(rateY - barY) >= 26;
                var labelY = labelAbove ? rateY - 13 : rateY + 20;
                var labelText = fmt(rate) + '%';
                var labelW = 12 + labelText.length * 6.4;
                ratePoints.push(centerX + ',' + rateY);
                parts.push('<circle cx="' + centerX + '" cy="' + rateY +
                    '" r="5" fill="#fff" stroke="#d98512" stroke-width="2.4"/>');
                parts.push('<rect x="' + (centerX - labelW / 2) + '" y="' + (labelY - 11) +
                    '" width="' + labelW + '" height="16" rx="5" fill="#ffffff" opacity="0.86"/>');
                parts.push('<text x="' + centerX + '" y="' + labelY + '" text-anchor="middle" ' +
                    'font-size="11.5" font-weight="700" fill="#b8820f">' + labelText + '</text>');
            }

            parts.push('<text x="' + centerX + '" y="' + (baseline + 24) + '" text-anchor="middle" ' +
                'font-size="13.5" font-weight="700" fill="#0f172a">' + esc(item.positionName) + '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 42) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#8695ab">' + esc(item.indexName) + ' · ' + esc(item.indexCode) +
                '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 60) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#475569">成本 ' + fmt(item.avgCostPrice, 3) + ' → 最新 ' +
                fmt(item.latestClose, 3) + '</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 78) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="' + profitColor(item.profitAmount) + '">盈亏 ' +
                fmtSignedMoney(item.profitAmount) + ' 元（' + fmtSigned(item.profitPercent) + '%）</text>');
            parts.push('<text x="' + centerX + '" y="' + (baseline + 96) + '" text-anchor="middle" ' +
                'font-size="11.5" fill="#475569">真实胜率 ' + fmt(item.realWinRate) + '%</text>');
        });

        if (ratePoints.length > 1) {
            parts.push('<polyline points="' + ratePoints.join(' ') +
                '" fill="none" stroke="#d98512" stroke-width="2.2" stroke-linejoin="round" opacity="0.9"/>');
        }
        els.holdingChart.innerHTML = parts.join('');
    }

    /* ---------------- 持仓明细表 ---------------- */
    function renderHoldingTable(positions) {
        if (!positions || positions.length === 0) {
            els.holdingTableBody.innerHTML = '<tr><td colspan="8" class="empty">' +
                '暂无持仓，请在上方录入第一笔买入</td></tr>';
            return;
        }
        els.holdingTableBody.innerHTML = positions.map(function (item, index) {
            var accent = HOLDING_ACCENTS[index % HOLDING_ACCENTS.length];
            var threshold = Number(holdingState.overview
                ? holdingState.overview.stats.highWinRateThreshold : 70);
            return '' +
                '<tr>' +
                '  <td><div class="index-cell" style="--accent:' + accent + '">' +
                '      <i class="index-badge"></i><div>' +
                '      <div class="name">' + esc(item.positionName) + '</div>' +
                '      <div class="code">' + esc(item.indexName) + ' ' + esc(item.indexCode) +
                '</div></div></div></td>' +
                '  <td class="num" style="font-weight:700">' + item.buyCount + ' 次' +
                '      <div class="code">' + esc(String(item.firstBuyDate || '--').slice(5)) +
                ' ~ ' + esc(String(item.lastBuyDate || '--').slice(5)) + '</div></td>' +
                '  <td class="num">' + fmtMoney(item.marketValue) +
                '      <div class="code">投入 ' + fmtMoney(item.totalAmount) + '</div></td>' +
                '  <td class="num">' + fmt(item.latestClose, 3) +
                '      <div class="code">成本 ' + fmt(item.avgCostPrice, 3) + '</div></td>' +
                '  <td class="num profit-num" style="color:' + profitColor(item.profitAmount) + '">' +
                fmtSignedMoney(item.profitAmount) +
                '      <div class="code">' + fmtSigned(item.profitPercent) + '%</div></td>' +
                '  <td><div class="win-cell">' +
                '      <span class="win-num" style="color:' + tierOf(Number(item.predictedWinRate)).color + '">' +
                fmt(item.predictedWinRate) + '%</span>' +
                '  </div></td>' +
                '  <td><div class="win-cell">' +
                '      <span class="win-num" style="color:' + tierOf(Number(item.realWinRate)).color + '">' +
                fmt(item.realWinRate) + '%</span>' +
                '      <span class="code">盈利 ' + item.winCount + ' / 亏损 ' + item.lossCount + '</span>' +
                '      <span class="code">≥' + fmt(threshold, 0) + '%：' +
                (item.highWinRateCount > 0 ? fmt(item.highWinRateRealWinRate) + '%' : '--') +
                ' · ' + item.highWinRateCount + ' 笔</span>' +
                '  </div></td>' +
                '  <td><div class="pos-actions">' +
                '      <button type="button" class="btn mini danger" data-holding-delete-position="' +
                item.id + '">删除持仓</button>' +
                '  </div></td>' +
                '</tr>';
        }).join('');
    }

    /* ---------------- 买入记录明细表 ---------------- */
    function renderHoldingRecords(records) {
        if (!records || records.length === 0) {
            els.holdingRecordBody.innerHTML = '<tr><td colspan="9" class="empty">' +
                '暂无买入记录</td></tr>';
            return;
        }
        var sorted = records.slice().sort(function (a, b) {
            if (a.buyDate === b.buyDate) {
                return (b.id || 0) - (a.id || 0);
            }
            return a.buyDate < b.buyDate ? 1 : -1;
        });
        els.holdingRecordBody.innerHTML = sorted.map(function (item) {
            var winRateTier = tierOf(Number(item.buyWinRate));
            var resultTag = item.resultLabel === '盈利' ? 'good'
                : (item.resultLabel === '亏损' ? 'bad' : 'plain');
            var note = item.winRateNote ? '<span class="record-formula">' + esc(item.winRateNote) + '</span>' : '';
            var costLine = item.costTradeDate && item.costTradeDate !== item.buyDate
                ? '<div class="code">成本取 ' + esc(String(item.costTradeDate).slice(5)) + '</div>' : '';
            return '' +
                '<tr>' +
                '  <td><div class="name">' + esc(item.positionName) + '</div></td>' +
                '  <td class="nowrap">' + esc(item.buyDate) +
                costLine + '</td>' +
                '  <td class="num">' + fmtMoney(item.buyAmount) + '</td>' +
                '  <td class="num">' + fmt(item.costPrice, 3) + '</td>' +
                '  <td class="num">' + fmt(item.latestClose, 3) +
                '      <div class="code" style="color:' + profitColor(item.changePercent) + '">' +
                fmtSigned(item.changePercent) + '%</div></td>' +
                '  <td><div class="win-cell">' +
                '      <span class="win-num" style="color:' +
                (item.buyWinRate === null || item.buyWinRate === undefined
                    ? '#8695ab' : winRateTier.color) + '">' +
                (item.buyWinRate === null || item.buyWinRate === undefined ? '--' : fmt(item.buyWinRate) + '%') +
                '</span>' +
                '      <span class="code">窗口 ' + esc(item.winRatePeriodStart || '--') + ' ~ ' +
                esc(item.winRatePeriodEnd || '--') + '（' + (item.winRateSampleCount || 0) + ' 个交易日）</span>' +
                note +
                '  </div></td>' +
                '  <td class="num profit-num" style="color:' + profitColor(item.profitAmount) + '">' +
                fmtSignedMoney(item.profitAmount) + '</td>' +
                '  <td><span class="tag ' + resultTag + '">' + esc(item.resultLabel) + '</span>' +
                (item.highWinRateHit ? ' <span class="tag good">预测命中</span>' : '') + '</td>' +
                '  <td><div class="pos-actions">' +
                '      <button type="button" class="btn mini danger" data-holding-delete-record="' +
                item.id + '">删除</button>' +
                '  </div></td>' +
                '</tr>';
        }).join('');
    }

    function renderHoldingSummary(overview) {
        var stats = overview.stats;
        els.holdingBuyChip.textContent = stats.buyCount + ' 次';
        els.holdingQuoteChip.textContent = '更新于 ' + fmtDateTime(overview.generatedAt);
        els.holdingChartDesc.textContent = overview.positions.length
            ? '柱高为各持仓的买入次数（共 ' + stats.buyCount + ' 次），橙色折线与圆点为持仓预测胜率'
            : '暂无持仓，录入买入后这里会展示每个持仓的买入次数';
        els.holdingTableDesc.textContent = '真实胜率 = 当前盈利的买入笔数 ÷ 买入次数：'
            + '全部持仓合计 ' + fmt(stats.realWinRate) + '%（盈利 ' + stats.winCount
            + ' 笔 / 亏损 ' + stats.lossCount + ' 笔）；预测胜率 ≥ '
            + fmt(stats.highWinRateThreshold, 0) + '% 的买入真实胜率 '
            + (stats.highWinRateCount > 0 ? fmt(stats.highWinRateRealWinRate) + '%' : '暂无样本')
            + '（' + stats.highWinRateCount + ' 笔）';
        els.holdingRecordDesc.textContent = '共 ' + stats.buyCount + ' 笔买入记录；'
            + overview.quoteSummary;
        els.holdingRuleBox.textContent = overview.winRateRule + '。' + overview.profitRule;
    }

    function renderHolding(overview) {
        holdingState.overview = overview;
        renderHoldingCards(overview.stats);
        renderHoldingChart(overview.positions);
        renderHoldingTable(overview.positions);
        renderHoldingRecords(overview.records);
        renderHoldingSummary(overview);
        setTabCount('holding', 'detail', overview.positions.length);
        setTabCount('holding', 'records', overview.records.length);
        // 首次进入且尚无持仓时，直接把用户带到「录入买入」页签
        if (!holdingState.autoPaneApplied) {
            holdingState.autoPaneApplied = true;
            if (!parseHash().pane && overview.stats.positionCount === 0) {
                activatePane('holding', 'input');
            }
        }
    }

    function loadHolding(forceRefresh) {
        clearHoldingError();
        showHoldingLoading(forceRefresh ? '正在调用行情接口刷新最新点位…' : '正在装载持仓与最新点位…');
        els.holdingRefreshBtn.disabled = true;
        request(forceRefresh ? API.holdingRefresh : API.holdingOverview,
            forceRefresh ? {method: 'POST'} : {method: 'GET'})
            .then(function (overview) {
                renderHolding(overview);
            })
            .catch(function (error) {
                showHoldingError('持仓数据加载失败：' + error.message);
            })
            .then(function () {
                hideHoldingLoading();
                els.holdingRefreshBtn.disabled = false;
            });
    }

    function submitHoldingBuy() {
        clearHoldingError();
        var config = holdingState.config;
        var name = (els.holdingName.value || '').trim();
        var code = els.holdingIndex.value;
        var date = els.holdingDate.value;
        var amount = Number(els.holdingAmount.value);

        if (!name) {
            showHoldingError('请填写持仓名称');
            return;
        }
        if (!code) {
            showHoldingError('请选择持仓关联的指数');
            return;
        }
        if (!date) {
            showHoldingError('请选择具体的买入日期');
            return;
        }
        if (!amount || amount <= 0) {
            showHoldingError('请输入大于 0 的买入金额');
            return;
        }

        els.holdingSubmitBtn.disabled = true;
        els.holdingSubmitBtn.innerHTML = '<span class="btn-icon">⟳</span>记录中…';
        request(API.holdingBuy, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({positionName: name, indexCode: code, buyDate: date, buyAmount: amount})
        })
            .then(function (overview) {
                renderHolding(overview);
                // 记录成功后自动切到「持仓概览」，让用户立刻看到盈亏变化
                activatePane('holding', 'overview');
                els.holdingAmount.value = '';
                showHoldingNotice('已记录买入：' + name + ' · ' + date + ' · ' + fmtMoney(amount) + ' 元');
                window.scrollTo({top: 0, behavior: 'smooth'});
            })
            .catch(function (error) {
                showHoldingError('买入记录失败：' + error.message);
            })
            .then(function () {
                els.holdingSubmitBtn.disabled = false;
                els.holdingSubmitBtn.innerHTML = '<span class="btn-icon">＋</span>记录买入';
            });
    }

    function deleteHoldingPosition(id) {
        if (!window.confirm('删除该持仓会同时删除它的全部买入记录，是否继续？')) {
            return;
        }
        clearHoldingError();
        request(API.holdingPosition + id, {method: 'DELETE'})
            .then(function (overview) {
                renderHolding(overview);
                showHoldingNotice('持仓已删除');
            })
            .catch(function (error) {
                showHoldingError('删除失败：' + error.message);
            });
    }

    function deleteHoldingRecord(id) {
        if (!window.confirm('确认删除这笔买入记录？该持仓最后一笔记录被删除后，持仓也会一并移除。')) {
            return;
        }
        clearHoldingError();
        request(API.holdingRecord + id, {method: 'DELETE'})
            .then(function (overview) {
                renderHolding(overview);
                showHoldingNotice('买入记录已删除');
            })
            .catch(function (error) {
                showHoldingError('删除失败：' + error.message);
            });
    }

    function bindHoldingEvents() {
        els.holdingSubmitBtn.addEventListener('click', submitHoldingBuy);
        els.holdingRefreshBtn.addEventListener('click', function () {
            loadHolding(true);
        });
        Array.prototype.forEach.call(
            [els.holdingName, els.holdingDate, els.holdingAmount], function (input) {
                input.addEventListener('keydown', function (event) {
                    if (event.key === 'Enter') {
                        submitHoldingBuy();
                    }
                });
            });
        els.holdingQuick.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-name]');
            if (!button) {
                return;
            }
            els.holdingName.value = button.getAttribute('data-name');
            els.holdingIndex.value = button.getAttribute('data-code');
        });
        els.holdingTableBody.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-holding-delete-position]');
            if (button) {
                deleteHoldingPosition(button.getAttribute('data-holding-delete-position'));
            }
        });
        els.holdingRecordBody.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-holding-delete-record]');
            if (button) {
                deleteHoldingRecord(button.getAttribute('data-holding-delete-record'));
            }
        });
    }

    /** 首次进入持仓管理模块时初始化（懒加载） */
    function initHoldingModule() {
        if (holdingState.initialized) {
            return;
        }
        holdingState.initialized = true;
        bindHoldingEvents();
        request(API.holdingConfig)
            .then(function (config) {
                holdingState.config = config;
                els.holdingIndex.innerHTML = (config.indices || []).map(function (item) {
                    return '<option value="' + esc(item.code) + '">' + esc(item.name) +
                        '（' + esc(item.code) + '）</option>';
                }).join('');
                els.holdingDate.max = config.today;
                els.holdingDate.value = config.today;
                els.holdingDateHint.textContent = '不能晚于今天（' + config.today +
                    '）；买入日非交易日时自动向前取最近交易日';
                els.holdingAmountHint.textContent = '单笔上限 ' + fmtMoney(config.maxBuyAmount) +
                    ' 元；预测胜率阈值 ' + fmt(config.highWinRateThreshold, 0) + '%';
                els.holdingFormHint.textContent = '预测胜率 = 以买入日期为终点、最近 ' + config.windowDays +
                    ' 天行情算出的最终胜率（与投资分析模块同一套规则）';
            })
            .catch(function () {
                // 配置读取失败时保留页面默认值，仍可查看已有持仓
            })
            .then(function () {
                loadHolding(false);
            });
    }

    /* ==================== 风险评估模块 ==================== */

    function clearRiskError() {
        els.riskAlert.hidden = true;
        els.riskAlert.textContent = '';
    }

    function showRiskError(message) {
        els.riskAlert.hidden = false;
        els.riskAlert.textContent = message;
    }

    function showRiskLoading(text) {
        els.riskLoading.hidden = false;
        els.riskLoading.lastElementChild.textContent = text || '正在评估各持仓的止盈/止损方案…';
    }

    function hideRiskLoading() {
        els.riskLoading.hidden = true;
    }

    /** 档位标签样式：当前档 > 已触发 > 未触发 */
    function levelChipClass(level) {
        if (level.current) {
            return 'current';
        }
        return level.triggered ? 'done' : 'todo';
    }

    function renderRiskCards(stats) {
        var cards = [
            {
                accent: '#2f6bff', label: '评估方案数（持仓数）',
                value: stats.planCount, unit: '个方案',
                note: '建议止盈 ' + stats.takeProfitCount + ' 个 · 建议止损 ' + stats.stopLossCount
                    + ' 个 · 持有观察 ' + stats.watchCount + ' 个'
                    + (stats.unavailableCount > 0 ? ' · 无法评估 ' + stats.unavailableCount + ' 个' : '')
            },
            {
                accent: '#dc2f2f', label: '当前建议卖出金额合计',
                value: fmtMoney(stats.suggestedSellAmount), unit: '元',
                note: '按各方案当前已触发档位与当前市值折算，执行后剩余仓位见明细'
            },
            {
                accent: '#d98512', label: '已到「全部卖出」档的方案数',
                value: stats.liquidationCount, unit: '个方案',
                note: '盈利 ≥ ' + fmt(stats.takeProfitClearTrigger, 0) + '% 或亏损 ≤ '
                    + fmt(stats.stopLossClearTrigger, 0) + '% 时建议清仓'
            },
            {
                accent: profitColor(stats.profitAmount), label: '方案合计盈亏（最新点位 vs 买入成本）',
                value: fmtSignedMoney(stats.profitAmount), unit: '元',
                note: '盈亏比例 ' + fmtSigned(stats.profitPercent) + '%；投入 '
                    + fmtMoney(stats.totalAmount) + ' 元，市值 ' + fmtMoney(stats.marketValue) + ' 元'
            }
        ];
        els.riskCards.innerHTML = cards.map(function (card) {
            return '' +
                '<div class="card" style="--accent:' + card.accent + '">' +
                '  <div class="card-head"><span class="card-name">' + esc(card.label) + '</span></div>' +
                '  <div class="card-value">' +
                '    <span class="num" style="color:' + card.accent + '">' + esc(card.value) + '</span>' +
                (card.unit ? '<span class="unit">' + esc(card.unit) + '</span>' : '') +
                '  </div>' +
                '  <div class="metric-note">' + esc(card.note) + '</div>' +
                '</div>';
        }).join('');
    }

    /** 各方案建议列表（一个持仓一个方案） */
    function renderRiskList(plans) {
        if (!plans || plans.length === 0) {
            els.riskList.innerHTML = '<div class="empty">暂无持仓方案，请先在「持仓管理」中录入买入</div>';
            return;
        }
        els.riskList.innerHTML = plans.map(function (plan, index) {
            var accent = HOLDING_ACCENTS[index % HOLDING_ACCENTS.length];
            var color = profitColor(plan.profitPercent);
            var amountText = (plan.actionAmount === null || plan.actionAmount === undefined)
                ? '' : '≈ ' + fmtMoney(plan.actionAmount) + ' 元';
            var metrics = plan.quoteAvailable
                ? '<span>当前盈亏 <b style="color:' + color + '">' + fmtSigned(plan.profitPercent) + '%</b></span>' +
                '<span>盈亏金额 <b style="color:' + color + '">' + fmtSignedMoney(plan.profitAmount) + ' 元</b></span>' +
                '<span>当前市值 ' + fmtMoney(plan.marketValue) + ' 元</span>' +
                '<span>平均成本 ' + fmt(plan.avgCostPrice, 3) + ' · 最新 ' + fmt(plan.latestClose, 3) +
                '（' + esc(plan.latestTradeDate || '--') + '）</span>'
                : '<span>关联指数行情不可用，暂无法评估</span>';
            var nextText = '';
            if (plan.nextTriggerLabel) {
                nextText = '下一档 ' + plan.nextTriggerLabel + '：' + plan.nextActionLabel
                    + ((plan.nextActionAmount === null || plan.nextActionAmount === undefined)
                        ? '' : '（≈ ' + fmtMoney(plan.nextActionAmount) + ' 元）')
                    + '，还差 ' + fmt(plan.gapToNextPercent) + ' 个百分点';
            } else if (plan.quoteAvailable) {
                nextText = '已到最后一档，无后续档位';
            }
            var chips = (plan.levels || []).map(function (level) {
                return '<span class="level-chip ' + levelChipClass(level) + '">' + esc(level.triggerLabel)
                    + ' 卖' + esc(level.sellRatioLabel) + '</span>';
            }).join('');
            return '' +
                '<div class="risk-item" style="--accent:' + accent + '">' +
                '  <div class="risk-item-head">' +
                '    <div class="risk-name">' + esc(plan.positionName) +
                '      <span class="risk-code">' + esc(plan.indexName) + ' ' + esc(plan.indexCode) + '</span>' +
                '    </div>' +
                '    <span class="tag ' + esc(plan.riskLevelTag) + '">' + esc(plan.riskLevel) + '</span>' +
                '  </div>' +
                '  <div class="risk-metrics">' + metrics + '</div>' +
                '  <div class="risk-advice">' +
                '    <span class="risk-action">建议：' + esc(plan.actionLabel) + '</span>' +
                '    <span class="risk-amount">' + esc(amountText) + '</span>' +
                '  </div>' +
                '  <div class="risk-note">' + esc(plan.suggestion) + '</div>' +
                '  <div class="risk-meta">' +
                '    <span>已触发：' + esc(plan.triggeredSummary) + '</span>' +
                (plan.quoteAvailable
                    ? '<span>当前档卖出占初始仓位 ' + fmt(plan.actionRatioOfInitial, 4) + '%，执行后剩余 '
                    + fmt(plan.remainRatioAfterAction, 3) + '%</span>' : '') +
                (nextText ? '<span>' + esc(nextText) + '</span>' : '') +
                '  </div>' +
                (chips ? '<div class="risk-chips">' + chips + '</div>' : '') +
                (plan.oppositeHint ? '<div class="risk-hint">' + esc(plan.oppositeHint) + '</div>' : '') +
                '</div>';
        }).join('');
    }

    /** 方案建议明细列表：每个持仓 × 每一档位一行 */
    function renderRiskTable(plans) {
        if (!plans || plans.length === 0) {
            els.riskTableBody.innerHTML = '<tr><td colspan="10" class="empty">' +
                '暂无持仓方案，请先在「持仓管理」中录入买入</td></tr>';
            return;
        }
        var rows = [];
        plans.forEach(function (plan) {
            var positionCell = '<div class="risk-name">' + esc(plan.positionName) + '</div>' +
                '<div class="code">' + esc(plan.indexName) + ' ' + esc(plan.indexCode) + '</div>';
            if (!plan.levels || plan.levels.length === 0) {
                rows.push('<tr class="group-start">' +
                    '<td>' + positionCell + '</td>' +
                    '<td><span class="tag plain">--</span></td>' +
                    '<td class="num">--</td><td class="num">--</td><td class="num">--</td>' +
                    '<td class="num">--</td><td class="num">--</td><td class="num">--</td>' +
                    '<td><span class="tag plain">无法评估</span></td>' +
                    '<td class="risk-remark">' + esc(plan.suggestion) + '</td>' +
                    '</tr>');
                return;
            }
            plan.levels.forEach(function (level, levelIndex) {
                var statusTag = level.current ? 'bad' : (level.triggered ? 'mid' : 'plain');
                var statusText = level.current ? '当前档（需执行）' : (level.triggered ? '已触发' : '未触发');
                var profitSide = level.side === 'PROFIT';
                rows.push('<tr class="' + (levelIndex === 0 ? 'group-start ' : '')
                    + (level.current ? 'current-row' : '') + '">' +
                    '<td>' + (levelIndex === 0 ? positionCell
                        : '<div class="code">' + esc(plan.positionName) + '</div>') + '</td>' +
                    '<td><span class="tag ' + (profitSide ? 'good' : 'bad') + '">' +
                    (profitSide ? '止盈' : '止损') + '</span></td>' +
                    '<td class="num" style="font-weight:700">' + esc(level.triggerLabel) + '</td>' +
                    '<td class="num">' + esc(level.sellRatioLabel) + '</td>' +
                    '<td class="num">' + fmt(Number(level.sellRatioOfInitial) * 100, 4) + '%</td>' +
                    '<td class="num">' + fmt(Number(level.cumulativeSellRatio) * 100, 4) + '%</td>' +
                    '<td class="num">' + fmt(Number(level.remainRatioAfter) * 100, 4) + '%</td>' +
                    '<td class="num">' + fmtMoney(level.amountAtCurrentValue) + '</td>' +
                    '<td><span class="tag ' + statusTag + '">' + statusText + '</span></td>' +
                    '<td class="risk-remark">' + esc(level.remark) + '</td>' +
                    '</tr>');
            });
        });
        els.riskTableBody.innerHTML = rows.join('');
    }

    function renderRiskLadder(element, ladder, profitSide) {
        if (!element) {
            return;
        }
        if (!ladder || ladder.length === 0) {
            element.innerHTML = '<li>暂未配置阶梯</li>';
            return;
        }
        element.innerHTML = ladder.map(function (level) {
            var action = level.sellRatioLabel === '全部'
                ? '全部卖出' : '卖出剩余仓位的 ' + level.sellRatioLabel;
            return '<li>' +
                '<span class="ladder-trigger ' + (profitSide ? 'profit' : 'loss') + '">'
                + esc(level.triggerLabel) + '</span>' +
                '<span class="ladder-action">' + esc(action) + '</span>' +
                '<span class="ladder-meta">累计卖出 ' + fmt(Number(level.cumulativeSellRatio) * 100, 3)
                + '%，剩余 ' + fmt(Number(level.remainRatioAfter) * 100, 3) + '%</span>' +
                '</li>';
        }).join('');
    }

    function renderRisk(overview) {
        riskState.overview = overview;
        var stats = overview.stats;
        renderRiskCards(stats);
        renderRiskList(overview.plans);
        renderRiskTable(overview.plans);

        els.riskPlanChip.textContent = stats.planCount + ' 个';
        els.riskSellChip.textContent = fmtMoney(stats.suggestedSellAmount) + ' 元';
        els.riskListDesc.textContent = stats.planCount === 0
            ? '暂无持仓方案，请先在「持仓管理」中录入买入'
            : '共 ' + stats.planCount + ' 个方案：建议止盈 ' + stats.takeProfitCount
            + ' 个、建议止损 ' + stats.stopLossCount + ' 个、持有观察 ' + stats.watchCount
            + ' 个；当前建议卖出金额合计 ' + fmtMoney(stats.suggestedSellAmount) + ' 元';
        var levelRows = overview.plans.reduce(function (total, plan) {
            return total + (plan.levels ? Math.max(1, plan.levels.length) : 1);
        }, 0);
        setTabCount('risk', 'detail', levelRows);
        els.riskTableDesc.textContent = '共 ' + stats.planCount + ' 个方案 / ' + levelRows
            + ' 行阶梯明细；每档「卖出比例」指触发时剩余仓位的比例，金额按当前市值折算；'
            + (overview.quoteSummary || '');
        els.riskRuleBox.textContent = overview.takeProfitRule + ' ' + overview.stopLossRule + ' '
            + overview.calcRule;

        if (riskState.config) {
            renderRiskLadder(els.riskProfitLadder, riskState.config.takeProfitLadder, true);
            renderRiskLadder(els.riskLossLadder, riskState.config.stopLossLadder, false);
        } else {
            var first = overview.plans.filter(function (plan) {
                return plan.levels && plan.levels.length > 0;
            })[0];
            if (first) {
                renderRiskLadder(els.riskProfitLadder, first.levels.filter(function (level) {
                    return level.side === 'PROFIT';
                }), true);
                renderRiskLadder(els.riskLossLadder, first.levels.filter(function (level) {
                    return level.side === 'LOSS';
                }), false);
            }
        }
    }

    function loadRisk(forceRefresh) {
        clearRiskError();
        showRiskLoading(forceRefresh ? '正在调用行情接口刷新最新点位…' : '正在评估各持仓的止盈/止损方案…');
        els.riskRefreshBtn.disabled = true;
        request(forceRefresh ? API.riskRefresh : API.riskOverview,
            forceRefresh ? {method: 'POST'} : {method: 'GET'})
            .then(function (overview) {
                renderRisk(overview);
            })
            .catch(function (error) {
                showRiskError('风险评估加载失败：' + error.message);
            })
            .then(function () {
                hideRiskLoading();
                els.riskRefreshBtn.disabled = false;
            });
    }

    /** 首次进入风险评估模块时初始化（懒加载） */
    function initRiskModule() {
        if (riskState.initialized) {
            return;
        }
        riskState.initialized = true;
        els.riskRefreshBtn.addEventListener('click', function () {
            loadRisk(true);
        });
        request(API.riskConfig)
            .then(function (config) {
                riskState.config = config;
                renderRiskLadder(els.riskProfitLadder, config.takeProfitLadder, true);
                renderRiskLadder(els.riskLossLadder, config.stopLossLadder, false);
            })
            .catch(function () {
                // 配置读取失败时仍可展示方案建议
            })
            .then(function () {
                loadRisk(false);
            });
    }

    /* ==================== 模块切换 ==================== */

    /** 各模块当前页签（懒加载时记住上次选择） */
    var PANE_STATE = { analysis: 'overview', plan: 'overview', holding: 'overview', risk: 'overview' };

    /**
     * 解析地址栏：#/模块[/页签]，例如 #/risk/detail。
     */
    function parseHash() {
        var raw = (window.location.hash || '').replace(/^#\/?/, '').trim();
        var parts = raw.split('/');
        var moduleName = MODULE_TITLES[parts[0]] ? parts[0] : 'analysis';
        var paneKey = parts[1] && parts[1].length > 0 ? parts[1] : null;
        return {module: moduleName, pane: paneKey};
    }

    function resolveModuleFromHash() {
        return parseHash().module;
    }

    function moduleElement(name) {
        return document.querySelector('.module[data-module="' + name + '"]');
    }

    /**
     * 切换模块内页签：显示 data-pane 匹配的区块，其余隐藏。
     */
    function activatePane(moduleName, paneKey) {
        var moduleEl = moduleElement(moduleName);
        if (!moduleEl || !paneKey) {
            return;
        }
        var panes = moduleEl.querySelectorAll('[data-pane]:not(.tab-btn)');
        var matched = false;
        Array.prototype.forEach.call(panes, function (el) {
            var match = el.getAttribute('data-pane') === paneKey;
            matched = matched || match;
            el.classList.toggle('pane-hidden', !match);
        });
        if (!matched) {
            var first = moduleEl.querySelector('.page-tabs .tab-btn');
            if (first) {
                activatePane(moduleName, first.getAttribute('data-pane'));
            }
            return;
        }
        Array.prototype.forEach.call(moduleEl.querySelectorAll('.page-tabs .tab-btn'), function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-pane') === paneKey);
        });
        PANE_STATE[moduleName] = paneKey;
    }

    function bindPaneTabs() {
        Array.prototype.forEach.call(document.querySelectorAll('.page-tabs'), function (bar) {
            var moduleEl = bar.closest('.module');
            var moduleName = moduleEl ? moduleEl.getAttribute('data-module') : null;
            if (!moduleName) {
                return;
            }
            bar.addEventListener('click', function (event) {
                var button = event.target.closest('.tab-btn');
                if (!button) {
                    return;
                }
                var paneKey = button.getAttribute('data-pane');
                var target = '#/' + moduleName + '/' + paneKey;
                if (window.location.hash === target) {
                    activatePane(moduleName, paneKey);
                } else {
                    // 同步到地址栏：便于分享/后退，并触发 hashchange 切换页签
                    window.location.hash = target;
                }
                var head = moduleEl.querySelector('.page-head');
                if (head && window.scrollY > 0) {
                    var top = head.getBoundingClientRect().bottom + window.scrollY - 10;
                    if (window.scrollY > top) {
                        window.scrollTo({top: Math.max(0, top), behavior: 'smooth'});
                    }
                }
            });
        });
    }

    /**
     * 在页签上标注条目数量（信息密度提示），如「买入记录 6」。
     */
    function setTabCount(moduleName, paneKey, count) {
        var moduleEl = moduleElement(moduleName);
        if (!moduleEl) {
            return;
        }
        var button = moduleEl.querySelector('.tab-btn[data-pane="' + paneKey + '"]');
        if (!button) {
            return;
        }
        var badge = button.querySelector('.tab-count');
        if (count === null || count === undefined) {
            if (badge) {
                badge.parentNode.removeChild(badge);
            }
            return;
        }
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'tab-count';
            button.appendChild(badge);
        }
        badge.textContent = count;
    }

    /** 回到顶部按钮 */
    function initToTop() {
        var button = document.getElementById('toTop');
        if (!button) {
            return;
        }
        window.addEventListener('scroll', function () {
            button.classList.toggle('show', window.scrollY > 420);
        });
        button.addEventListener('click', function () {
            window.scrollTo({top: 0, behavior: 'smooth'});
        });
    }

    function activateModule(name, paneKey) {
        Array.prototype.forEach.call(document.querySelectorAll('.nav-item[data-module]'), function (item) {
            item.classList.toggle('active', item.getAttribute('data-module') === name);
        });
        Array.prototype.forEach.call(document.querySelectorAll('.module'), function (module) {
            module.classList.toggle('active', module.getAttribute('data-module') === name);
        });
        if (name === 'plan') {
            initPlanModule();
        }
        if (name === 'holding') {
            initHoldingModule();
        }
        if (name === 'risk') {
            initRiskModule();
        }
        if (paneKey) {
            PANE_STATE[name] = paneKey;
        }
        activatePane(name, PANE_STATE[name]);
        window.scrollTo(0, 0);
    }

    Array.prototype.forEach.call(document.querySelectorAll('.nav-item[data-module]'), function (item) {
        item.addEventListener('click', function (event) {
            event.preventDefault();
            var name = item.getAttribute('data-module');
            if (window.location.hash !== '#/' + name) {
                window.location.hash = '#/' + name;
            } else {
                activateModule(name);
            }
        });
    });

    window.addEventListener('hashchange', function () {
        var target = parseHash();
        activateModule(target.module, target.pane);
    });

    bindPaneTabs();
    initToTop();
    load(false);
    var initial = parseHash();
    activateModule(initial.module, initial.pane);
})();
