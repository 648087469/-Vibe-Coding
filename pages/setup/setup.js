const P = require('../../utils/plan.js');

// 每日学习时间：小时 + 分钟两列滚轮，最多 8 小时 45 分钟
const HOURS = [0, 1, 2, 3, 4, 5, 6, 7, 8];
const MINUTES = [0, 15, 30, 45];

const MODES = [
  { key: 'mixed', name: '分散式', desc: '每天同时推进 2~3 个科目，交替轮换' },
  { key: 'block', name: '集中式', desc: '一个科目连攻若干天，逐个突破' }
];

const modsOf = (exam, keys) =>
  exam.modules.map(m => ({
    key: m.key,
    name: m.name,
    part: m.part,
    mock: !!m.mock,
    checked: !keys || keys.indexOf(m.key) >= 0
  }));

// 分钟数还原成滚轮的下标（小时 / 分钟两列）
const timeIndexFrom = minutes => {
  const h = Math.min(HOURS.length - 1, Math.floor(minutes / 60));
  const mi = MINUTES.indexOf(minutes % 60);
  return [h, mi < 0 ? 0 : mi];
};

Page({
  data: {
    exams: P.EXAMS.map(e => ({ key: e.key, name: e.name, full: e.full })),
    examIndex: 0,
    minutes: 120,
    timeText: '2 小时',
    timeRange: [HOURS.map(h => h + ' 小时'), MINUTES.map(m => m + ' 分')],
    timeIndex: [2, 0],
    start: '',
    end: '',
    examDate: '',
    examHint: '',
    modes: MODES,
    mode: 'mixed',
    modeHint: '',
    theoryHint: '',
    mockHint: '',
    mods: [],
    days: 0,
    taskCount: 0,
    hasPlan: false,
    planSummary: ''
  },

  onLoad() {
    // 有已保存的计划就回填它的配置，避免每次进入都像重新开始
    const saved = P.load();
    const today = new Date();
    const examIndex = saved ? Math.max(0, P.EXAMS.map(e => e.key).indexOf(saved.examKey)) : 0;
    const exam = P.EXAMS[examIndex];
    const minutes = saved ? saved.minutes : 120;
    const examDate = saved ? P.addDays(P.parseKey(saved.end), 1) : P.defaultExamDate(exam.key, today);
    this.setData({
      examIndex,
      start: saved ? saved.start : P.toKey(today),
      end: saved ? saved.end : P.toKey(P.addDays(examDate, -1)),
      examDate: P.toKey(examDate),
      examHint: exam.name + '笔试预估 ' + P.toKey(examDate) + '（按历年规律推算，以公告为准）',
      minutes,
      timeIndex: timeIndexFrom(minutes),
      mode: saved && saved.mode ? saved.mode : 'mixed',
      mods: modsOf(exam, saved && saved.modules)
    });
    this.refresh();
  },

  onShow() {
    const plan = P.load();
    const patches = { hasPlan: !!plan };
    if (plan) {
      const s = P.stats(plan);
      patches.planSummary =
        plan.examName + ' · ' + s.days + ' 天 · ' + s.total + ' 个任务 · 已完成 ' + s.rate + '%';
    }
    this.setData(patches);
  },

  // 重新计算预估天数与任务数
  refresh() {
    const { start, end, minutes, mods, mode } = this.data;
    const days = Math.max(0, P.diffDays(start, end) + 1);
    const normals = mods.filter(m => !m.mock && m.checked).length;
    const hasMock = mods.some(m => m.mock && m.checked);
    const blocks = P.blocksPerDay(minutes);
    const per = Math.max(1, mode === 'block' ? blocks : Math.min(blocks, normals));

    // 勾选模考后每周日为模考日；只勾模考则每天都是
    const mockDays = hasMock ? (normals ? Math.min(days, P.mockDayCount(start, end, mode)) : days) : 0;
    const studyDays = Math.max(0, days - mockDays);
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    const mockTotal = P.MOCK.brush + P.MOCK.review;

    this.setData({
      days,
      timeText: (h ? h + ' 小时' : '') + (m ? (h ? ' ' : '') + m + ' 分钟' : ''),
      taskCount: studyDays * per + mockDays * 2,
      modeHint:
        mode === 'block'
          ? normals
            ? '每个科目连攻约 ' + Math.ceil(studyDays / normals) + ' 天，冲刺期集中做模考'
            : '请至少选择一个科目'
          : '每天轮换 ' + Math.min(per, normals || 1) + ' 个科目，交替推进',
      theoryHint: '普通科目按「理论 : 刷题 = 1 : 3」拆：每 4 个时段里 1 个理论、3 个刷题',
      mockHint:
        '模考科目（' +
        (mode === 'block' ? '考前 ' + P.MOCK_TAIL_DAYS + ' 天起' : '每周六、周日') +
        '）：刷题 ' + P.MOCK.brush + ' 分钟 + 复盘 ' + P.MOCK.review + ' 分钟' +
        (hasMock && minutes < mockTotal
          ? '（模考日需 ' + mockTotal + ' 分钟，已超过你的每日时长，当天请预留时间）'
          : '')
    });
  },

  onModeChange(e) {
    this.setData({ mode: e.currentTarget.dataset.key });
    this.refresh();
  },

  onExamChange(e) {
    const i = Number(e.currentTarget.dataset.index);
    const exam = P.EXAMS[i];
    const examDate = P.defaultExamDate(exam.key, new Date());
    this.setData({
      examIndex: i,
      mods: modsOf(exam),
      examDate: P.toKey(examDate),
      end: P.toKey(P.addDays(examDate, -1)),
      examHint: exam.name + '笔试预估 ' + P.toKey(examDate) + '（按历年规律推算，以公告为准）'
    });
    this.refresh();
  },

  onTimeChange(e) {
    const [hi, mi] = e.detail.value;
    this.setData({
      timeIndex: [hi, mi],
      minutes: HOURS[hi] * 60 + MINUTES[mi]
    });
    this.refresh();
  },

  onStartChange(e) {
    const start = e.detail.value;
    const patches = { start };
    if (P.diffDays(start, this.data.end) < 0) {
      patches.examDate = P.toKey(P.addDays(P.parseKey(start), 1));
      patches.end = start;
    }
    this.setData(patches);
    this.refresh();
  },

  onEndChange(e) {
    const end = e.detail.value;
    this.setData({
      end,
      examDate: P.toKey(P.addDays(P.parseKey(end), 1)),
      examHint: '结束日期的次日视为笔试日'
    });
    this.refresh();
  },

  onToggleModule(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({
      mods: this.data.mods.map(m =>
        m.key === key ? Object.assign({}, m, { checked: !m.checked }) : m
      )
    });
    this.refresh();
  },

  onGenerate() {
    const { start, end, minutes, mods, examIndex, mode } = this.data;
    const modules = mods.filter(m => m.checked).map(m => m.key);
    if (!modules.length) {
      return wx.showToast({ title: '至少选择一个模块', icon: 'none' });
    }
    if (minutes < 15) {
      return wx.showToast({ title: '每日学习时间至少 15 分钟', icon: 'none' });
    }
    if (P.diffDays(start, end) < 0) {
      return wx.showToast({ title: '考试日期需晚于开始日期', icon: 'none' });
    }

    const go = () => {
      P.save(
        P.generatePlan({
          examKey: P.EXAMS[examIndex].key,
          start,
          end,
          minutes,
          modules,
          mode
        })
      );
      wx.navigateTo({ url: '/pages/calendar/calendar' });
    };

    if (P.load()) {
      wx.showModal({
        title: '覆盖当前计划？',
        content: '重新生成会清空已有的调整与打卡记录。',
        success: res => res.confirm && go()
      });
    } else {
      go();
    }
  },

  onViewPlan() {
    wx.navigateTo({ url: '/pages/calendar/calendar' });
  },

  onClear() {
    wx.showModal({
      title: '清空计划？',
      content: '将删除本机保存的备考计划。',
      success: res => {
        if (res.confirm) {
          P.clear();
          this.setData({ hasPlan: false });
        }
      }
    });
  }
});
