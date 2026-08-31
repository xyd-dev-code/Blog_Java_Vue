import { parseMd } from './markdownImport.js'
self.onmessage = ({ data }) => {
  try { self.postMessage({ result: parseMd(data.filename, data.text) }) }
  catch (error) { self.postMessage({ error: error.message || '解析失败' }) }
}
