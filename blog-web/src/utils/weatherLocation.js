// IP 归属地是估算位置。只使用服务明确返回的区县，不用城市中心点反推出区名。
export const MANUAL_CITY_KEY = 'blog.weather.manual-city.v1'
const IP_CACHE_KEY = 'blog.weather.ip-location.v1'
const IP_CACHE_TTL = 10 * 60 * 1000

export async function fetchWeatherJson(url, { signal, timeout = 6000 } = {}) {
  const controller = new AbortController()
  const abort = () => controller.abort()
  if (signal?.aborted) controller.abort()
  signal?.addEventListener('abort', abort, { once: true })
  const timer = setTimeout(abort, timeout)
  try {
    const response = await fetch(url, {
      signal: controller.signal, credentials: 'omit', referrerPolicy: 'no-referrer',
    })
    if (!response.ok) throw new Error(`weather_http_${response.status}`)
    return await response.json()
  } finally {
    clearTimeout(timer)
    signal?.removeEventListener('abort', abort)
  }
}

const cleanName = value => typeof value === 'string' && !['-', '0', 'N/A'].includes(value.trim()) ? value.trim() : ''

export function normalizeLocation(value) {
  const isCoordinate = coordinate => typeof coordinate === 'number' || (typeof coordinate === 'string' && coordinate.trim() !== '')
  if (!value || !isCoordinate(value.lat) || !isCoordinate(value.lon)) return null
  const lat = Number(value.lat)
  const lon = Number(value.lon)
  const city = cleanName(value.city)
  const district = cleanName(value.district)
  const name = district ? [...new Set([city, district].filter(Boolean))].join(' · ') : cleanName(value.name) || city
  if (!name || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) return null
  return { lat, lon, name, city, district, precision: district ? 'district' : 'city' }
}

export function readManualCity() {
  try { return normalizeLocation(JSON.parse(localStorage.getItem(MANUAL_CITY_KEY))) } catch { return null }
}

export function saveManualCity(city) {
  try {
    if (city) localStorage.setItem(MANUAL_CITY_KEY, JSON.stringify(city))
    else localStorage.removeItem(MANUAL_CITY_KEY)
  } catch { /* 禁用存储时仍可在当前页面选城市。 */ }
}

export async function locateByIp({ signal, refresh = false } = {}) {
  if (!refresh) {
    try {
      const cached = JSON.parse(sessionStorage.getItem(IP_CACHE_KEY))
      const age = Date.now() - cached?.savedAt
      const location = normalizeLocation(cached?.location)
      if (location && age >= 0 && age < IP_CACHE_TTL) return location
    } catch { /* 缓存不可用时重新获取。 */ }
  }

  const sources = [
    // 可选的区县数据源由后端调用，API Key 永远不会传给浏览器。
    async () => {
      const result = await fetchWeatherJson('/api/v1/site/weather-location', { signal })
      return result.code === 200 ? normalizeLocation(result.data) : null
    },
    // 浏览器直接发请求：识别访客的公网出口，不会误用网站服务器 IP。
    async () => {
      const result = await fetchWeatherJson('https://ipwho.is/?lang=zh-CN&fields=success,city,latitude,longitude', { signal })
      return result.success === true ? normalizeLocation({ city: result.city, lat: result.latitude, lon: result.longitude }) : null
    },
    async () => {
      // 该服务的匿名接口只保证经纬度，不返回城市名；仍可获取天气，不能编造城市/区县。
      const result = await fetchWeatherJson('https://api.ipapi.is/', { signal })
      return !result.error && !result.is_bogon
        ? normalizeLocation({ name: '附近', lat: result.lat, lon: result.lon })
        : null
    },
    async () => {
      const result = await fetchWeatherJson('https://ipapi.co/json/', { signal })
      return !result.error ? normalizeLocation({ city: result.city, lat: result.latitude, lon: result.longitude }) : null
    },
  ]
  for (const source of sources) {
    if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
    try {
      const location = await source()
      if (!location || signal?.aborted) continue
      try { sessionStorage.setItem(IP_CACHE_KEY, JSON.stringify({ savedAt: Date.now(), location })) } catch { /* 可选缓存 */ }
      return location
    } catch { /* 超时、限流、CORS 或无城市信息时尝试下一个来源。 */ }
  }
  throw new Error('ip_location_unavailable')
}
