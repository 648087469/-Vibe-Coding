const P = require('../../utils/plan.js');

const WEEK_CN = ['日', '一', '二', '三', '四', '五', '六'];
const DAY_LABEL = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
// 课程表节次；每天几节、每节多长由生成计划时的每日时长决定
const SLOTS = [
  { name: '第1节', time: '08:30' },
  { name: '第2节', time: '14:00' },
  { name: '第3节', time: '19:30' },
  { name: '第4节', time: '21:30' },
  { name: '第5节', time: '22:30' }
];

Page({
  data: {
    mode: 'week',
    examName: '',
    modeName: '',
    stat: { total: 0, done: 0, days: 0, minutes: 0, rate: 0 },
    week: WEEK_CN,
    weekTitle: '',
    slots: [],
    rows: [],
    title: '',
    cells: [],
    modKeys: [],
    modNames: [],
    popup: false,
    date: '',
    dayLabel: '',
    tasks: [],
    dayMinutes: 0
  },

  onLoad() {
    const plan = P.load();
    if (!plan) {
      wx.showToast({ title: '请先生成计划', icon: 'none' });
      return setTimeout(() => wx.navigateBack(), 800);
    }

    this.plan = plan;
    const exam = P.getExam(plan.examKey);
    const mods = exam.modules.filter(m => plan.modules.indexOf(m.key) >= 0);
    const start = P.parseKey(plan.start);
    this.firstMonday = P.toKey(this.mondayOf(start));

    this.setData({
      examName: plan.examName,
      modeName: plan.mode === 'block' ? '集中式' : '分散式',
      modKeys: mods.map(m => m.key),
      modNames: mods.map(m => m.name),
      stat: P.stats(plan)
    });
    this.showMonth(start.getFullYear(), start.getMonth() + 1);
    this.buildWeek(this.firstMonday);
  },

  mondayOf(date) {
    return P.addDays(date, -((date.getDay() + 6) % 7));
  },

  onMode(e) {
    const mode = e.currentTarget.dataset.mode;
    if (mode === this.data.mode) return;
    this.setData({ mode });
    const m = P.parseKey(this.monday);
    if (mode === 'week') {
      const sameMonth = m.getFullYear() === this.data.year && m.getMonth() + 1 === this.data.month;
      this.buildWeek(
        sameMonth ? this.monday : P.toKey(this.mondayOf(new Date(this.data.year, this.data.month - 1, 1)))
      );
    } else {
      this.showMonth(m.getFullYear(), m.getMonth() + 1);
    }
  },

  // ---------- 课程表（按周） ----------
  buildWeek(mondayKey) {
    this.monday = mondayKey;
    const monday = P.parseKey(mondayKey);
    const today = P.toKey(new Date());
    const slots = SLOTS.slice(0, this.slotCount());
    const rows = [];

    for (let i = 0; i < 7; i++) {
      const d = P.addDays(monday, i);
      const key = P.toKey(d);
      const list = this.plan.days[key] || [];
      rows.push({
        key,
        label: DAY_LABEL[i],
        date: d.getMonth() + 1 + '/' + d.getDate(),
        today: key === today,
        off: key < this.plan.start || key > this.plan.end,
        cells: slots.map((slot, j) => {
          const t = list[j];
          return {
            k: key + '_' + j,
            name: t ? t.name : '',
            minutes: t ? t.minutes : 0,
            cls: t ? (t.done ? 'done' : 'todo') : 'blank'
          };
        })
      });
    }

    const end = P.addDays(monday, 6);
    const weekNo = Math.round(P.diffDays(this.firstMonday, mondayKey) / 7) + 1;
    this.setData({
      slots,
      rows,
      weekTitle:
        monday.getMonth() +
        1 +
        '月' +
        monday.getDate() +
        '日 - ' +
        (end.getMonth() + 1) +
        '月' +
        end.getDate() +
        '日' +
        (weekNo > 0 ? ' · 第 ' + weekNo + ' 周' : '')
    });
  },

  // 课表列数 = 计划每天节数；若手动加过更多任务，则以最多的一天为准
  slotCount() {
    let max = Math.min(P.blocksPerDay(this.plan.minutes), this.plan.modules.length);
    Object.keys(this.plan.days).forEach(k => {
      const n = this.plan.days[k].length;
      if (n > max) max = n;
    });
    return Math.min(SLOTS.length, Math.max(1, max));
  },

  onWeekStep(e) {
    this.buildWeek(
      P.toKey(P.addDays(P.parseKey(this.monday), Number(e.currentTarget.dataset.delta) * 7))
    );
  },

  onThisWeek() {
    this.buildWeek(P.toKey(this.mondayOf(new Date())));
  },

  // ---------- 日历（按月） ----------
  showMonth(year, month) {
    this.setData({ year, month, title: year + ' 年 ' + month + ' 月' });
    this.buildCells();
  },

  buildCells() {
    const { year, month } = this.data;
    const today = P.toKey(new Date());
    const cells = [];
    const padCount = new Date(year, month - 1, 1).getDay();
    const daysInMonth = new Date(year, month, 0).getDate();

    for (let i = 0; i < padCount; i++) cells.push({ key: 'p' + i, empty: true });

    for (let d = 1; d <= daysInMonth; d++) {
      const key = P.toKey(new Date(year, month - 1, d));
      const tasks = this.plan.days[key] || [];
      const done = tasks.filter(t => t.done).length;
      cells.push({
        key,
        day: d,
        count: tasks.length,
        allDone: tasks.length > 0 && done === tasks.length,
        partial: done > 0 && done < tasks.length,
        inPlan: key >= this.plan.start && key <= this.plan.end,
        isToday: key === today
      });
    }

    while (cells.length % 7) cells.push({ key: 'q' + cells.length, empty: true });
    this.setData({ cells });
  },

  onPrev() {
    const { year, month } = this.data;
    this.showMonth(month === 1 ? year - 1 : year, month === 1 ? 12 : month - 1);
  },

  onNext() {
    const { year, month } = this.data;
    this.showMonth(month === 12 ? year + 1 : year, month === 12 ? 1 : month + 1);
  },

  // ---------- 当天任务编辑 ----------
  onCellTap(e) {
    const key = e.currentTarget.dataset.key;
    this.plan.days[key] = this.plan.days[key] || [];
    this.setData({ popup: true, date: key });
    this.syncDay();
  },

  closePopup() {
    this.setData({ popup: false });
  },

  noop() {},

  syncDay() {
    const tasks = (this.plan.days[this.data.date] || []).map((t, i) =>
      Object.assign({}, t, {
        mi: this.data.modKeys.indexOf(t.module),
        time: SLOTS[i] ? SLOTS[i].time : ''
      })
    );
    const d = P.parseKey(this.data.date);
    this.setData({
      tasks,
      dayLabel: d.getMonth() + 1 + '月' + d.getDate() + '日 周' + WEEK_CN[d.getDay()],
      dayMinutes: tasks.reduce((sum, t) => sum + t.minutes, 0)
    });
  },

  save() {
    P.save(this.plan);
    this.setData({ stat: P.stats(this.plan) });
    if (this.data.mode === 'week') this.buildWeek(this.monday);
    else this.buildCells();
    this.syncDay();
  },

  findTask(id) {
    return (this.plan.days[this.data.date] || []).filter(t => t.id === id)[0];
  },

  toggleDone(e) {
    const task = this.findTask(e.currentTarget.dataset.id);
    if (task) task.done = !task.done;
    this.save();
  },

  changeMinutes(e) {
    const { id, delta } = e.currentTarget.dataset;
    const task = this.findTask(id);
    if (!task) return;
    task.minutes = Math.min(480, Math.max(15, task.minutes + Number(delta)));
    this.save();
  },

  changeModule(e) {
    const task = this.findTask(e.currentTarget.dataset.id);
    if (!task) return;
    const i = Number(e.detail.value);
    task.module = this.data.modKeys[i];
    task.name = this.data.modNames[i];
    this.save();
  },

  removeTask(e) {
    const id = e.currentTarget.dataset.id;
    const date = this.data.date;
    const left = this.plan.days[date].filter(t => t.id !== id);
    if (left.length) {
      this.plan.days[date] = left;
    } else {
      delete this.plan.days[date];
    }
    this.save();
  },

  addTask() {
    const date = this.data.date;
    const tasks = this.plan.days[date] || [];
    tasks.push({
      id: date + '#' + Date.now(),
      module: this.data.modKeys[0],
      name: this.data.modNames[0],
      minutes: 60,
      done: false
    });
    this.plan.days[date] = tasks;
    this.save();
  },

  onBack() {
    wx.navigateBack();
  },

  onReset() {
    wx.showModal({
      title: '重置计划？',
      content: '将删除本机保存的备考计划，回到设置页。',
      success: res => {
        if (res.confirm) {
          P.clear();
          wx.reLaunch({ url: '/pages/setup/setup' });
        }
      }
    });
  }
});
