import { ref, onMounted } from 'vue'

/**
 * 基于浏览器地理位置 + Open-Meteo 免 Key API 的实时天气。
 *
 * 流程：
 *   1) navigator.geolocation 拿经纬度（用户拒绝则用默认 fallback 城市）
 *   2) 反向地理编码到城市名（Open-Meteo 的 geocoding-api 接口，免 Key）
 *   3) 拿当前温度与 WMO 天气代码
 *   4) WMO code → 中文文案 + emoji 图标
 *
 * 返回的 temp/desc/icon/location 都是 ref，可直接在模板里用。
 */
export function useWeather(options = {}) {
  const {
    fallback = { lat: 28.23, lon: 112.94, name: '长沙' }, // 站点默认
    lang = 'zh'
  } = options

  const temp = ref(null)          // 当前温度（°C）
  const desc = ref('晴')          // 中文天气描述
  const icon = ref('☀️')           // emoji 图标
  const location = ref(fallback.name) // 城市名
  const status = ref('idle')      // idle | locating | fetching | ready | denied | error

  const WMO_MAP = {
    0:  { desc: '晴',           icon: '☀️' },
    1:  { desc: '晴间多云',     icon: '🌤️' },
    2:  { desc: '多云',         icon: '⛅' },
    3:  { desc: '阴',           icon: '☁️' },
    45: { desc: '有雾',         icon: '🌫️' },
    48: { desc: '冰雾',         icon: '🌫️' },
    51: { desc: '小毛毛雨',     icon: '🌦️' },
    53: { desc: '毛毛雨',       icon: '🌦️' },
    55: { desc: '大毛毛雨',     icon: '🌧️' },
    61: { desc: '小雨',         icon: '🌦️' },
    63: { desc: '中雨',         icon: '🌧️' },
    65: { desc: '大雨',         icon: '🌧️' },
    71: { desc: '小雪',         icon: '🌨️' },
    73: { desc: '中雪',         icon: '❄️' },
    75: { desc: '大雪',         icon: '❄️' },
    77: { desc: '雪粒',         icon: '🌨️' },
    80: { desc: '阵雨',         icon: '🌦️' },
    81: { desc: '强阵雨',       icon: '🌧️' },
    82: { desc: '暴雨',         icon: '⛈️' },
    85: { desc: '阵雪',         icon: '🌨️' },
    86: { desc: '强阵雪',       icon: '❄️' },
    95: { desc: '雷阵雨',       icon: '⛈️' },
    96: { desc: '雷雨夹冰雹',   icon: '⛈️' },
    99: { desc: '强雷雨夹冰雹', icon: '⛈️' }
  }

  const getCoords = () => new Promise((resolve) => {
    if (!('geolocation' in navigator)) return resolve(fallback)
    navigator.geolocation.getCurrentPosition(
      pos => resolve({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
      () => resolve({ ...fallback, _denied: true }),
      { timeout: 6000, maximumAge: 10 * 60 * 1000 }
    )
  })

  const reverseGeocode = async (lat, lon) => {
    try {
      const url = `https://geocoding-api.open-meteo.com/v1/reverse?latitude=${lat}&longitude=${lon}&language=${lang}`
      const resp = await fetch(url)
      if (!resp.ok) return null
      const data = await resp.json()
      const r = data?.results?.[0]
      return r?.name || r?.admin1 || null
    } catch { return null }
  }

  const fetchWeather = async (lat, lon) => {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,weather_code&timezone=auto`
    const resp = await fetch(url)
    if (!resp.ok) throw new Error('weather api failed')
    const data = await resp.json()
    const cur = data?.current
    if (!cur) throw new Error('no current data')
    return { temperature: cur.temperature_2m, code: cur.weather_code }
  }

  const load = async () => {
    status.value = 'locating'
    const coords = await getCoords()
    location.value = (await reverseGeocode(coords.lat, coords.lon)) || fallback.name
    if (coords._denied) status.value = 'denied'

    status.value = 'fetching'
    try {
      const { temperature, code } = await fetchWeather(coords.lat, coords.lon)
      temp.value = Math.round(temperature)
      const m = WMO_MAP[code] || { desc: '晴', icon: '☀️' }
      desc.value = m.desc
      icon.value = m.icon
      status.value = 'ready'
    } catch (e) {
      status.value = 'error'
      // 极端兜底
      temp.value = 24
      desc.value = '天气未知'
      icon.value = '🌤️'
    }
  }

  onMounted(load)

  return { temp, desc, icon, location, status }
}