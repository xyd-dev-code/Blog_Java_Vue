import assert from 'node:assert/strict'
import { chromium } from 'playwright'

const origin = process.env.TOC_QA_ORIGIN || 'http://127.0.0.1:5173'
const browser = await chromium.launch({ channel: 'msedge', headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const content = Array.from({ length: 16 }, (_, i) =>
    `${i % 3 === 1 ? '###' : '##'} Section ${i + 1}\n\n${('Paragraph for reading and scrolling. '.repeat(20) + '\n\n').repeat(3)}`
  ).join('\n')
  await page.route('**/*', async route => {
    const url = new URL(route.request().url())
    if (url.pathname.startsWith('/api/')) {
      let data = []
      if (url.pathname.endsWith('/site')) data = { siteTheme: 'sunny', siteName: 'TOC QA' }
      if (/\/articles\/toc-/.test(url.pathname)) data = {
        article: { id: 1, slug: 'toc-one', title: 'TOC regression', content, tags: [], allowComment: 0 },
        next: { slug: 'toc-two', title: 'Next article' }, related: []
      }
      await route.fulfill({ json: { code: 200, data } })
    } else if (url.origin !== origin) await route.abort()
    else await route.continue()
  })
  await page.goto(`${origin}/articles/toc-one`)
  const headings = page.locator('.art-content h2, .art-content h3')
  await headings.last().waitFor()
  await page.waitForFunction(() => document.querySelectorAll('.toc-item').length === 16)
  const expectActive = async index => {
    await page.waitForFunction(i => document.querySelector('.toc-item.active a')?.textContent === `Section ${i + 1}`, index)
    assert.equal(await page.locator('.toc-item.active').count(), 1)
  }
  const scrollToSection = async index => {
    await headings.nth(index).evaluate(el => window.scrollTo({ top: window.scrollY + el.getBoundingClientRect().top - 100, behavior: 'instant' }))
    await expectActive(index)
  }
  await scrollToSection(0)
  // Simulate MdPreview replacing rendered HTML after its asynchronous dependencies load.
  await page.evaluate(() => {
    for (const el of document.querySelectorAll('.art-content h2, .art-content h3')) el.replaceWith(el.cloneNode(true))
    window.dispatchEvent(new Event('scroll'))
  })
  await scrollToSection(1)
  console.log('PASS: asynchronously replaced headings remain synchronized')
  for (const index of [2, 5, 9, 15, 10, 4, 0]) await scrollToSection(index)
  console.log('PASS: h2/h3 synchronization in both scroll directions')
  await page.locator('.toc-item a').nth(8).click()
  await expectActive(8)
  await page.waitForFunction(() => {
    const h = document.querySelectorAll('.art-content h2, .art-content h3')[8]
    return Math.abs(h.getBoundingClientRect().top - 110) < 2
  })
  console.log('PASS: directory click aligns heading and active item')
  await scrollToSection(15)
  assert.ok(await page.locator('.toc-item.active').evaluate(el => {
    const box = el.closest('.art-toc').getBoundingClientRect()
    const item = el.getBoundingClientRect()
    return item.top >= box.top && item.bottom <= box.bottom
  }), 'Active item should stay inside the independently scrolling directory')
  await page.locator('.art-footer-nav a').click()
  await page.waitForURL('**/articles/toc-two')
  await headings.last().waitFor()
  await scrollToSection(3)
  console.log('PASS: long directory visibility and article navigation')
} finally {
  await browser.close()
}
