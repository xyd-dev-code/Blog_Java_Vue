import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const origin = process.env.WEATHER_QA_ORIGIN || 'http://localhost:5173'
const output = resolve('design-qa-assets/weather-ip')
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const results = []
const manualKey = 'blog.weather.manual-city.v1'
const current = { time: Math.floor(Date.now() / 1000), temperature_2m: 27, apparent_temperature: 27,
  relative_humidity_2m: 65, wind_speed_10m: 5, wind_direction_10m: 90, pressure_msl: 1008, weather_code: 1 }
const ipCity = { success: true, city: '长沙', latitude: 28.13, longitude: 113.03 }
const search = { results: [{ name: '雨花区', latitude: 28.12, longitude: 113.04, admin2: '长沙市', admin1: '湖南省', country: '中国' }] }

async function scenario(name, config, run) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' })
  const requests = []
  const errors = []
  let releaseIp
  const ipGate = config.holdIp ? new Promise(resolveGate => { releaseIp = resolveGate }) : null
  await context.addInitScript(({ corrupt, blocked, manualKey, geo }) => {
    window.weatherGeoCalls = 0
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
      getCurrentPosition(success, failure) {
        window.weatherGeoCalls++
        if (geo?.coords) {
          success({ coords: { latitude: geo.coords.lat, longitude: geo.coords.lon } })
          return
        }
        if (geo?.denied) {
          failure({ code: 1 })
          return
        }
        throw new Error('Unexpected location permission request')
      },
      watchPosition() { window.weatherGeoCalls++; throw new Error('Unexpected location permission request') },
    } })
    if (corrupt) localStorage.setItem(manualKey, '{invalid json')
    if (blocked) {
      for (const storage of [localStorage, sessionStorage]) {
        const get = storage.getItem.bind(storage)
        const set = storage.setItem.bind(storage)
        const remove = storage.removeItem.bind(storage)
        Object.defineProperty(storage, 'getItem', { value(key) { if (key.startsWith('blog.weather.')) throw new Error('Storage disabled'); return get(key) } })
        Object.defineProperty(storage, 'setItem', { value(key, value) { if (key.startsWith('blog.weather.')) throw new Error('Storage disabled'); return set(key, value) } })
        Object.defineProperty(storage, 'removeItem', { value(key) { if (key.startsWith('blog.weather.')) throw new Error('Storage disabled'); return remove(key) } })
      }
    }
  }, { corrupt: config.corrupt, blocked: config.blockedStorage, manualKey, geo: config.geo })
  await context.route('**/*', async route => {
    const url = new URL(route.request().url())
    const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
    if (url.pathname === '/api/v1/site/weather-location') {
      requests.push({ kind: 'backend' })
      return reply({ code: 200, data: config.district || null })
    }
    if (url.hostname === 'ipwho.is') {
      requests.push({ kind: 'ip' })
      if (ipGate) await ipGate
      if (config.ipFailure) return reply({ success: false }, 429)
      return reply(config.ipData || ipCity)
    }
    if (url.hostname === 'ipapi.co') {
      requests.push({ kind: 'fallback' })
      return config.fallback ? reply(config.fallback) : reply({ error: true }, 503)
    }
    if (url.hostname === 'api.ipapi.is') {
      requests.push({ kind: 'coordinate-fallback' })
      return config.coordinateFallback ? reply(config.coordinateFallback) : reply({ error: true }, 503)
    }
    if (url.hostname === 'api.open-meteo.com') {
      requests.push({ kind: 'weather', lat: url.searchParams.get('latitude'), timeformat: url.searchParams.get('timeformat') })
      if (config.weatherFailure) return reply({}, 503)
      return reply({ current })
    }
    if (url.hostname === 'geocoding-api.open-meteo.com') {
      requests.push({ kind: 'search' })
      return config.searchFailure ? reply({}, 503) : reply(search)
    }
    if (url.hostname === 'api.bigdatacloud.net') {
      requests.push({ kind: 'reverse', lat: url.searchParams.get('latitude') })
      return reply(config.reverse || { city: '长沙市' })
    }
    if (url.pathname.startsWith('/api/')) {
      let data = []
      if (url.pathname === '/api/v1/site') data = { siteTheme: 'sunny', siteName: '拾光小筑', motto: 'Hello World', authorName: '站长', description: '天气 IP 定位回归验证' }
      if (url.pathname === '/api/v1/home') data = { featured: [], latest: [], stats: { articleCount: 8, categoryCount: 7, tagCount: 23, viewCount: 3755 } }
      return reply({ code: 200, data })
    }
    if (url.origin !== origin) return route.abort()
    return route.continue()
  })
  const page = await context.newPage()
  page.on('pageerror', error => errors.push(error.message))
  try {
    await page.goto(origin, { waitUntil: 'domcontentloaded' })
    await page.locator('.sky-hero').waitFor()
    await run({ page, requests, releaseIp, config })
    assert.equal(
      await page.evaluate(() => window.weatherGeoCalls),
      config.expectedGeoCalls || 0,
      'Device location may only be requested by the explicit precise-location action'
    )
    assert.deepEqual(errors, [])
    results.push({ name, passed: true })
    console.log(`PASS ${name}`)
  } finally {
    releaseIp?.()
    await context.close()
  }
}

async function ready(page, expected = '长沙') {
  await page.waitForFunction(expectedLocation => (
    document.querySelector('.weather-temp')?.textContent.includes('27°')
    && document.querySelector('.we-text')?.textContent.includes(expectedLocation)
  ), expected)
  assert.ok((await page.locator('.we-text').textContent()).includes(expected))
}

try {
  await scenario('浏览器直连 IP 优先，避免服务端 CDN 节点误判，刷新复用缓存', {
    district: { city: '洛杉矶', district: '', lat: 34.05, lon: -118.24 },
  }, async ({ page, requests }) => {
    await ready(page)
    assert.doesNotMatch(await page.locator('.we-text').innerText(), /IP 估算/)
    assert.equal(requests.filter(r => r.kind === 'ip').length, 1)
    assert.equal(requests.filter(r => r.kind === 'backend').length, 0)
    assert.deepEqual(requests.find(r => r.kind === 'weather'), { kind: 'weather', lat: '28.13', timeformat: 'unixtime' })
    await page.reload({ waitUntil: 'domcontentloaded' })
    await ready(page)
    assert.equal(requests.filter(r => r.kind === 'ip').length, 1)
    assert.equal(requests.filter(r => r.kind === 'search').length, 0, 'Do not reverse-geocode city center into an invented district')
  })

  await scenario('服务返回区县时展示区县，桌面和手机无横向溢出', {
    ipFailure: true,
    district: { city: '长沙市', district: '雨花区', lat: 28.13, lon: 113.03 },
  }, async ({ page, requests }) => {
    await ready(page, '长沙市 · 雨花区')
    assert.equal(requests.filter(r => r.kind === 'ip').length, 1)
    assert.equal(requests.filter(r => r.kind === 'backend').length, 1)
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 900 })
      await page.evaluate(async width => {
        const { useThemeStore } = await import('/src/stores/theme.js')
        const { useSiteStore } = await import('/src/stores/site.js')
        await useSiteStore().load()
        useSiteStore().setSiteTheme(width === 390 ? 'ink' : 'sunny')
        useThemeStore().setTheme(width === 390 ? 'ink' : 'sunny')
        await document.fonts.ready
      }, width)
      await page.locator('.weather-card.visible').waitFor()
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
      await page.locator('.weather-card').screenshot({ path: resolve(output, `district-${width}.png`) })
    }
  })

  await scenario('主 IP 接口限流后使用备用来源', { ipFailure: true, fallback: { city: 'Hangzhou', latitude: 30.27, longitude: 120.15 } }, async ({ page }) => {
    await ready(page, 'Hangzhou')
  })

  await scenario('备用源只返回经纬度时仍显示天气，不编造城市名', { ipFailure: true, coordinateFallback: { lat: 28.13, lon: 113.03, cc: 'CN' } }, async ({ page }) => {
    await ready(page, '附近')
    assert.doesNotMatch(await page.locator('.we-text').textContent(), /IP 估算/)
  })

  await scenario('IP 归属地误判美国时，用户可授权设备定位恢复长沙天气', {
    ipData: { success: true, city: '洛杉矶', latitude: 34.05, longitude: -118.24 },
    geo: { coords: { lat: 28.23, lon: 112.94 } },
    reverse: { city: '长沙市' },
    expectedGeoCalls: 1,
  }, async ({ page, requests }) => {
    await ready(page, '洛杉矶')
    await page.setViewportSize({ width: 375, height: 812 })
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    const preciseButton = page.getByRole('button', { name: /使用设备精确定位/ })
    assert.ok((await preciseButton.boundingBox()).height >= 44)
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
    await page.locator('.weather-card').screenshot({ path: resolve(output, 'precise-picker-375.png') })
    await page.setViewportSize({ width: 667, height: 375 })
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false)
    await page.setViewportSize({ width: 375, height: 812 })
    await preciseButton.click()
    await ready(page, '长沙市')
    assert.deepEqual(requests.findLast(r => r.kind === 'weather'), {
      kind: 'weather', lat: '28.23', timeformat: 'unixtime',
    })
    assert.equal(requests.filter(r => r.kind === 'reverse').length, 1)
    assert.equal(await page.locator('.we-text').getAttribute('title'), '根据已授权的设备坐标定位')
    assert.equal(await page.evaluate(key => localStorage.getItem(key), manualKey), null)
  })

  await scenario('用户拒绝设备定位时保留已有 IP 天气并提供恢复指引', {
    geo: { denied: true },
    expectedGeoCalls: 1,
  }, async ({ page }) => {
    await ready(page)
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    await page.getByRole('button', { name: /使用设备精确定位/ }).click()
    await page.getByText('未获得定位权限，可在浏览器设置中开启，或直接搜索所在城市。').waitFor()
    await ready(page)
  })

  await scenario('所有 IP 来源失败可重试，也可直接选择区县并持久保存', { ipFailure: true }, async ({ page, config }) => {
    await page.getByText('暂未识别所在地').waitFor()
    assert.equal(await page.getByText('位置信息暂未授权').count(), 0)
    config.ipFailure = false
    await page.getByRole('button', { name: '重试', exact: true }).click()
    await ready(page)
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    await page.getByRole('textbox', { name: '搜索城市或区县' }).fill('雨花区')
    await page.getByRole('button', { name: /雨花区.*长沙市/ }).click()
    await ready(page, '雨花区')
    assert.doesNotMatch(await page.locator('.we-text').innerText(), /IP 估算/)
    assert.ok(await page.evaluate(key => localStorage.getItem(key), manualKey))
    await page.reload({ waitUntil: 'domcontentloaded' })
    await ready(page, '雨花区')
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    await page.getByRole('button', { name: '自动', exact: true }).click()
    await ready(page, '长沙')
    assert.equal(await page.evaluate(key => localStorage.getItem(key), manualKey), null)
  })

  await scenario('天气接口失败后的重试不会请求设备位置', { weatherFailure: true }, async ({ page, config }) => {
    await page.getByText('天气未知').waitFor()
    config.weatherFailure = false
    await page.getByRole('button', { name: '重试', exact: true }).click()
    await ready(page)
  })

  await scenario('慢 IP 响应不能覆盖用户刚选中的区县', { holdIp: true }, async ({ page, requests, releaseIp }) => {
    await page.waitForFunction(() => document.querySelector('.weather-temp')?.textContent.includes('正在识别'))
    // 等待请求真正开始，随后在它返回前选城市。
    while (!requests.some(r => r.kind === 'ip')) await page.waitForTimeout(20)
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    await page.getByRole('textbox', { name: '搜索城市或区县' }).fill('雨花区')
    await page.getByRole('button', { name: /雨花区.*长沙市/ }).click()
    await ready(page, '雨花区')
    releaseIp()
    await page.waitForTimeout(250)
    assert.equal((await page.locator('.we-text').innerText()).trim(), '雨花区')
    assert.equal(requests.filter(r => r.kind === 'weather').length, 1)
  })

  await scenario('损坏的手动城市缓存不会阻断自动天气', { corrupt: true }, async ({ page }) => ready(page))
  await scenario('浏览器禁用天气存储仍能显示天气', { blockedStorage: true }, async ({ page }) => ready(page))
  await scenario('无效坐标不被当成零度坐标使用', { ipData: { success: true, city: '长沙', latitude: null, longitude: null } }, async ({ page, requests }) => {
    await page.getByText('暂未识别所在地').waitFor()
    assert.equal(requests.filter(r => r.kind === 'weather').length, 0)
  })
  await scenario('城市搜索故障明确提示并允许重试', { searchFailure: true }, async ({ page, config }) => {
    await ready(page)
    await page.getByRole('button', { name: '切换城市或区县' }).click()
    const input = page.getByRole('textbox', { name: '搜索城市或区县' })
    await input.fill('雨花区')
    await page.getByText('搜索暂不可用，请稍后重试').waitFor()
    config.searchFailure = false
    await input.press('Enter')
    await page.getByRole('button', { name: /雨花区.*长沙市/ }).waitFor()
  })
} finally {
  await writeFile(resolve(output, 'results.json'), JSON.stringify(results, null, 2))
  await browser.close()
}
