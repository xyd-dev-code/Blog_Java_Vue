<template>
  <div class="stats-page">
    <el-card>
      <template #header>
        <div class="header-bar">
          <span>
            {{ wx('访问统计') }}
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

      <!-- 分时访问趋势与操作系统、省份分布 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
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
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>操作系统</span></template>
            <div class="pie-chart-wrap">
              <div ref="osChartRef" class="pie-chart" />
              <div v-if="!hasOsData" class="pie-empty">{{ wx('暂无访问数据') }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span>访客省份</span></template>
            <div class="pie-chart-wrap">
              <div ref="provinceChartRef" class="pie-chart" />
              <div v-if="!hasProvinceData" class="pie-empty">{{ wx('暂无访问数据') }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 访客明细 -->
    <el-card class="detail-card">
      <template #header>
        <div class="header-bar detail-heading">
          <div>
            <h2>访客明细</h2>
            <p>按 IP 与日期分组，展开查看访问路径</p>
          </div>
          <div class="detail-summary" aria-live="polite">
            <span><strong>{{ detailTotal.toLocaleString() }}</strong> 条记录</span>
            <span><strong>{{ groupTotal.toLocaleString() }}</strong> 个已加载分组</span>
            <el-button :icon="Refresh" :loading="detailLoading" @click="loadAll">刷新</el-button>
          </div>
        </div>
      </template>

      <form class="toolbar" @submit.prevent="reloadDetail">
        <div class="filter-field filter-date">
          <label id="visit-period-label">访问时间</label>
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm"
            aria-labelledby="visit-period-label"
            @change="reloadDetail"
          />
        </div>
        <div class="filter-field filter-ip">
          <label for="visitor-ip">IP 地址</label>
          <el-input id="visitor-ip" v-model="filters.ip" :prefix-icon="Search" placeholder="输入 IP，支持模糊匹配" clearable @clear="reloadDetail" />
        </div>
        <div class="filter-field">
          <label id="visit-device-label">设备</label>
          <el-select v-model="filters.deviceType" aria-labelledby="visit-device-label" placeholder="全部设备" clearable @change="reloadDetail">
            <el-option v-for="d in ['PC','Mobile','Tablet','Other']" :key="d" :label="d" :value="d" />
          </el-select>
        </div>
        <div class="filter-field">
          <label id="visit-os-label">操作系统</label>
          <el-select v-model="filters.os" aria-labelledby="visit-os-label" placeholder="全部系统" clearable @change="reloadDetail">
            <el-option v-for="o in ['Windows','macOS','iOS','Android','Linux','Other']" :key="o" :label="o" :value="o" />
          </el-select>
        </div>
        <div class="filter-field">
          <label id="visit-browser-label">浏览器</label>
          <el-select v-model="filters.browser" aria-labelledby="visit-browser-label" placeholder="全部浏览器" clearable @change="reloadDetail">
            <el-option v-for="b in ['Chrome','Edge','Safari','Firefox','QQ','WeChat','UC','Other']" :key="b" :label="b" :value="b" />
          </el-select>
        </div>
        <div class="filter-field">
          <label id="visit-province-label">地区</label>
          <el-select v-model="filters.province" aria-labelledby="visit-province-label" placeholder="全部地区" clearable filterable @change="reloadDetail">
            <el-option v-for="p in provinceOptions" :key="p" :label="formatProvince(p)" :value="p" />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button native-type="submit" type="primary" :icon="Search" :loading="detailLoading">查询</el-button>
          <el-button :disabled="!hasFilters" @click="resetFilters">重置</el-button>
        </div>
      </form>

      <div class="detail-list-meta">
        <span>{{ detailLoading ? '正在加载访问记录…' : '按最近访问排序' }}<template v-if="detailTotal > detailList.length"> · 当前仅加载最近 {{ detailList.length }} 条记录，缩小筛选范围可查看更早记录</template></span>
        <el-button text :disabled="!groupedLogs.length" @click="toggleCurrentPage">{{ currentPageExpanded ? '收起本页' : '展开本页' }}</el-button>
      </div>

      <div class="table-scroll" :aria-busy="detailLoading">
        <div v-if="detailError" class="empty-tip" role="alert">
          <span>访问明细加载失败，请稍后重试</span>
          <el-button @click="loadDetail" :loading="detailLoading">重新加载</el-button>
        </div>
        <el-empty v-else-if="!detailList.length && !detailLoading" :image-size="72" :description="hasFilters ? '没有符合筛选条件的访问记录' : '暂无访问记录'">
          <el-button v-if="hasFilters" @click="resetFilters">清空筛选</el-button>
        </el-empty>
        <div v-if="groupedLogs.length" class="group-columns" aria-hidden="true">
          <span></span><span>访客 IP / 地区</span><span>访问环境</span><span>访问次数</span><span>最近 / 首次访问</span><span></span>
        </div>

        <!-- 按 IP + 日期 分组折叠 -->
        <div v-for="g in groupedLogs" :key="g.key" class="log-group">
          <!-- 汇总行（点击展开/收起） -->
          <button
            type="button"
            class="group-header"
            data-ink-feedback="off"
            :class="{ 'is-expanded': expandedGroups.has(g.key) }"
            :aria-expanded="expandedGroups.has(g.key)"
            :aria-controls="`visit-${g.key}`"
            @click="toggleGroup(g.key)"
          >
            <span class="gh-toggle">
              <el-icon><ArrowRight /></el-icon>
            </span>
            <span class="gh-visitor">
              <code class="ip-text">{{ g.ip || '未知 IP' }}</code>
              <span class="gh-province">{{ formatProvince(g.location || g.province) }}</span>
            </span>
            <span class="gh-environment">
              <span><el-tag size="small" :type="deviceTag(g.deviceType)">{{ g.deviceType || '未知设备' }}</el-tag><span>{{ g.os || '未知系统' }}</span></span>
              <span class="gh-browser">{{ g.browser || '未知浏览器' }}</span>
            </span>
            <span class="gh-count">
              <strong>{{ g.rows.length.toLocaleString() }}</strong><span> 次访问</span>
            </span>
            <span class="gh-time-range"><span><span class="time-label">最近</span>{{ g.lastTime }}</span><span><span class="time-label">首次</span>{{ g.firstTime }}</span></span>
            <span class="gh-action">{{ expandedGroups.has(g.key) ? '收起' : '详情' }}</span>
          </button>

          <!-- 展开明细 -->
            <div v-if="expandedGroups.has(g.key)" :id="`visit-${g.key}`" class="group-detail">
              <div class="visit-history-heading">
                <span>访问记录 <span class="history-date">{{ g.lastTime.slice(0, 10) }}</span></span>
                <span>最新在前 · {{ g.rows.length }} 条已加载记录</span>
              </div>
              <div class="detail-row detail-column-labels"><span class="dr-index">序号</span><span class="dr-path">访问内容 / 请求路径</span><span class="dr-time">时间</span></div>
              <div v-for="(row, ri) in visibleGroupRows(g)" :key="row.id" class="detail-row">
                <span class="dr-index">{{ (rowPage(g) - 1) * rowPageSize + ri + 1 }}</span>
                <div class="dr-path">
                  <span class="path-page">{{ pageLabel(row.path) || '访问请求' }}</span>
                  <details v-if="readablePath(row.path).length > 90" class="path-disclosure">
                    <summary><code class="path-preview">{{ readablePath(row.path) }}</code><span class="path-disclosure-label">完整路径</span></summary>
                    <code class="path-text">{{ readablePath(row.path) }}</code>
                  </details>
                  <code v-else class="path-text">{{ readablePath(row.path) }}</code>
                </div>
                <time class="dr-time" :datetime="row.visitTime" :title="formatTime(row.visitTime)">{{ formatTime(row.visitTime).slice(11) || '—' }}</time>
              </div>
              <div class="history-footer">
                <span>显示 {{ (rowPage(g) - 1) * rowPageSize + 1 }}–{{ Math.min(rowPage(g) * rowPageSize, g.rows.length) }} / {{ g.rows.length }} 条</span>
                <div class="history-actions">
                  <template v-if="g.rows.length > rowPageSize">
                    <el-button :disabled="rowPage(g) === 1" :aria-label="`${g.ip} 访问记录上一页`" @click="changeRowPage(g, -1)">上一页</el-button>
                    <span class="history-page" aria-live="polite">{{ rowPage(g) }} / {{ Math.ceil(g.rows.length / rowPageSize) }}</span>
                    <el-button :disabled="rowPage(g) * rowPageSize >= g.rows.length" :aria-label="`${g.ip} 访问记录下一页`" @click="changeRowPage(g, 1)">下一页</el-button>
                  </template>
                  <el-button text @click="toggleGroup(g.key)">收起记录</el-button>
                </div>
              </div>
            </div>
        </div>
      </div>

      <div v-if="groupTotal" class="detail-footer">
        <span>第 {{ (groupPage - 1) * groupPageSize + 1 }}–{{ Math.min(groupPage * groupPageSize, groupTotal) }} 组，共 {{ groupTotal }} 组</span>
      <el-pagination
        v-if="groupTotal > groupPageSize"
        class="pager"
        background
        layout="prev, pager, next"
        :pager-count="5"
        :total="groupTotal"
        :page-size="groupPageSize"
        :current-page="groupPage"
        @current-change="changeGroupPage"
      />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useWuxiaCopy } from '@/composables/useWuxiaCopy'
const { wx } = useWuxiaCopy()

import { ref, reactive, onMounted, onUnmounted, computed, nextTick, watch } from 'vue'
import { Refresh, ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { adminStatsToday, adminStatsLogs } from '@/api/admin'
import { formatProvince } from '@/utils/visitRegion'
import { resolveThemeToken, resolveThemeTokens, subscribeThemeChange } from '@/utils/theme'
// 按需引入 ECharts，仅引入饼图与必要组件以控制体积
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const PIE_PALETTE_TOKENS = [
  '--c-botany-500', '--c-autumn-500', '--c-cyan-500', '--c-botany-700', '--c-orange-500',
  '--c-cyan-700', '--c-botany-800', '--c-autumn-700', '--c-cyan-300', '--c-lime-500',
]

const buildPieData = (map, nameFmt = (x) => x) => {
  if (!map) return []
  return Object.entries(map)
    .map(([name, value]) => ({ name: nameFmt(name || '未知'), value: Number(value) || 0 }))
    .filter((d) => d.value > 0)
    .sort((a, b) => b.value - a.value)
}

// 各饼图「是否有数据」标记——空时由模板层覆盖层显示「暂无访问数据」（不依赖 ECharts 渲染）
const hasOsData       = computed(() => Object.values(stats.byOs       || {}).some(v => Number(v) > 0))
const hasProvinceData = computed(() => Object.values(stats.byProvince || {}).some(v => Number(v) > 0))

const pieOption = (data) => {
  const hasData = Array.isArray(data) && data.length > 0
  // 空数据时返回空 option，不画任何东西；占位由模板层 .pie-empty 覆盖层负责（不依赖 ECharts title 渲染，更稳）
  if (!hasData) return {}
  return {
    tooltip: { trigger: 'item', formatter: ({ name, value, percent }) => `${name}<br/>${value} 次 (${percent}%)` },
    legend: { bottom: 0, type: 'scroll', textStyle: { fontSize: 12, color: resolveThemeToken('--c-ink-soft') } },
    color: resolveThemeTokens(PIE_PALETTE_TOKENS),
    series: [{
      name: '占比',
      type: 'pie',
      radius: '62%',
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 4, borderColor: resolveThemeToken('--c-paper'), borderWidth: 2 },
      label: {
        formatter: ({ name, percent }) => `${name}\n${percent}%`,
        fontSize: 12,
        color: resolveThemeToken('--c-ink')
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
  byOs: {},
  byProvince: {}
})

// ── 饼图（ECharts）──
const osChartRef = ref(null)
const provinceChartRef = ref(null)
let osChart, provinceChart
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
  osChart?.setOption(pieOption(buildPieData(stats.byOs)), true)
  provinceChart?.setOption(pieOption(buildPieData(stats.byProvince, formatProvince)), true)
  // 数据更新后兜底 resize，确保画布与容器同步
  osChart?.resize()
  provinceChart?.resize()
}
const resizeCharts = () => {
  osChart?.resize()
  provinceChart?.resize()
}
// resize 防抖：ECharts resize 涉及重新布局，连续 resize 合并到 150ms 后只执行一次
let resizeTimer = null
let unsubscribeTheme = null
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
const detailError = ref(false)
let detailRequestId = 0
const hasFilters = computed(() => Boolean(dateRange.value?.length || Object.values(filters).some(Boolean)))
const resetFilters = () => {
  dateRange.value = null
  Object.keys(filters).forEach(key => { filters[key] = '' })
  reloadDetail()
}

// ── 分组折叠 + 组级分页 ──
const expandedGroups = ref(new Set())
const rowPages = reactive({})
const rowPageSize = 8
const rowPage = (group) => Math.min(rowPages[group.key] || 1, Math.max(1, Math.ceil(group.rows.length / rowPageSize)))
const visibleGroupRows = (group) => group.rows.slice((rowPage(group) - 1) * rowPageSize, rowPage(group) * rowPageSize)
const changeRowPage = (group, delta) => {
  rowPages[group.key] = Math.min(Math.max(1, rowPage(group) + delta), Math.ceil(group.rows.length / rowPageSize))
}
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
        location: row.location,
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
    if (!g.firstTime || t < g.firstTime) g.firstTime = t
    if (!g.lastTime || t > g.lastTime) g.lastTime = t
  }
  for (const group of map.values()) {
    group.rows.sort((a, b) => String(b.visitTime || '').localeCompare(String(a.visitTime || '')))
  }
  return Array.from(map.values()).sort((a, b) =>
    a.lastTime > b.lastTime ? -1 : a.lastTime < b.lastTime ? 1 : 0
  )
})

/** 当前页的分组切片 */
const groupedLogs = computed(() => {
  const start = (groupPage.value - 1) * groupPageSize
  return allGroups.value.slice(start, start + groupPageSize)
})

/** 分组总数 */
const groupTotal = computed(() => allGroups.value.length)
const currentPageExpanded = computed(() => groupedLogs.value.length > 0 && groupedLogs.value.every(g => expandedGroups.value.has(g.key)))
const toggleCurrentPage = () => {
  const next = new Set(expandedGroups.value)
  const collapse = currentPageExpanded.value
  groupedLogs.value.forEach(g => collapse ? next.delete(g.key) : next.add(g.key))
  expandedGroups.value = next
}
const changeGroupPage = (page) => {
  groupPage.value = page
  expandedGroups.value = new Set()
}

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

// 后端接口路径 → 页面名映射；用于访客明细「访问路径」列下面加一行注释说明
const pageLabel = (path) => {
  if (!path) return ''
  const p = String(path).split('?')[0]
  const map = {
    '/api/v1/site':           '站点信息（首页）',
    '/api/v1/home':           '首页聚合数据',
    '/api/v1/articles':       '文章列表',
    '/api/v1/articles/featured': '精选文章',
    '/api/v1/articles/search': '文章搜索',
    '/api/v1/articles/archive': '文章归档',
    '/api/v1/articles/tag-cloud': '文章标签云',
    '/api/v1/about':          '关于页',
    '/api/v1/pages/about':    '关于页（页面管理已下架，历史路径）',
    '/api/v1/pages/guestbook':'留言板（历史路径）',
    '/api/v1/tags':           '标签列表',
    '/api/v1/comments/guestbook': '留言板',
    '/api/v1/comments/captcha': '评论验证码',
    '/api/v1/friend-links':   '友情链接',
    '/api/v1/projects':       '项目集',
    '/api/v1/tools':          '工具集'
  }
  if (map[p]) return map[p]
  if (p.startsWith('/api/v1/articles/archive/')) return '按月归档'
  if (p.startsWith('/api/v1/articles/by-tag/')) return '按标签浏览文章'
  if (p.startsWith('/api/v1/articles/by-category/')) return '按分类浏览文章'
  // 详情类 /:id 或 /slug
  if (/^\/api\/v1\/articles\/[^/]+\/?$/.test(p)) return '文章详情'
  if (/^\/api\/v1\/tags\/\d+/.test(p) || /^\/api\/v1\/tags\/[a-z0-9-]+/i.test(p)) return '标签详情'
  if (/^\/api\/v1\/projects\/\d+/.test(p)) return '项目详情'
  if (/^\/api\/v1\/tools\/\d+/.test(p)) return '工具详情'
  if (/^\/api\/v1\/categories\/\d+/.test(p)) return '分类详情'
  // 兜底
  if (p.startsWith('/api/v1/')) return '后端接口'
  if (p === '/' || p === '/home') return '前端首页'
  return ''
}

// 仅解码展示文本，不导航或请求日志中的地址；非法百分号编码保留原样。
const readablePath = (path) => {
  if (!path) return '未记录路径'
  try { return decodeURI(String(path)) } catch { return String(path) }
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
  Object.keys(rowPages).forEach(key => { delete rowPages[key] })
  await loadDetail()
}

const loadDetail = async () => {
  const requestId = ++detailRequestId
  detailLoading.value = true
  detailError.value = false
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
    if (requestId !== detailRequestId) return
    const data = resp.data || {}
    detailList.value = data.records || []
    detailTotal.value = data.total || 0
    groupPage.value = Math.min(groupPage.value, Math.max(1, Math.ceil(groupTotal.value / groupPageSize)))
    const keys = new Set(allGroups.value.map(g => g.key))
    Object.keys(rowPages).forEach(key => { if (!keys.has(key)) delete rowPages[key] })
    expandedGroups.value = new Set([...expandedGroups.value].filter(key => keys.has(key)))
  } catch (e) {
    if (requestId !== detailRequestId) return
    detailError.value = true
    ElMessage.error('访问明细加载失败')
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false
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
  osChart = initChart(osChartRef.value, buildPieData(stats.byOs))
  provinceChart = initChart(provinceChartRef.value, buildPieData(stats.byProvince, formatProvince))
  window.addEventListener('resize', onResize)
  unsubscribeTheme = subscribeThemeChange(() => nextTick(renderCharts))
  loadAll()
  onAutoRefreshChange(autoRefresh.value)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', onResize)
  unsubscribeTheme?.()
  if (resizeTimer) clearTimeout(resizeTimer)
  chartObservers.forEach((ro) => ro.disconnect())
  chartObservers.length = 0
  osChart?.dispose()
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
.kpi-pv::before    { background: linear-gradient(180deg, var(--c-chart-1), var(--c-chart-2)); }
.kpi-uv::before    { background: linear-gradient(180deg, var(--c-chart-3), var(--c-chart-4)); }
.kpi-peak::before  { background: linear-gradient(180deg, var(--c-chart-5), var(--c-chart-6)); }
.kpi-refresh::before { background: linear-gradient(180deg, var(--c-chart-7), var(--c-chart-8)); }
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
  background: linear-gradient(180deg, var(--c-autumn-400, var(--c-chart-9)), var(--c-autumn-600, var(--c-chart-10)));
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
  color: var(--theme-on-primary);
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
  background: var(--c-paper);
  pointer-events: none;
}

/* 访客明细：复用当前主题色，局部容器负责响应式布局。 */
.detail-card { margin-top: 8px; container-type: inline-size; }
.detail-heading h2 { margin: 0; font-size: 18px; font-weight: 600; color: var(--c-ink); }
.detail-heading p { margin: 6px 0 0; font-size: 13px; color: var(--c-ink-soft); font-weight: 400; }
.detail-summary { display: flex; align-items: center; flex-wrap: wrap; gap: 20px; font-size: 13px; color: var(--c-ink-soft); font-weight: 400; }
.detail-summary strong { color: var(--c-ink); font-size: 18px; font-variant-numeric: tabular-nums; font-weight: 600; margin-right: 4px; }
.toolbar { display: grid; grid-template-columns: minmax(330px, 2.4fr) minmax(190px, 1.4fr) repeat(4, minmax(110px, 1fr)) max-content; align-items: end; gap: 12px; padding: 20px; overflow-x: auto; background: var(--c-paper-soft); border: 1px solid var(--c-line-soft); border-radius: 8px; }
.filter-field { min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.filter-field label { font-size: 13px; font-weight: 500; color: var(--c-ink); }
.filter-field :deep(.el-date-editor) { width: 100%; min-width: 0; box-sizing: border-box; }
.filter-field :deep(.el-input__wrapper), .filter-field :deep(.el-select__wrapper) { min-height: 40px; box-sizing: border-box; }
.filter-actions { display: flex; align-items: center; flex-wrap: nowrap; gap: 10px; white-space: nowrap; }
.filter-actions .el-button { margin: 0; min-height: 40px; min-width: 80px; }
.detail-list-meta { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 14px 0; font-size: 13px; color: var(--c-ink-soft); }
.table-scroll { border: 1px solid var(--c-line-soft); border-radius: 8px; overflow: hidden; background: var(--c-paper); }
.table-scroll[aria-busy="true"] .log-group { opacity: .5; pointer-events: none; }
.empty-tip { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; min-height: 180px; color: var(--c-ink-soft); font-size: 14px; }
.group-columns, .group-header { display: grid; grid-template-columns: 20px minmax(150px, 1.3fr) minmax(140px, 1fr) minmax(90px, .6fr) minmax(220px, 1.2fr) 36px; align-items: center; gap: 16px; padding: 0 18px; }
.group-columns { min-height: 42px; font-size: 12px; font-weight: 500; color: var(--c-ink-soft); background: var(--c-paper-soft); border-bottom: 1px solid var(--c-line-soft); }
.log-group { border-bottom: 1px solid var(--c-line-soft); }
.log-group:last-child { border-bottom: none; }
.group-header { width: 100%; min-height: 88px; padding-top: 16px; padding-bottom: 16px; border: 0; border-radius: 0; background: transparent; text-align: left; color: var(--c-ink); font: inherit; font-size: 14px; cursor: pointer; transition: background-color .15s ease; }
.group-header:hover, .group-header.is-expanded { background: rgba(var(--theme-primary-rgb), .05); }
.group-header:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: -3px; }
.gh-toggle { display: flex; align-items: center; justify-content: center; color: var(--c-ink-soft); transition: transform .2s ease; }
.is-expanded .gh-toggle { transform: rotate(90deg); color: var(--el-color-primary); }
.gh-visitor, .gh-environment, .gh-time-range { display: flex; flex-direction: column; gap: 8px; min-width: 0; }
.ip-text { font-family: inherit; font-size: 14px; font-weight: 600; overflow-wrap: anywhere; }
.gh-province { color: var(--c-ink-soft); font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }
.gh-environment > span:first-child { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.gh-browser { color: var(--c-ink-soft); font-size: 13px; }
.gh-count { font-size: 12px; color: var(--c-ink-soft); }
.gh-count strong { font-size: 16px; font-weight: 600; color: var(--c-ink); font-variant-numeric: tabular-nums; }
.gh-time-range { font-size: 13px; font-variant-numeric: tabular-nums; white-space: nowrap; }
.gh-time-range > span:last-child { color: var(--c-ink-soft); }
.time-label { font-size: 12px; color: var(--c-ink-soft); margin-right: 10px; }
.gh-action { font-size: 12px; color: var(--el-color-primary); }
.group-detail { padding: 0 18px 16px 54px; background: rgba(var(--theme-primary-rgb), .025); }
.visit-history-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; padding: 18px 12px 14px; font-size: 14px; font-weight: 600; color: var(--c-ink); }
.visit-history-heading > span:last-child, .history-date { color: var(--c-ink-soft); font-size: 12px; font-weight: 400; }
.history-date { margin-left: 12px; font-variant-numeric: tabular-nums; }
.detail-row { display: flex; align-items: center; gap: 16px; min-height: 60px; padding: 10px 12px; border-bottom: 1px solid var(--c-line-soft); }
.detail-row:last-child { border-bottom: 0; }
.detail-column-labels { min-height: 36px; font-size: 12px; color: var(--c-ink-soft); padding-top: 6px; padding-bottom: 6px; }
.dr-index { width: 32px; flex-shrink: 0; text-align: center; font-size: 12px; color: var(--c-ink-soft); font-variant-numeric: tabular-nums; }
.dr-path { display: flex; flex-direction: column; gap: 4px; min-width: 0; flex: 1; }
.path-text, .path-preview { font-family: inherit; font-size: 13px; color: var(--c-ink-soft); overflow-wrap: anywhere; line-height: 1.6; }
.path-page { font-size: 14px; font-weight: 500; color: var(--c-ink); line-height: 1.5; }
.path-disclosure { min-width: 0; }
.path-disclosure summary { display: flex; align-items: center; gap: 12px; min-height: 32px; cursor: pointer; border-radius: 4px; list-style: none; }
.path-disclosure summary::-webkit-details-marker { display: none; }
.path-disclosure summary:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.path-preview { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }
.path-disclosure-label { color: var(--el-color-primary); font-size: 12px; flex-shrink: 0; }
.path-disclosure[open] .path-preview { display: none; }
.path-disclosure[open] > .path-text { display: block; padding: 10px 12px; border: 1px solid var(--c-line-soft); border-radius: 4px; background: var(--c-paper); }
.dr-time { font-size: 13px; color: var(--c-ink-soft); white-space: nowrap; font-variant-numeric: tabular-nums; flex-shrink: 0; }
.history-footer { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; padding: 16px 12px 0; font-size: 12px; color: var(--c-ink-soft); }
.history-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.history-actions .el-button { margin: 0; min-height: 36px; }
.history-page { font-variant-numeric: tabular-nums; }
.detail-footer { margin-top: 18px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; font-size: 13px; color: var(--c-ink-soft); }
.pager { margin: 0; }
@container (max-width: 900px) {
  .group-columns, .group-header { grid-template-columns: 16px minmax(130px, 1.2fr) minmax(100px, 1fr) 70px minmax(180px, 1.4fr); gap: 12px; padding-left: 12px; padding-right: 12px; }
  .gh-action, .group-columns > span:last-child { display: none; }
  .gh-time-range { font-size: 12px; }
  .time-label { margin-right: 6px; }
}
@container (max-width: 680px) {
  .toolbar { padding: 14px; }
  .detail-summary { gap: 14px; }
  .detail-list-meta { align-items: flex-start; }
  .detail-list-meta > span { line-height: 1.6; }
  .group-columns { display: none; }
  .group-header { grid-template-columns: 16px minmax(0, 1fr) minmax(0, 1fr); gap: 14px 10px; }
  .gh-toggle { grid-column: 1; grid-row: 1; }
  .gh-visitor { grid-column: 2; grid-row: 1; }
  .gh-environment { grid-column: 3; grid-row: 1; }
  .gh-count { grid-column: 2; grid-row: 2; }
  .gh-time-range { grid-column: 2 / -1; grid-row: 3; flex-direction: row; flex-wrap: wrap; gap: 8px 18px; }
  .gh-action { display: block; grid-column: 3; grid-row: 2; text-align: right; }
  .group-detail { padding: 0 12px 12px 26px; }
  .detail-row { flex-wrap: wrap; gap: 6px 10px; }
  .dr-index { width: 24px; }
  .dr-path { flex-basis: calc(100% - 34px); }
  .dr-time { margin-left: 34px; }
  .detail-column-labels .dr-time { display: none; }
  .history-footer { padding-left: 0; padding-right: 0; }
  .history-actions .el-button { min-height: 44px; }
  .path-disclosure summary { min-height: 44px; }
}
.group-header, .gh-toggle { transition: none; }
@media (max-width: 600px) { .kpi-value { font-size: 24px; } }
</style>
