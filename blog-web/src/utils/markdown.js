import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-light.css'

marked.setOptions({
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (_) {}
    }
    return hljs.highlightAuto(code).value
  },
  langPrefix: 'hljs language-',
  breaks: true,
  gfm: true
})

const wrapTables = (html) => {
  if (!html) return html
  return html.replace(/<table\b([^>]*)>[\s\S]*?<\/table>/g, (match, attrs) => {
    if (/<table\b/i.test(match.slice(attrs.length + 7))) {
      return match
    }
    const inner = match.slice(7 + attrs.length, -8)
    return `<div class="table-wrap"><table${attrs}>${inner}</table></div>`
  })
}

export const renderMarkdown = (md) => {
  if (!md) return ''
  return wrapTables(marked.parse(md, { async: false }))
}