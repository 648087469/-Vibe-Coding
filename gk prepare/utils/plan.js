const STORAGE_KEY = 'gk_plan_v1';

// 模考时长：刷题 2 小时 + 复盘 1 小时（国考、浙江省考一致）
const MOCK = { brush: 120, review: 60 };
// 集中式：统一放到最后，覆盖考前 15 天（不足 15 天则每天模考）
const MOCK_TAIL_DAYS = 15;

// 考试类型：决定可选模块与默认笔试日规则
const EXAMS = [
  {
    key: 'guokao',
    name: '国考',
    full: '中央机关及其直属机构公务员考试',
    // 公共科目笔试：11 月 26 日及以后的第一个周日
    // 历年实际：2023-11-26 / 2024-12-01 / 2025-11-30，均落在该规则上
    exam: { month: 11, day: 26, weekday: 0 },
    modules: [
      { key: 'yanyu', name: '言语理解与表达', part: '行测' },
      { key: 'shuliang', name: '数量关系', part: '行测' },
      { key: 'panduan', name: '判断推理', part: '行测' },
      { key: 'ziliao', name: '资料分析', part: '行测' },
      { key: 'changshi', name: '常识判断', part: '行测' },
      { key: 'shenlun', name: '申论', part: '申论' },
      { key: 'fupan', name: '模考与复盘', part: '综合', mock: true }
    ]
  },
  {
    key: 'zhejiang',
    name: '浙江省考',
    full: '浙江省各级机关单位公务员考试',
    // 公共科目笔试：12 月 7 日及以后的第一个周日
    // 历年实际：2023-12-10 / 2024-12-08 / 2025-12-07，均落在该规则上
    exam: { month: 12, day: 7, weekday: 0 },
    modules: [
      { key: 'yanyu', name: '言语理解与表达', part: '行测' },
      { key: 'shuliang', name: '数量关系与数字推理', part: '行测' },
      { key: 'panduan', name: '判断推理', part: '行测' },
      { key: 'ziliao', name: '资料分析', part: '行测' },
      { key: 'changshi', name: '常识判断与浙江省情', part: '行测' },
      { key: 'shenlun', name: '申论（浙江卷）', part: '申论' },
      { key: 'fupan', name: '模考与复盘', part: '综合', mock: true }
    ]
  }
];

const pad = n => (n < 10 ? '0' + n : '' + n);

const toKey = date =>
  date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());

const parseKey = key => {
  const p = key.split('-');
  return new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
};

const addDays = (date, n) =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate() + n);

const diffDays = (a, b) => Math.round((parseKey(b) - parseKey(a)) / 86400000);

const getExam = key => EXAMS.filter(e => e.key === key)[0] || EXAMS[0];

// 某年按规则取笔试日：month/day 当天及之后的第一个 weekday
function examDateOfYear(year, rule) {
  const base = new Date(year, rule.month - 1, rule.day);
  return new Date(
    year,
    rule.month - 1,
    rule.day + ((rule.weekday - base.getDay() + 7) % 7)
  );
}

/**
 * 默认笔试日：按历年规律推算今年日期（今年已过则顺延到明年）。
 * 仅为预估默认值，用户可在设置页自行修改。
 */
function defaultExamDate(examKey, today) {
  const rule = getExam(examKey).exam;
  const thisYear = examDateOfYear(today.getFullYear(), rule);
  const nextYear = examDateOfYear(today.getFullYear() + 1, rule);
  return toKey(thisYear) > toKey(today) ? thisYear : nextYear;
}

// 每天 1~3 个时段：时间越长拆得越细
const blocksPerDay = minutes => (minutes >= 210 ? 3 : minutes >= 90 ? 2 : 1);

/**
 * 模考日数量：集中式取考前 MOCK_TAIL_DAYS 天（计划不足则每天都是），
 * 分散式取每周末的周六 + 周日。
 */
function mockDayCount(start, end, mode) {
  const total = diffDays(start, end) + 1;
  if (mode === 'block') return Math.min(total, MOCK_TAIL_DAYS);
  let n = 0;
  const last = parseKey(end);
  for (let d = parseKey(start); d <= last; d = addDays(d, 1)) {
    if (d.getDay() === 0 || d.getDay() === 6) n++;
  }
  return n;
}

/**
 * 生成逐日计划。
 * mode = 'mixed' 分散式：每天轮换多个科目
 * mode = 'block' 集中式：一个科目连续攻若干天
 * 普通科目按「理论 : 刷题 = 1 : 3」拆（理论排在前面）；
 * 模考科目独占每周日，固定为「刷题 2 小时 + 复盘 1 小时」。
 * cfg: { examKey, start, end, minutes, modules: [], mode }
 */
function generatePlan(cfg) {
  const exam = getExam(cfg.examKey);
  const picked = exam.modules.filter(m => cfg.modules.indexOf(m.key) >= 0);
  const all = picked.length ? picked : exam.modules;
  const normal = all.filter(m => !m.mock);
  const mock = all.filter(m => m.mock)[0] || null;
  const block = cfg.mode === 'block';
  const total = Math.max(1, diffDays(cfg.start, cfg.end) + 1);

  const dates = [];
  for (let i = 0; i < total; i++) dates.push(addDays(parseKey(cfg.start), i));

  // 1. 划出模考日：
  //    集中式 → 统一放到最后，覆盖考前 15 天（不足 15 天则每天都是模考）
  //    分散式 → 每周六、周日
  //    只勾了模考 → 每天都是模考
  const tailStart = toKey(dates[total - Math.min(total, MOCK_TAIL_DAYS)]);
  const isMockDay = !mock
    ? () => false
    : !normal.length
      ? () => true
      : block
        ? d => toKey(d) >= tailStart
        : d => d.getDay() === 0 || d.getDay() === 6;
  const studyDays = dates.filter(d => !isMockDay(d));

  // 2. 普通日每天几节、每节多长
  const blocks = blocksPerDay(cfg.minutes);
  const per = Math.max(1, block ? blocks : Math.min(blocks, normal.length));
  const unit = Math.max(15, Math.round(cfg.minutes / per / 5) * 5);

  // 3. 普通日先排科目：集中式按阶段连攻，分散式按轮转
  const span = block && normal.length ? Math.ceil(studyDays.length / normal.length) : 0;
  const rows = studyDays.map((d, i) => {
    const row = [];
    for (let j = 0; j < per; j++) {
      row.push(
        block
          ? normal[Math.min(normal.length - 1, Math.floor(i / span))]
          : normal[(i * per + j) % normal.length]
      );
    }
    return row;
  });

  // 4. 每个科目约 1/4 的时段用于理论，其余刷题
  const count = {};
  rows.forEach(row => row.forEach(m => (count[m.key] = (count[m.key] || 0) + 1)));
  const theoryLeft = {};
  Object.keys(count).forEach(k => (theoryLeft[k] = Math.max(1, Math.round(count[k] / 4))));

  // 5. 装填每天的课程
  const days = {};
  let n = 0;
  dates.forEach(d => {
    const key = toKey(d);
    if (isMockDay(d)) {
      days[key] = [
        {
          id: key + '#0',
          module: mock.key,
          name: '模考·刷题',
          minutes: MOCK.brush,
          done: false
        },
        {
          id: key + '#1',
          module: mock.key,
          name: '模考·复盘',
          minutes: MOCK.review,
          done: false
        }
      ];
      return;
    }
    days[key] = (rows[n++] || []).map((m, j) => {
      const theory = theoryLeft[m.key] > 0;
      if (theory) theoryLeft[m.key]--;
      return {
        id: key + '#' + j,
        module: m.key,
        name: m.name + (theory ? '·理论' : '·刷题'),
        minutes: unit,
        done: false
      };
    });
  });

  return {
    examKey: exam.key,
    examName: exam.name,
    mode: block ? 'block' : 'mixed',
    start: cfg.start,
    end: toKey(addDays(parseKey(cfg.start), total - 1)),
    minutes: cfg.minutes,
    modules: all.map(m => m.key),
    days
  };
}

function stats(plan) {
  let total = 0;
  let done = 0;
  let minutes = 0;
  Object.keys(plan.days).forEach(k =>
    plan.days[k].forEach(t => {
      total++;
      minutes += t.minutes;
      if (t.done) done++;
    })
  );
  return {
    total,
    done,
    minutes,
    days: diffDays(plan.start, plan.end) + 1,
    rate: total ? Math.round((done / total) * 100) : 0
  };
}

module.exports = {
  EXAMS,
  MOCK,
  toKey,
  parseKey,
  addDays,
  diffDays,
  getExam,
  defaultExamDate,
  examDateOfYear,
  blocksPerDay,
  MOCK_TAIL_DAYS,
  mockDayCount,
  generatePlan,
  stats,
  save: plan => wx.setStorageSync(STORAGE_KEY, plan),
  load: () => wx.getStorageSync(STORAGE_KEY) || null,
  clear: () => wx.removeStorageSync(STORAGE_KEY)
};
