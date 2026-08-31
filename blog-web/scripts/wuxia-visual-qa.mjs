import { createRequire } from 'node:module'
import { mkdir, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import assert from 'node:assert/strict'
import { hasWuxiaForegroundOverlap } from './wuxia-art-visibility.mjs'

// Uses the desktop's bundled browser driver. The site and its API remain read-only.
const require = createRequire(import.meta.url)
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright')
const output = resolve('design-qa-assets/wuxia')
await mkdir(output, { recursive: true })
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' })
const exceptions = []
page.on('pageerror', error => exceptions.push(error.message))
const before = process.argv.includes('--before')
const report = { exceptions, views: [] }
try {
  for (const width of [1900, 1440, 1024, 768, 520, 390]) {
    await page.setViewportSize({ width, height: width === 768 ? 1024 : width === 390 ? 844 : 900 })
    await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
    await page.locator('.sky-hero').waitFor()
    await page.evaluate(async () => {
      const { useThemeStore } = await import('/src/stores/theme.js')
      useThemeStore().setTheme('ink')
      await document.fonts.ready
      await Promise.all([...document.querySelectorAll('.ink-hero-scene img')].map(img => img.decode().catch(() => {})))
    })
    await page.locator('.hero-text.visible').waitFor()
    await page.waitForTimeout(600)
    await page.screenshot({ path: resolve(output, `${before ? 'before' : 'after'}-${width}.png`) })
    report.views.push(await page.evaluate(() => ({
      width: innerWidth, theme: document.documentElement.dataset.theme,
      overflow: document.documentElement.scrollWidth > innerWidth,
      geometry: Object.fromEntries(['.nav-inner', '.sky-hero', '.hero-grid', '.hero-text', '.weather-card', '.hero-actions'].map(selector => [selector, document.querySelector(selector).getBoundingClientRect().toJSON()])),
      titleFont: getComputedStyle(document.querySelector('.hero-title')).fontFamily,
      images: [...document.querySelectorAll('.ink-hero-scene img')].map(img => ({ src: img.getAttribute('src'), loaded: img.complete && img.naturalWidth > 0 })),
      video: document.querySelector('.wuxia-duelist video')?.getAttribute('src') || null,
    })))
    report.views.at(-1).characterOverlapsForeground = await page.evaluate(hasWuxiaForegroundOverlap)
  }
  if (!before) {
    // Same existing navigation and city-picker interactions, with decorative
    // layers present. Nothing is submitted or written to the backend.
    await page.getByRole('button', { name: '菜单', exact: true }).click()
    await page.locator('.drawer-panel').waitFor({ state: 'visible' })
    assert.equal(await page.locator('.drawer-panel').isVisible(), true)
    await page.locator('.drawer-link[href="/articles"]').click()
    await page.waitForURL('**/articles')
    await page.locator('.mobile-drawer').waitFor({ state: 'detached' })
    assert.equal(await page.locator('.mobile-drawer').count(), 0)
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.evaluate(() => document.fonts.ready)
    await page.screenshot({ path: resolve(output, 'articles-desktop.png') })
    await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
    await page.locator('.hero-text.visible').waitFor()
    await page.locator('.weather-loc-btn').click()
    await page.locator('.cp-input').waitFor({ state: 'visible' })
    assert.equal(await page.locator('.cp-input').isVisible(), true)
    await page.locator('.weather-loc-btn').click()
    await page.locator('.hero-actions .el-button--primary').click()
    await page.waitForURL('**/articles')

    // Full theme transitions are driven through the real store, without saving
    // admin settings. The configured motto must survive both theme transitions.
    await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
    await page.locator('.sky-hero').waitFor()
    const configuredMotto = await page.evaluate(async () => {
      const site = (await import('/src/stores/site.js')).useSiteStore()
      await site.load()
      return site.info?.motto || '春山可望'
    })
    await page.evaluate(async () => (await import('/src/stores/theme.js')).useThemeStore().setTheme('sunny'))
    await page.waitForFunction(motto => document.querySelector('.title-accent').textContent.trim() === motto, configuredMotto)
    assert.equal((await page.locator('.title-accent').textContent()).trim(), configuredMotto)
    assert.equal(await page.locator('.wuxia-duelist video').count(), 0)
    await page.evaluate(async () => (await import('/src/stores/theme.js')).useThemeStore().setTheme('ink'))
    await page.waitForFunction(motto => document.querySelector('.title-accent').textContent.trim() === motto, configuredMotto)
    assert.equal((await page.locator('.title-accent').textContent()).trim(), configuredMotto)
    assert.equal(await page.locator('.wuxia-duelist video').count(), 0)
    await page.emulateMedia({ reducedMotion: 'no-preference' })
    // The completed outline is a still asset. Motion preferences must not
    // bring back the old cropped rectangular video.
    assert.equal(await page.locator('.wuxia-duelist video').count(), 0)
    assert.ok(await page.locator('.wuxia-duelist img').evaluate(el => el.complete && el.naturalWidth > 0))
    await page.screenshot({ path: resolve(output, 'home-complete-outline.png') })
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.locator('.wuxia-duelist video').waitFor({ state: 'detached' })
    assert.equal(await page.locator('.wuxia-duelist video').count(), 0)
    report.interactions = { mobileMenu: true, primaryCta: true, cityPicker: true, themeRoundTrip: true, reducedMotion: true, staticArtwork: true }

    // Save a full-page view after observing the existing scroll-reveal sections.
    for (let y = 0; y < await page.evaluate(() => document.documentElement.scrollHeight); y += 650) {
      await page.evaluate(scrollY => window.scrollTo(0, scrollY), y)
      await page.waitForTimeout(60)
    }
    await page.evaluate(() => window.scrollTo(0, 0))
    await page.waitForTimeout(200)
    await page.screenshot({ path: resolve(output, 'home-full.png'), fullPage: true })
    for (const view of report.views) {
      assert.equal(view.overflow, false)
      assert.equal(view.characterOverlapsForeground, false, `${view.width}px: swordsman must avoid title glyphs, buttons and weather card`)
      assert.ok(view.images.every(image => image.loaded))
    }
    assert.deepEqual(exceptions, [])
  }
  await writeFile(resolve(output, `${before ? 'before' : 'after'}.json`), JSON.stringify(report, null, 2))
  console.log(JSON.stringify({ pass: true, widths: report.views.map(view => view.width), exceptions, interactions: report.interactions, output }, null, 2))
} finally { await browser.close() }
