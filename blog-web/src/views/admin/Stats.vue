<template>
  <div class="stats-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>
            访问统计
            <el-tag size="small" type="info" style="margin-left: 8px">
              {{ stats.date || today }}
            </el-tag>
          </span>
          <div class="header-actions">
            <el-date-picker
              v-model="date"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期(默认今天)"
              format="YYYY-MM-DD"
              style="width: 180px"
              @change="loadAll"
            />
            <el-button @click="goToday">回到今天</el-button>
            <el-switch
              v-model="autoRefresh"
              active-text="5 分钟自动刷新"
              @change="onAutoRefreshChange"
            />
          </div>
        </div>
      </template>

      <!-- 顶部 4 张数据卡 -->
      <div class="kpi-row">
        <div class="kpi-card kpi-pv">
          <div class="kpi-label">总访问量 PV</div>
          <div class="kpi-value">{{ stats.pv ?? 0 }}</div>
          <div class="kpi-sub">每次 GET 算一次</div>
        </div>
        <div class="kpi-card kpi-uv">
          <div class="kpi-label">独立访客 UV</div>
          <div class="kpi-value">{{ stats.uv ?? 0 }}</div>
          <div class="kpi-sub">按 IP 去重</div>
        </div>
        <div class="kpi-card kpi-peak">
          <div class="kpi-label">访问峰值小时</div>
          <div class="kpi-value">
            <template v-if="stats.peakHour != null">
              {{ String(stats.peakHour).padStart(2, '0') }}:00
            </template>
            <template v-else>-</template>
          </div>
          <div class="kpi-sub">当日访问最多的小时</div>
        </div>
        <div class="kpi-card kpi-refresh">
          <div class="kpi-label">数据最后刷新</div>
          <div class="kpi-value" style="font-size: 18px; line-height: 36px;">{{ lastRefreshedAt }}</div>
          <div class="kpi-sub">{{ autoRefresh ? '自动刷新开' : '自动刷新关' }}</div>
        </div>
      </div>

      <!-- 分时柱状图 + 设备/浏览器分布 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="16">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span>分时访问(每小时 PV)</span>
            </template>
            <div class="hour-chart">
              <div
                v-for="(count, hour) in (stats.byHour || {})"
                :key="hour"
                class="hour-col"
                :title="`${hour}:00 — ${count} 次访问`"
              >
                <div class="hour-bar" :style="{ height: hourBarHeight(count) + 'px' }">
                  <span class="hour-num">{{ count || '' }}</span>
                </div>
                <div class="hour-label">{{ hour }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="never" class="chart-card">
            <template #header><span>设备类型</span></template>
            <div class="pie-chart-wrap">
              <div ref="deviceChartRef" class="pie-chart" />
              <div v-if="!hasDeviceData" class="pie-empty">暂无访问数据</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="8">
          <el-card shadow="never" class="chart-card">
            <template #header><span>操作系统</span></template>
            <div class="pie-chart-wrap">
              <div ref="osChartRef" class="pie-chart" />
              <div v-if="!hasOsData" class="pie-empty">暂无访问数据</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="never" class="chart-card">
            <template #header><span>浏览器</span></template>
            <div class="pie-chart-wrap">
              <div ref="browserChartRef" class="pie-chart" />
              <div v-if="!hasBrowserData" class="pie-empty">暂无访问数据</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-card shadow="never" class="chart-card">
            <template #header><span>访客省份</span></template>
            <div class="pie-chart-wrap">
              <div ref="provinceChartRef" class="pie-chart" />
              <div v-if="!hasProvinceData" class="pie-empty">暂无访问数据</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 访客明细 -->
    <el-card class="detail-card">
      <template #header>
        <div class="header-bar">
          <span>访客明细</span>
          <span class="text-soft" style="font-weight: 400;">{{ detailTotal }} 条记录 · {{ groupTotal }} 个访客分组</span>
        </div>
      </template>

      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm"
          style="width: 360px"
          @change="reloadDetail"
        />
        <el-input v-model="filters.ip" placeholder="筛选 IP(模糊)" clearable style="width: 180px" @keyup.enter="reloadDetail" @clear="reloadDetail" />
        <el-select v-model="filters.deviceType" placeholder="设备" clearable style="width: 120px" @change="reloadDetail">
          <el-option v-for="d in ['PC','Mobile','Tablet','Other']" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="filters.os" placeholder="系统" clearable style="width: 130px" @change="reloadDetail">
          <el-option v-for="o in ['Windows','macOS','iOS','Android','Linux','Other']" :key="o" :label="o" :value="o" />
        </el-select>
        <el-select v-model="filters.browser" placeholder="浏览器" clearable style="width: 130px" @change="reloadDetail">
          <el-option v-for="b in ['Chrome','Edge','Safari','Firefox','QQ','WeChat','UC','Other']" :key="b" :label="b" :value="b" />
        </el-select>
        <el-select v-model="filters.province" placeholder="省份" clearable style="width: 120px" @change="reloadDetail">
          <el-option v-for="p in provinceOptions" :key="p" :label="formatProvince(p)" :value="p" />
        </el-select>
        <el-button @click="reloadDetail" :loading="detailLoading">查询</el-button>
        <div class="toolbar-spacer" />
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>

      <div class="table-scroll">
        <div v-if="!detailList.length && !detailLoading" class="empty-tip">暂无访问记录</div>

        <!-- 按 IP + 日期 分组折叠 -->
        <div v-for="g in groupedLogs" :key="g.key" class="log-group">
          <!-- 汇总行（点击展开/收起） -->
          <div
            class="group-header"
            :class="{ 'is-expanded': expandedGroups.has(g.key) }"
            @click="toggleGroup(g.key)"
          >
            <span class="gh-toggle">
              <el-icon><ArrowRight /></el-icon>
            </span>
            <span class="gh-ip"><code class="ip-text">{{ g.ip }}</code></span>
            <span class="gh-province">{{ formatProvince(g.province) }}</span>
            <span class="gh-device">
              <el-tag size="small" :type="deviceTag(g.deviceType)">{{ g.deviceType }}</el-tag>
            </span>
            <span class="gh-os">{{ g.os }}</span>
            <span class="gh-browser">{{ g.browser }}</span>
            <span class="gh-count">
              <el-tag size="small" type="warning" effect="plain">{{ g.rows.length }} 次访问</el-tag>
            </span>
            <span class="gh-time-range">{{ g.firstTime }} ~ {{ g.lastTime }}</span>
          </div>

          <!-- 展开明细 -->
          <transition name="group-expand">
            <div v-if="expandedGroups.has(g.key)" class="group-detail">
              <div v-for="(row, ri) in g.rows" :key="row.id" class="detail-row">
                <span class="dr-index">{{ ri + 1 }}</span>
                <span class="dr-path">
                  <code class="path-text">{{ row.path }}</code>
                  <span v-if="pageLabel(row.path)" class="path-page">→ {{ pageLabel(row.path) }}</span>
                </span>
                <span class="dr-time">{{ formatTime(row.visitTime) }}</span>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <el-pagination
        v-if="groupTotal > groupPageSize"
        class="pager"
        background
        layout="prev, pager, next, jumper, total"
        :total="groupTotal"
        :page-size="groupPageSize"
        :current-page="groupPage"
        @current-change="(p) => { groupPage = p; expandedGroups.value = new Set() }"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { Refresh, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminStatsToday, adminStatsLogs } from '@/api/admin'
// 按需引入 ECharts，仅引入饼图与必要组件以控制体积
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

// 晴天主题调色板：botany 蓝主调 + 暖橙辅色，按数据项依次取色
const PIE_PALETTE = [
  '#38bdf8', '#fbbf24', '#22d3ee', '#0ea5e9', '#f97316',
  '#06b6d4', '#0284c7', '#f59e0b', '#67e8f9', '#84cc16'
]

const buildPieData = (map, nameFmt = (x) => x) => {
  if (!map) return []
  return Object.entries(map)
    .map(([name, value]) => ({ name: nameFmt(name || '未知'), value: Number(value) || 0 }))
    .filter((d) => d.value > 0)
    .sort((a, b) => b.value - a.value)
}

// 各饼图「是否有数据」标记——空时由模板层覆盖层显示「暂无访问数据」（不依赖 ECharts 渲染）
const hasDeviceData   = computed(() => Object.values(stats.byDevice   || {}).some(v => Number(v) > 0))
const hasOsData       = computed(() => Object.values(stats.byOs       || {}).some(v => Number(v) > 0))
const hasBrowserData  = computed(() => Object.values(stats.byBrowser  || {}).some(v => Number(v) > 0))
const hasProvinceData = computed(() => Object.values(stats.byProvince || {}).some(v => Number(v) > 0))

const pieOption = (data) => {
  const hasData = Array.isArray(data) && data.length > 0
  // 空数据时返回空 option，不画任何东西；占位由模板层 .pie-empty 覆盖层负责（不依赖 ECharts title 渲染，更稳）
  if (!hasData) return {}
  return {
    tooltip: { trigger: 'item', formatter: ({ name, value, percent }) => `${name}<br/>${value} 次 (${percent}%)` },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12, color: 'var(--c-ink-soft)' } },
    color: PIE_PALETTE,
    series: [{
      name: '占比',
      type: 'pie',
      radius: '62%',
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: {
        formatter: ({ name, percent }) => `${name}\n${percent}%`,
        fontSize: 12,
        color: 'var(--c-ink)'
      },
      labelLine: { length: 8, length2: 8 },
      data
    }]
  }
}

const today = new Date().toISOString().slice(0, 10)
const date = ref('')  // 默认空 = 今天
const autoRefresh = ref(true)
const lastRefreshedAt = ref('-')

const stats = reactive({
  date: '',
  pv: 0,
  uv: 0,
  peakHour: null,
  byHour: {},
  byDevice: {},
  byOs: {},
  byBrowser: {},
  byProvince: {}
})

// ── 饼图（ECharts）──
const deviceChartRef = ref(null)
const osChartRef = ref(null)
const browserChartRef = ref(null)
const provinceChartRef = ref(null)
let deviceChart, osChart, browserChart, provinceChart
const chartObservers = []

const initChart = (el, data) => {
  if (!el) return null
  const inst = echarts.init(el)
  inst.setOption(pieOption(data), true)
  // 容器尺寸变化时自动 resize：解决「onMounted 时容器尺寸为 0、el-card body 后续 layout」问题
  const ro = new ResizeObserver(() => inst.resize())
  ro.observe(el)
  chartObservers.push(ro)
  // 首屏兜底：再 resize 一次，确保画布尺寸正确
  requestAnimationFrame(() => inst.resize())
  return inst
}
const renderCharts = () => {
  deviceChart?.setOption(pieOption(buildPieData(stats.byDevice)), true)
  osChart?.setOption(pieOption(buildPieData(stats.byOs)), true)
  browserChart?.setOption(pieOption(buildPieData(stats.byBrowser)), true)
  provinceChart?.setOption(pieOption(buildPieData(stats.byProvince, formatProvince)), true)
  // 数据更新后兜底 resize，确保画布与容器同步
  deviceChart?.resize()
  osChart?.resize()
  browserChart?.resize()
  provinceChart?.resize()
}
const resizeCharts = () => {
  deviceChart?.resize()
  osChart?.resize()
  browserChart?.resize()
  provinceChart?.resize()
}
// resize 防抖：ECharts resize 涉及重新布局，连续 resize 合并到 150ms 后只执行一次
let resizeTimer = null
const onResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(resizeCharts, 150)
}

// 监听整个 stats（深度），任一字段变化都重绘饼图；比监听字段数组更稳
watch(stats, () => {
  nextTick(renderCharts)
}, { deep: true })

// ── 详情 ──
const dateRange = ref(null)
const filters = reactive({ ip: '', deviceType: '', os: '', browser: '', province: '' })
const detailList = ref([])
const detailTotal = ref(0)
// 拉取时用大 size，让前端拿到足够多的原始记录来分组
const detailPage = ref(1)
const detailFetchSize = 500
const detailLoading = ref(false)

// ── 分组折叠 + 组级分页 ──
const expandedGroups = ref(new Set())
const groupPage = ref(1)
const groupPageSize = 10

/** 将 detailList 按 IP + 日期(YYYY-MM-DD) 分组（全量，不分页） */
const allGroups = computed(() => {
  const map = new Map()
  for (const row of detailList.value) {
    const datePart = row.visitTime ? String(row.visitTime).slice(0, 10) : ''
    const key = `${row.ip || ''}__${datePart}`
    if (!map.has(key)) {
      map.set(key, {
        key,
        ip: row.ip,
        province: row.province,
        deviceType: row.deviceType,
        os: row.os,
        browser: row.browser,
        rows: [],
        firstTime: '',
        lastTime: ''
      })
    }
    const g = map.get(key)
    g.rows.push(row)
    const t = formatTime(row.visitTime)
    if (!g.firstTime || t > g.firstTime) g.firstTime = t
    if (!g.lastTime || t < g.lastTime) g.lastTime = t
  }
  return Array.from(map.values()).sort((a, b) =>
    a.firstTime > b.firstTime ? -1 : a.firstTime < b.firstTime ? 1 : 0
  )
})

/** 当前页的分组切片 */
const groupedLogs = computed(() => {
  const start = (groupPage.value - 1) * groupPageSize
  return allGroups.value.slice(start, start + groupPageSize)
})

/** 分组总数 */
const groupTotal = computed(() => allGroups.value.length)

const toggleGroup = (key) => {
  const s = expandedGroups.value
  if (s.has(key)) {
    s.delete(key)
    // 触发响应式更新
    expandedGroups.value = new Set(s)
  } else {
    expandedGroups.value = new Set(s.add(key))
  }
}

let refreshTimer = null

const hourMax = computed(() => {
  const v = Object.values(stats.byHour || {})
  return Math.max(1, ...v)
})

const provinceOptions = computed(() => {
  const keys = Object.keys(stats.byProvince || {})
  return keys.filter(k => k && k !== '未知').sort()
})

const hourBarHeight = (count) => {
  // 最大 140px
  return Math.max(2, Math.round((count / hourMax.value) * 140))
}

const deviceTag = (t) => {
  if (t === 'PC') return 'primary'
  if (t === 'Mobile') return 'success'
  if (t === 'Tablet') return 'warning'
  return 'info'
}

// 省份/地区格式化：ip2region 对境外 IP 返回英文国家名（如 United States），
// 国内 IP 返回中文省/市（含汉字）。境外统一包成「境外(国家)」更友好；国内原样。
const formatProvince = (p) => {
  if (!p || p === '未知') return '未知'
  // 含汉字视为国内省市，原样返回
  if (/[一-龥]/.test(p)) return p
  // 其余（英文国家名等）视为境外
  return `境外(${p})`
}

// 后端接口路径 → 页面名映射；用于访客明细「访问路径」列下面加一行注释说明
const pageLabel = (path) => {
  if (!path) return ''
  const p = String(path).split('?')[0]
  const map = {
    '/api/v1/site':           '站点信息（首页）',
    '/api/v1/home':           '首页聚合数据',
    '/api/v1/articles':       '文章列表',
    '/api/v1/about':          '关于页',
    '/api/v1/pages/about':    '关于页（页面管理已下架，历史路径）',
    '/api/v1/pages/guestbook':'留言板（历史路径）',
    '/api/v1/tags':           '标签列表',
    '/api/v1/comments/guestbook': '留言板',
    '/api/v1/friend-links':   '友情链接',
    '/api/v1/projects':       '项目集',
    '/api/v1/tools':          '工具集'
  }
  if (map[p]) return map[p]
  // 详情类 /:id 或 /slug
  if (/^\/api\/v1\/articles\/\d+/.test(p)) return '文章详情'
  if (/^\/api\/v1\/tags\/\d+/.test(p) || /^\/api\/v1\/tags\/[a-z0-9-]+/i.test(p)) return '标签详情'
  if (/^\/api\/v1\/projects\/\d+/.test(p)) return '项目详情'
  if (/^\/api\/v1\/tools\/\d+/.test(p)) return '工具详情'
  if (/^\/api\/v1\/categories\/\d+/.test(p)) return '分类详情'
  // 兜底
  if (p.startsWith('/api/v1/')) return '后端接口'
  if (p === '/' || p === '/home') return '前端首页'
  return ''
}

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d)) return t
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const loadStats = async () => {
  try {
    const params = {}
    if (date.value) params.date = date.value
    const resp = await adminStatsToday(params)
    Object.assign(stats, resp.data || {})
    lastRefreshedAt.value = formatTime(new Date())
  } catch (e) {
    ElMessage.error('今日统计加载失败')
  }
}

const reloadDetail = async () => {
  groupPage.value = 1
  expandedGroups.value = new Set()
  await loadDetail()
}

const loadDetail = async () => {
  detailLoading.value = true
  try {
    const params = { page: detailPage.value, size: detailFetchSize }
    if (dateRange.value && dateRange.value.length === 2) {
      params.start = dateRange.value[0]
      params.end = dateRange.value[1]
    }
    if (filters.ip) params.ip = filters.ip
    if (filters.deviceType) params.deviceType = filters.deviceType
    if (filters.os) params.os = filters.os
    if (filters.browser) params.browser = filters.browser
    if (filters.province) params.province = filters.province
    const resp = await adminStatsLogs(params)
    const data = resp.data || {}
    detailList.value = data.records || []
    detailTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('访问明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

const loadAll = () => {
  loadStats()
  loadDetail()
}

const goToday = () => {
  date.value = ''
  dateRange.value = null
  loadAll()
}

const onAutoRefreshChange = (v) => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (v) {
    refreshTimer = setInterval(loadStats, 5 * 60 * 1000)
  }
}

onMounted(async () => {
  await nextTick()
  deviceChart = initChart(deviceChartRef.value, buildPieData(stats.byDevice))
  osChart = initChart(osChartRef.value, buildPieData(stats.byOs))
  browserChart = initChart(browserChartRef.value, buildPieData(stats.byBrowser))
  provinceChart = initChart(provinceChartRef.value, buildPieData(stats.byProvince, formatProvince))
  window.addEventListener('resize', onResize)
  loadAll()
  onAutoRefreshChange(autoRefresh.value)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', onResize)
  if (resizeTimer) clearTimeout(resizeTimer)
  chartObservers.forEach((ro) => ro.disconnect())
  chartObservers.length = 0
  deviceChart?.dispose()
  osChart?.dispose()
  browserChart?.dispose()
  provinceChart?.dispose()
})
</script>

<style scoped lang="scss">
.stats-page { display: flex; flex-direction: column; gap: 16px; }
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* 顶部 KPI 卡 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin: 8px 0 16px;
}
.kpi-card {
  background: var(--c-paper);
  border: 1px solid var(--c-line-soft);
  border-radius: var(--radius);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease;
}
.kpi-card:hover { transform: translateY(-2px); }
.kpi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 4px;
  height: 100%;
  background: var(--c-autumn-500);
}
.kpi-pv::before    { background: linear-gradient(180deg, #ff7e5f, #feb47b); }
.kpi-uv::before    { background: linear-gradient(180deg, #43cea2, #185a9d); }
.kpi-peak::before  { background: linear-gradient(180deg, #ff9966, #ff5e62); }
.kpi-refresh::before { background: linear-gradient(180deg, #614385, #516395); }
.kpi-label { font-size: 13px; color: var(--c-ink-soft); }
.kpi-value { font-size: 30px; font-weight: 700; line-height: 1.1; color: var(--c-ink); font-variant-numeric: tabular-nums; }
.kpi-sub { font-size: 12px; color: var(--c-ink-soft); }

/* 图表行 */
.chart-row { margin-bottom: 14px; }
.chart-card { height: 100%; }

/* 分时柱状图 */
.hour-chart {
  display: flex;
  align-items: flex-end;
  height: 180px;
  gap: 4px;
  padding: 12px 4px 4px;
}
.hour-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}
.hour-bar {
  width: 100%;
  max-width: 28px;
  background: linear-gradient(180deg, var(--c-autumn-400, #e8a87c), var(--c-autumn-600, #a85d3a));
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 2px;
  transition: height 0.3s ease;
  min-height: 2px;
}
.hour-num {
  font-size: 12px;
  color: #fff;
  font-variant-numeric: tabular-nums;
  transform: scale(0.85);
}
.hour-label {
  font-size: 12px;
  color: var(--c-ink-soft);
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

/* 饼图容器 */
/* 饼图容器：wrapper 给占位文字 absolute 定位提供父级；echarts div 维持原高 */
.pie-chart-wrap { position: relative; width: 100%; }
.pie-chart { width: 100%; height: 280px; }
/* 空态占位：绝对覆盖在 echarts 之上，白底 + 主色字，绝对可见 */
.pie-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: var(--c-ink);
  background: #ffffff;
  pointer-events: none;
}

/* 访客明细 */
.detail-card { margin-top: 8px; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.toolbar-spacer { flex: 1; }
.table-scroll { overflow-x: auto; -webkit-overflow-scrolling: touch; }
.text-soft { color: var(--c-ink-soft); }

/* ── 分组折叠列表 ── */
.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px;
  color: var(--c-ink-soft);
  font-size: 14px;
}
.log-group {
  border-bottom: 1px solid var(--c-line-soft);
}
.log-group:last-child { border-bottom: none; }
.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;            /* 统一行高：与全局 admin 表格 60px 约定一致 */
  padding: 0 16px;         /* 高度由 height 决定，padding 仅控左右 */
  cursor: pointer;
  user-select: none;
  transition: background-color .15s ease;
  border-radius: var(--radius-sm, 6px);
}
.group-header:hover {
  background-color: rgba(56, 189, 248, 0.06);
}
.group-header.is-expanded {
  background-color: rgba(56, 189, 248, 0.04);
}
.gh-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  transition: transform .2s ease;
  color: var(--c-ink-soft);
  font-size: 13px;
}
.is-expanded .gh-toggle {
  transform: rotate(90deg);
  color: var(--c-botany-500);
}
.gh-ip { min-width: 130px; }
.gh-province {
  width: 80px;
  flex-shrink: 0;
  white-space: nowrap;            /* 长地区名（如「境外（United States）」）单行省略，不再撑高整行 */
  overflow: hidden;
  text-overflow: ellipsis;
}
.gh-device { width: 70px; flex-shrink: 0; }
.gh-os { width: 90px; flex-shrink: 0; }
.gh-browser { width: 90px; flex-shrink: 0; }
.gh-count { flex-shrink: 0; }
.gh-time-range {
  margin-left: auto;
  font-size: 12px;
  color: var(--c-ink-soft);
  white-space: nowrap;
  font-family: ui-monospace, monospace;
}

/* 展开明细 */
.group-detail {
  padding: 4px 0 10px 36px;
}
.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 44px;            /* 统一明细行高 */
  padding: 0 12px;         /* 高度由 height 决定 */
  border-radius: 4px;
  transition: background-color .1s ease;
}
.detail-row:hover {
  background-color: rgba(0, 0, 0, 0.02);
}
.dr-index {
  width: 28px;
  text-align: center;
  font-size: 12px;
  color: var(--c-ink-soft);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}
.dr-path {
  min-width: 200px;
  flex: 1;
}
.dr-time {
  font-size: 12px;
  color: var(--c-ink-soft);
  white-space: nowrap;
  font-family: ui-monospace, monospace;
  flex-shrink: 0;
}

/* 展开动画 */
.group-expand-enter-active,
.group-expand-leave-active {
  transition: all .2s ease;
  overflow: hidden;
}
.group-expand-enter-from,
.group-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.group-expand-enter-to,
.group-expand-leave-from {
  opacity: 1;
  max-height: 600px;
}
.ip-text {
  background: var(--c-paper-soft);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-family: ui-monospace, monospace;
}
.path-text {
  background: var(--c-paper-soft);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--c-botany-600);
  word-break: break-all;
}
.path-page {
  font-size: 12px;
  color: var(--c-ink-soft);
  padding: 0 4px;
  letter-spacing: 0.02em;
}
.dr-path {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
@media (max-width: 600px) {
  .kpi-value { font-size: 24px; }
}
</style>