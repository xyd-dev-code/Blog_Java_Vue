export const parseMarkdownInWorker = (filename, text) => new Promise((resolve, reject) => {
  if (text.length > 250000) return reject(new Error('Markdown 内容不能超过 250k 字符'))
  const worker = new Worker(new URL('./markdownImport.worker.js', import.meta.url), { type: 'module' })
  const finish = (error, value) => {
    clearTimeout(timer)
    worker.terminate()
    if (error) reject(error)
    else resolve(value)
  }
  const timer = setTimeout(() => finish(new Error('解析超时，请简化文件内容')), 3000)
  worker.onmessage = ({ data }) => finish(data.error ? new Error(data.error) : null, data.result)
  worker.onerror = () => finish(new Error('解析失败，请检查文件格式'))
  worker.postMessage({ filename, text })
})
