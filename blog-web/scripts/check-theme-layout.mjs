import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { execFileSync } from 'node:child_process'
import { resolve, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import { compileString } from 'sass'
import postcss from 'postcss'
import { parse } from '@vue/compiler-sfc'
import { themeRegistry } from '../src/themes/registry.js'

const root = fileURLToPath(new URL('../', import.meta.url))
const geometry = /^(display|position|inset(?:-.+)?|top|right|bottom|left|z-index|isolation|box-sizing|(?:min-|max-)?(?:width|height)|aspect-ratio|margin(?:-.+)?|padding(?:-.+)?|gap|row-gap|column-gap|grid(?:-.+)?|flex(?:-.+)?|align(?:-.+)?|justify(?:-.+)?|order|float|clear|overflow(?:-.+)?|transform(?:-.+)?|perspective(?:-.+)?|font(?:-.+)?|line-height|letter-spacing|word-spacing|word-break|overflow-wrap|white-space|text-wrap|text-align|text-indent|text-transform|vertical-align|border(?:-(?:top|right|bottom|left))?(?:-width|-style)?|border(?:-.+)?-radius|clip-path|content)$/
const compile = (source) => postcss.parse(compileString(source, { logger: { warn() {}, debug() {} } }).css)
const themeCss = compile(readFileSync(resolve(root, 'src/styles/themes.scss'), 'utf8'))
const violations = []
// Only explicitly scoped font-family substitutions are permitted.
// Font size, line-height, spacing and wrapping remain strict layout invariants.
const typographyRules = new Map([
  ["html[data-theme=ink] .front-main :is(.hero-title, .ha-title, .hf-title, .hg-title, .hs-title, .sky-page-head h1, .art-title, .title-accent, .hero-desc, .section-title, .section-sub, .cta-title, .cta-desc, .sky-page-head p, .ha-sub, .hf-sub, .hg-sub, .hs-sub, .ha-tagline, .hero-subtitle, .hero-tagline, .hero-hint, .art-deck, .fh-title, .fh-desc, .fg-title, .ss-title, .sec-title, .rs-title, .toc-title, .profile-info h2)", ['font-family']],
  ["html[data-theme=ink] .notfound :is(h1, p)", ['font-family']],
  ['html[data-theme=ink] .ha-cursor', ['font-family']],
])
themeCss.walkRules((rule) => {
  const keyframes = rule.parent?.type === 'atrule' && /keyframes$/.test(rule.parent.name)
  if (keyframes && rule.parent.params.startsWith('ink-')) return
  rule.walkDecls((decl) => {
    const displayTypeface = typographyRules.get(rule.selector.replace(/\s+/g, ' '))?.includes(decl.prop)
    const decoration = (rule.selector === '.app-root' && decl.prop === 'isolation') ||
      rule.selector === '.ink-feedback-drop' ||
      (rule.selector === '.sky-hero .ink-mountain-layer' && decl.prop === 'transform')
    if (geometry.test(decl.prop) && !decoration && !displayTypeface) violations.push(`${rule.selector}: ${decl.prop}: ${decl.value}`)
  })
})
assert.deepEqual(violations, [], 'Theme CSS must not override layout, typography metrics or component shapes')

const baselineTokens = themeRegistry[0].tokens
for (const theme of themeRegistry) {
  for (const key of Object.keys(baselineTokens).filter((name) => /^--(?:font|radius)/.test(name))) {
    assert.equal(theme.tokens[key], baselineTokens[key], `${theme.id} changes shared geometry token ${key}`)
  }
}
assert.doesNotMatch(readFileSync(resolve(root, 'src/styles/themes.scss'), 'utf8'), /hero-text\s*::(?:before|after)/,
  'The original hero text is not a card: no theme-generated surface may wrap it')

const baseline = process.argv.find((arg) => arg.startsWith('--baseline='))?.split('=')[1]
let comparedFiles = 0
if (baseline) {
  const repoRoot = execFileSync('git', ['rev-parse', '--show-toplevel'], { cwd: root, encoding: 'utf8' }).trim()
  const htmlPath = relative(repoRoot, resolve(root, 'index.html')).replaceAll('\\', '/')
  const originalHtml = execFileSync('git', ['show', `${baseline}:${htmlPath}`], { cwd: root, encoding: 'utf8' })
  const fontLinks = (html) => html.match(/<link[^>]+fonts\.googleapis\.com\/css[^>]+>/g) || []
  assert.deepEqual(fontLinks(readFileSync(resolve(root, 'index.html'), 'utf8')), fontLinks(originalHtml),
    'Theme work must not change the original font metrics by loading a different font')
  const files = execFileSync('git', ['ls-files', 'src/components/*.vue', 'src/views/front/*.vue', 'src/layouts/FrontLayout.vue', 'src/styles/index.scss', 'src/styles/effects.scss'], { cwd: root, encoding: 'utf8' }).trim().split('\n')
  const layoutMap = (source, file) => {
    const styles = file.endsWith('.vue') ? parse(source).descriptor.styles.map((style) => style.content) : [source]
    const result = {}
    styles.forEach((style, index) => {
      compile(style).walkRules((rule) => {
        // Absolutely positioned, aria-hidden background art is theme paint, not content layout.
        if (/\.ink-(?:hero-scene|paper-layer|mountain-layer|foliage-)/.test(rule.selector)) return
        const parents = []
        for (let parent = rule.parent; parent; parent = parent.parent) {
          if (parent.type === 'atrule') parents.unshift(`@${parent.name} ${parent.params}`)
        }
        const key = `${index}|${parents.join('|')}|${rule.selector.replace(/\s+/g, ' ')}`
        rule.nodes?.filter((node) => node.type === 'decl' && geometry.test(node.prop)).forEach((decl) => {
          let value = decl.value.replace(/\s+/g, ' ').trim()
          if (/^border(?:-(?:top|right|bottom|left))?$/.test(decl.prop)) {
            value = value.match(/^(?:(?:\d*\.)?\d+(?:px|rem|em)|0|none)(?:\s+(?:solid|dashed|dotted|double|none))?/)?.[0] || value
          }
          const property = `${key}|${decl.prop}`
          ;(result[property] ||= []).push(value)
        })
      })
    })
    return result
  }
  const diffs = []
  for (const file of files) {
    const absolute = resolve(root, file)
    const gitPath = relative(repoRoot, absolute).replaceAll('\\', '/')
    let original
    try { original = execFileSync('git', ['show', `${baseline}:${gitPath}`], { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }) }
    catch { continue }
    const before = layoutMap(original, file)
    const after = layoutMap(readFileSync(absolute, 'utf8'), file)
    for (const key of new Set([...Object.keys(before), ...Object.keys(after)])) {
      if (JSON.stringify(before[key]) !== JSON.stringify(after[key])) diffs.push({ file, key, before: before[key], after: after[key] })
    }
    comparedFiles++
  }
  assert.deepEqual(diffs, [], 'Content layout must match the original sunny implementation')
}
console.log(JSON.stringify({ pass: true, themes: themeRegistry.map((theme) => theme.id), originalLayoutFilesCompared: comparedFiles }))
