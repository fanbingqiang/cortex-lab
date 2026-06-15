import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import hljs from 'highlight.js'

marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code: string, lang: string) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  }
}))

export function renderMarkdown(text: string): string {
  if (!text) return ''
  const result = marked.parse(text)
  // marked.parse 可能返回 Promise（使用异步插件时），但 v-html 需要同步字符串
  if (typeof result === 'string') return result
  return text
}
