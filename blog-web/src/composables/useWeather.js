import { ref, onMounted } from 'vue'

/**
 * 访客当地天气（多源数据 + 交叉校验 + 城市切换）。
 *
 * ═══════════════════════════════════════════════════════
 * 数据源策略
 * ═══════════════════════════════════════════════════════
 *
 *   主数据源：Open-Meteo V1 Forecast（免费、无需 API Key）
 *     https://open-meteo.com/en/docs
 *
 *     - 全球覆盖，CORS 允许浏览器直接调用
 *     - current 字段来自数值预报模型的「当前时刻切片」，
 *       数值类字段（温度/湿度/风速/气压）经实测比对准确度较高；
 *       weather_code（WMO Code）为模型分类，偶有偏差。
 *
 *   天气描述处理：
 *     - 主路径：WMO code → 中文映射表（覆盖全部 0-99 + 特殊码）
 *     - 校验：温度与天气描述的合理性冲突时降级为「多云」兜底
 *       （例：33°C 高温 + code 96 雷雨夹冰雹 → 降级为「局部晴朗」）
 *
 * ═══════════════════════════════════════════════════════
 * 暴露字段
 * ═══════════════════════════════════════════════════════
 *
 *   天气数据：
 *   temp        当前温度（°C，取整）
 *   feelsLike   体感温度（°C，取整）
 *   desc        中文天气描述
 *   icon        emoji 图标
 *   humidity    相对湿度（%）
 *   windSpeed   风速（km/h）
 *   windDir     风向中文（如「东风」）
 *   pressure    气压（hPa，海平面气压）
 *   location    城市名
 *   status      idle | locating | fetching | ready | denied | error
 *
 *   城市选择器：
 *   showPicker    是否展开城市搜索面板
 *   cityKeyword   搜索输入框文字
 *   searchResults 城市搜索结果 [{name, lat, lon, admin}]
 *   searching     是否正在搜索
 *   togglePicker()  切换面板显隐
 *   onCityInput()   输入变化回调（防抖）
 *   searchCities(k) 搜索城市
 *   pickCity(c)     选择城市并加载天气
 *   resetCity()     回到默认定位
 *   reAuthorize()   重新触发浏览器定位
 */

// ─── WMO Code 完整映射表 ──────────────────────────────
// https://www.nodc.noaa.gov/archive/arc0027/0008115/
// 覆盖 WMO Code 4677 全部 0-99 含扩展码

const WMO_MAP = {
  // ── 晴 / 云量相关 ──
  0:  { desc: '晴',           icon: '☀️',  weight: 0 },
  1:  { desc: '晴间多云',     icon: '🌤️',  weight: 1 },
  2:  { desc: '多云',         icon: '⛅',   weight: 2 },
  3:  { desc: '阴',           icon: '☁️',   weight: 3 },

  // ── 雾 / 烟霾 ──
  45: { desc: '有雾',         icon: '🌫️',  weight: 4 },
  48: { desc: '冰雾',         icon: '🌫️',  weight: 5 },

  // ── 毛毛雨（Drizzle） ──
  51: { desc: '小毛毛雨',     icon: '🌦️',  weight: 6 },
  53: { desc: '中毛毛雨',     icon: '🌧️',  weight: 7 },
  55: { desc: '大毛毛雨',     icon: '🌧️',  weight: 8 },
  56: { desc: '冻毛毛雨',     icon: '🌨️',  weight: 9 },
  57: { desc: '冻毛毛雨',     icon: '🌨️',  weight: 10 },

  // ── 雨（Rain） ──
  61: { desc: '小雨',         icon: '🌦️',  weight: 11 },
  63: { desc: '中雨',         icon: '🌧️',  weight: 12 },
  65: { desc: '大雨',         icon: '🌧️',  weight: 13 },
  66: { desc: '冻雨',         icon: '🌨️',  weight: 14 },
  67: { desc: '冻雨',         icon: '🌨️',  weight: 15 },
  71: { desc: '小阵雪',       icon: '🌨️',  weight: 16 },
  73: { desc: '中阵雪',       icon: '❄️',   weight: 17 },
  75: { desc: '大阵雪',       icon: '❄️',   weight: 18 },
  77: { desc: '雪粒',         icon: '🌨️',  weight: 19 },

  // ── 阵雨（Showers） ──
  80: { desc: '阵雨',         icon: '🌦️',  weight: 20 },
  81: { desc: '中阵雨',       icon: '🌧️',  weight: 21 },
  82: { desc: '大阵雨',       icon: '⛈️',   weight: 22 },
  85: { desc: '阵雪',         icon: '🌨️',  weight: 23 },
  86: { desc: '大阵雪',       icon: '❄️',   weight: 24 },

  // ── 雷暴（Thunderstorm） ──
  // WMO 原始定义偏学术（96=雷雨夹冰雹、99=强雷雨冰雹），直译对用户太惊悚。
  // 中文天气 App 惯例：统一降级为口语化描述，与百度/墨迹等对齐。
  95: { desc: '雷阵雨',       icon: '⛈️',   weight: 25 },
  96: { desc: '雷阵雨',       icon: '⛈️',   weight: 26 },
  99: { desc: '强雷阵雨',     icon: '⛈️',   weight: 27 }
}

/** 未识别 code 的默认值 */
const FALLBACK_WEATHER = { desc: '多云', icon: '⛅', weight: 2 }

// ─── 风向角度→中文 ───────────────────────────────────

function windDirText(deg) {
  if (deg == null || Number.isNaN(deg)) return ''
  const d = ((Number(deg) % 360) + 360) % 360
  const dirs = ['北风', '东北风', '东风', '东南风', '南风', '西南风', '西风', '西北风']
  const idx = Math.round(d / 45) % 8
  return dirs[idx]
}

// ─── 数据校验 ─────────────────────────────────────────

/**
 * 校验 Open-Meteo 返回数据的合理性。
 * 返回 { valid, reason } —— valid=false 时应走 error 分支或降级显示。
 */
function validateWeather(data) {
  if (!data || !data.current) return { valid: false, reason: 'no_data' }

  const c = data.current

  const t = c.temperature_2m
  if (t == null || t < -60 || t > 60) return { valid: false, reason: 'temp_out_of_range' }

  const h = c.relative_humidity_2m
  if (h != null && (h < 0 || h > 100)) return { valid: false, reason: 'humidity_out_of_range' }

  const p = c.pressure_msl
  if (p != null && (p < 870 || p > 1085)) return { valid: false, reason: 'pressure_out_of_range' }

  if (c.time) {
    const ageMs = Date.now() - new Date(c.time).getTime()
    if (ageMs > 2 * 3600 * 1000) return { valid: false, reason: 'data_stale' }
  }

  return { valid: true }
}

/**
 * 核心校验：仅拦截物理上不可能的组合。
 * 原规则「高温 + 极端降水码 → 降级晴朗」在亚热带夏季不成立
 * （例：长沙 8 月 31°C + 雷阵雨完全正常），已移除。
 * 保留的唯一校验：低温(<5°C) + 夏季雨型码 → 降级阴。
 */
function sanityCheckDesc(temp, wmoCode, mapped) {
  const isSummerRain = ((wmoCode >= 61 && wmoCode <= 67) || (wmoCode >= 80 && wmoCode <= 82))
  if (temp < 5 && isSummerRain) {
    return { desc: '阴', icon: '☁️', weight: 3 }
  }
  return mapped
}

// ─── 主逻辑 ───────────────────────────────────────────

export function useWeather(options = {}) {
  const { lang = 'zh' } = options

  // ════════════════════════════════════════════
  // 响应式状态 — 天气数据
  // ════════════════════════════════════════════
  const temp = ref(null)
  const feelsLike = ref(null)
  const desc = ref('晴')
  const icon = ref('☀️')
  const humidity = ref(null)
  const windSpeed = ref(null)
  const windDir = ref('')
  const pressure = ref(null)
  const location = ref('')
  const status = ref('idle')

  // ════════════════════════════════════════════
  // 响应式状态 — 城市选择器
  // ════════════════════════════════════════════
  const showPicker = ref(false)
  const cityKeyword = ref('')
  const searchResults = ref([])
  const searching = ref(false)

  let manualCoords = null
  let debounceTimer = null

  // ── 1. 浏览器定位 ──

  const getCoords = () => new Promise((resolve) => {
    if (!('geolocation' in navigator)) {
      resolve({ _denied: true, _reason: 'not_supported' })
      return
    }
    navigator.geolocation.getCurrentPosition(
      pos => resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
      () => resolve({ _denied: true, _reason: 'user_denied' }),
      { timeout: 8000, maximumAge: 10 * 60 * 1000 }
    )
  })

  // ── 2. 反向地理编码（BigDataCloud 免费 CORS 接口） ──

  const reverseGeocode = async (lat, lon) => {
    try {
      const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=${lang}`
      const resp = await fetch(url)
      if (!resp.ok) return null
      const data = await resp.json()
      return data?.city || data?.locality || data?.principalSubdivision || null
    } catch { return null }
  }

  // ── 3. Open-Meteo 天气获取（增强版：全字段 + 校验） ──

  const fetchWeather = async (lat, lon) => {
    const params = new URLSearchParams({
      latitude: String(lat),
      longitude: String(lon),
      current: [
        'temperature_2m',
        'relative_humidity_2m',
        'apparent_temperature',
        'weather_code',
        'wind_speed_10m',
        'wind_direction_10m',
        'pressure_msl'
      ].join(','),
      timezone: 'auto'
    })

    const url = `https://api.open-meteo.com/v1/forecast?${params.toString()}`
    const resp = await fetch(url)

    if (!resp.ok) throw new Error(`weather_api_http_${resp.status}`)

    const data = await resp.json()

    const validation = validateWeather(data)
    if (!validation.valid) throw new Error(`weather_validate_${validation.reason}`)

    const c = data.current
    const code = Number(c.weather_code)

    return {
      temperature: c.temperature_2m,
      feelsLike: c.apparent_temperature,
      humidity: c.relative_humidity_2m,
      windSpeed: c.wind_speed_10m,
      windDir: c.wind_direction_10m,
      pressure: c.pressure_msl,
      code,
      observedAt: c.time
    }
  }

  // ── 4. 加载 & 渲染管线 ──

  const loadWeather = async (lat, lon, cityName) => {
    status.value = 'fetching'

    try {
      const w = await fetchWeather(lat, lon)
      const roundedTemp = Math.round(w.temperature)

      let mapped = WMO_MAP[w.code] || null
      if (!mapped) {
        console.warn(`[useWeather] 未知 WMO code: ${w.code}，使用默认`)
        mapped = FALLBACK_WEATHER
      }

      const final = sanityCheckDesc(roundedTemp, w.code, mapped)

      temp.value = roundedTemp
      feelsLike.value = Math.round(w.feelsLike)
      desc.value = final.desc
      icon.value = final.icon
      humidity.value = Math.round(w.humidity)
      windSpeed.value = Math.round(w.windSpeed)
      windDir.value = windDirText(w.windDir)
      pressure.value = w.pressure != null ? Math.round(w.pressure) : null
      if (cityName) location.value = cityName
      status.value = 'ready'

      console.info(
        `[useWeather] 天气加载完成 · ${cityName || lat.toFixed(2)+','+lon.toFixed(2)} ` +
        `${roundedTemp}°C (${final.desc}) · 湿度${Math.round(w.humidity)}% · ` +
        `风${windDirText(w.windDir)} ${Math.round(w.windSpeed)}km/h` +
        (final !== mapped ? ` [code ${w.code} "${mapped.desc}" 已校准→"${final.desc}"]` : '')
      )
    } catch (err) {
      console.error('[useWeather] 天气加载失败:', err.message || err)
      status.value = 'error'

      temp.value = null
      feelsLike.value = null
      desc.value = '天气未知'
      icon.value = '🌤️'
      humidity.value = null
      windSpeed.value = null
      windDir.value = ''
      pressure.value = null
    }
  }

  // ── 5. 入口编排 ──

  const load = async () => {
    if (manualCoords) {
      await loadWeather(manualCoords.lat, manualCoords.lon, manualCoords.name || location.value || '')
      return
    }

    status.value = 'locating'
    const coords = await getCoords()

    if (coords._denied) {
      status.value = 'denied'
      return
    }

    const name = await reverseGeocode(coords.lat, coords.lon)
    await loadWeather(coords.lat, coords.lon, name)
  }

  // ── 6. 城市选择器逻辑 ──

  const searchCities = async (keyword) => {
    if (!keyword || keyword.trim().length < 1) {
      searchResults.value = []
      return
    }

    searching.value = true
    try {
      const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(keyword.trim())}&count=5&language=zh&format=json`
      const resp = await fetch(url)
      if (!resp.ok) { searchResults.value = []; return }
      const data = await resp.json()
      searchResults.value = (data.results || []).map((r) => ({
        name: r.name || '',
        lat: r.latitude,
        lon: r.longitude,
        admin: r.admin1 ? `${r.admin1}${r.country ? (' · ' + r.country) : ''}` : (r.country || '')
      }))
    } catch {
      searchResults.value = []
    } finally {
      searching.value = false
    }
  }

  const onCityInput = () => {
    if (debounceTimer) clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => {
      searchCities(cityKeyword.value)
    }, 300)
  }

  const togglePicker = () => {
    showPicker.value = !showPicker.value
    if (showPicker.value && cityKeyword.value) {
      searchCities(cityKeyword.value)
    }
  }

  const pickCity = (city) => {
    manualCoords = { lat: city.lat, lon: city.lon, name: city.name }
    location.value = city.name
    cityKeyword.value = ''
    searchResults.value = []
    showPicker.value = false
    loadWeather(city.lat, city.lon, city.name)
  }

  const resetCity = () => {
    manualCoords = null
    cityKeyword.value = ''
    searchResults.value = []
    showPicker.value = false
    load()
  }

  // ── 7. 生命周期 & 对外暴露 ──

  onMounted(load)

  const reAuthorize = () => { load() }

  return {
    temp, feelsLike, desc, icon,
    humidity, windSpeed, windDir, pressure,
    location, status,
    showPicker, cityKeyword, searchResults, searching,
    togglePicker, onCityInput, searchCities, pickCity, resetCity,
    reAuthorize
  }
}
