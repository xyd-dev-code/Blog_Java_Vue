import assert from 'node:assert/strict'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

// Compare real, unmodified copy before/after this enhancement in both themes.
// The baseline is captured from the working tree, never from an unrelated HEAD.
const directory = resolve(process.env.WUXIA_QA_OUTPUT || 'design-qa-assets/wuxia-pages')
const before = JSON.parse(readFileSync(resolve(directory, 'before.json'), 'utf8'))
const after = JSON.parse(readFileSync(resolve(directory, 'after.json'), 'utf8'))
// Other work may intentionally update the homepage in the shared workspace.
// Use this option when verifying this task's explicitly non-home scope.
if (process.argv.includes('--non-home')) {
  before.records = before.records.filter(r => r.route !== '/')
  after.records = after.records.filter(r => r.route !== '/')
}
const key = r => `${r.width}:${r.theme}:${r.route}`
const baseline = new Map(before.records.map(r => [key(r), r]))
const differences = []
let boxes = 0
for (const current of after.records) {
  const original = baseline.get(key(current))
  assert.ok(original, `Missing baseline: ${key(current)}`)
  if (current.boxes.length !== original.boxes.length) {
    differences.push({ case: key(current), kind: 'element-count', before: original.boxes.length, after: current.boxes.length })
  }
  current.boxes.forEach((box, index) => {
    const old = original.boxes[index]
    if (!old) return
    boxes++
    const styles = Object.keys(box.styles).filter(p => box.styles[p] !== old.styles[p])
    const rect = box.rect.map((value, i) => +(value - old.rect[i]).toFixed(2))
    if (styles.length || rect.some(d => Math.abs(d) > 1)) {
      differences.push({ case: key(current), element: box.key, styles, rect, before: old.rect, after: box.rect })
    }
  })
  if (current.overflow && !original.overflow) differences.push({ case: key(current), kind: 'new-horizontal-overflow' })
  // Stats includes a live "last refreshed" clock; it is not authored UI copy.
  const copy = text => current.route === '/admin/stats' ? text.replace(/\b\d{2}:\d{2}:\d{2}\b/g, '[clock]') : text
  if ((current.route === '/' || current.theme === 'sunny') && copy(current.text) !== copy(original.text)) {
    differences.push({ case: key(current), kind: 'protected-copy-changed' })
  }
}
assert.equal(after.records.length, before.records.length, 'All baseline routes/themes must be tested')
const report = { pass: differences.length === 0 && after.exceptions.length === 0,
  cases: after.records.length, boxes, exceptions: after.exceptions, differences,
  dataMode: 'Local deterministic API fixtures, no backend writes; original copy retained in measurements.' }
writeFileSync(resolve(directory, 'report.json'), JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
if (!report.pass) process.exitCode = 1
