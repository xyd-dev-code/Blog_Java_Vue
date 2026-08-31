import yaml from 'js-yaml'

export function parseMd(filename, text) {
  text = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  let body = text
  let fm = {}
  if (text.startsWith('---\n') || text.startsWith('---\r\n')) {
    const firstNl = text.indexOf('\n')
    const secondDash = text.indexOf('\n---', firstNl + 1)
    if (secondDash > 0) {
      const fmRaw = text.substring(firstNl + 1, secondDash)
      if (fmRaw.length > 20000) return { filename, status: 'err', error: '元数据不能超过 20k 字符', bodyLength: 0 }
      body = text.substring(secondDash + 4)
      if (body.startsWith('\n')) body = body.substring(1)
      try {
        const parsed = yaml.load(fmRaw, { schema: yaml.JSON_SCHEMA })
        if (parsed && typeof parsed === 'object') fm = parsed
      } catch (e) {
        return {
          filename, status: 'err', error: `YAML 解析失败: ${e.message}`,
          existed: false, title: filename, category: '', tags: [], bodyLength: body.length
        }
      }
    }
  }

  const title = (fm.title && String(fm.title).trim()) || filename.replace(/\.md$/i, '')
  const slug = fm.slug ? String(fm.slug) : ''
  const category = fm.category ? String(fm.category) : ''
  const tags = Array.isArray(fm.tags) ? fm.tags.filter(t => t != null).map(String) : []

  return {
    filename,
    status: 'ok',
    title,
    slug,
    category,
    tags,
    bodyLength: body.length,
    existed: false,        // 前端**只**展示;真伪由后端权威判定
    error: ''
  }
}
