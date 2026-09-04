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
  const precision = value.precision === 'device' ? 'device' : (district ? 'district' : 'city')
  return { lat, lon, name, city, district, precision }
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

function getDeviceCoordinates({ signal } = {}) {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('device_location_unsupported'))
      return
    }
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'))
      return
    }

    let settled = false
    const finish = (callback, value) => {
      if (settled) return
      settled = true
      signal?.removeEventListener('abort', onAbort)
      callback(value)
    }
    const onAbort = () => finish(reject, new DOMException('Aborted', 'AbortError'))
    signal?.addEventListener('abort', onAbort, { once: true })

    try {
      navigator.geolocation.getCurrentPosition(
        position => finish(resolve, {
          lat: position.coords.latitude,
          lon: position.coords.longitude,
        }),
        geolocationError => {
          const reason = geolocationError?.code === 1
            ? 'device_location_denied'
            : geolocationError?.code === 3
              ? 'device_location_timeout'
              : 'device_location_unavailable'
          finish(reject, new Error(reason))
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 5 * 60 * 1000 }
      )
    } catch {
      finish(reject, new Error('device_location_unavailable'))
    }
  })
}

/** 用户主动授权后的设备坐标。反向地理编码失败仍可按真实坐标获取天气。 */
export async function locateByDevice({ signal } = {}) {
  const coordinates = await getDeviceCoordinates({ signal })
  let name = '当前位置'
  try {
    const query = new URLSearchParams({
      latitude: String(coordinates.lat),
      longitude: String(coordinates.lon),
      localityLanguage: 'zh',
    })
    const result = await fetchWeatherJson(
      `https://api.bigdatacloud.net/data/reverse-geocode-client?${query.toString()}`,
      { signal, timeout: 6000 }
    )
    name = cleanName(result.city) || cleanName(result.locality)
      || cleanName(result.principalSubdivision) || name
  } catch (error) {
    if (signal?.aborted || error?.name === 'AbortError') throw error
  }
  return normalizeLocation({ ...coordinates, name, precision: 'device' })
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
    // 浏览器直接发请求：识别访客的公网出口，不会误用网站服务器 IP。
    async () => {
      const result = await fetchWeatherJson('https://ipwho.is/?lang=zh-CN&fields=success,city,latitude,longitude', { signal })
      return result.success === true ? normalizeLocation({ city: result.city, lat: result.latitude, lon: result.longitude }) : null
    },
    async () => {
      const result = await fetchWeatherJson('https://ipapi.co/json/', { signal })
      return !result.error ? normalizeLocation({ city: result.city, lat: result.latitude, lon: result.longitude }) : null
    },
    async () => {
      const result = await fetchWeatherJson('https://api.ipapi.is/', { signal })
      const details = result.location || result
      return !result.error && !result.is_bogon
        ? normalizeLocation({
            name: cleanName(details.city) || '附近',
            lat: details.latitude ?? result.lat,
            lon: details.longitude ?? result.lon,
          })
        : null
    },
    // 服务端可能只能看到 CDN/反代节点，因此仅在浏览器直连来源均失败时兜底。
    // 可选区县数据源的 API Key 永远不会传给浏览器。
    async () => {
      const result = await fetchWeatherJson('/api/v1/site/weather-location', { signal })
      return result.code === 200 ? normalizeLocation(result.data) : null
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
