import DOMPurify from 'dompurify'

// Clean the renderer's final HTML, including generated code blocks, SVG and math.
export const sanitizeMarkdown = (html) => DOMPurify.sanitize(html, {
  USE_PROFILES: { html: true, svg: true, mathMl: true },
  FORBID_TAGS: ['iframe', 'object', 'embed', 'form'],
  FORBID_ATTR: ['srcdoc']
})

export const sanitizeDiagram = async (html) => sanitizeMarkdown(html)
