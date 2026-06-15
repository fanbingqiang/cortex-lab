<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { EditorState } from '@codemirror/state'
import {
  EditorView, keymap, lineNumbers, highlightActiveLineGutter,
  highlightSpecialChars, drawSelection, dropCursor, highlightActiveLine
} from '@codemirror/view'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching, indentOnInput } from '@codemirror/language'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { cpp } from '@codemirror/lang-cpp'
import { sql } from '@codemirror/lang-sql'
import { html } from '@codemirror/lang-html'
import { css } from '@codemirror/lang-css'
import { json } from '@codemirror/lang-json'

const props = defineProps<{ code?: string; language?: string }>()
const emit = defineEmits<{ 'code-change': [code: string] }>()
const container = ref<HTMLDivElement>()
const fallback = ref<HTMLTextAreaElement>()
const initError = ref(false)
let editorView: EditorView | null = null

// 语言扩展映射
function langExtension(lang: string) {
  const map: Record<string, () => any> = {
    java, python, javascript, js: javascript,
    cpp: cpp, c: cpp, 'c++': cpp,
    sql, html, css, json,
  }
  return (map[lang] || java)()
}

// 米黄色主题
const cmTheme = EditorView.theme({
  '&': {
    height: '100%',
    backgroundColor: '#faf6ed',
    fontSize: '14px',
  },
  '.cm-editor': { height: '100%' },
  '.cm-scroller': {
    overflow: 'auto',
    fontFamily: "'Cascadia Code', 'Fira Code', Consolas, monospace",
    fontSize: '14px',
  },
  '.cm-content': {
    padding: '12px 0',
    color: '#1e293b',
    caretColor: '#2563eb',
  },
  '.cm-gutters': {
    backgroundColor: '#f5f0e6',
    color: '#a09880',
    border: 'none',
    borderRight: '1px solid #e0d8c8',
  },
  '.cm-activeLineGutter': { backgroundColor: '#ede7db', color: '#7a6a50' },
  '.cm-activeLine': { backgroundColor: '#f0ebe0' },
  '.cm-cursor': { borderLeftColor: '#2563eb' },
  '.cm-selectionBackground': { backgroundColor: '#d4c5a9 !important' },
  '.cm-selectionMatch': { backgroundColor: '#e8e0d0' },
  '.ͼ1.cm-focused': { outline: 'none' },
}, { dark: false })

// 占位文本扩展
const placeholderExt = EditorView.updateListener.of((update) => {
  if (update.view.state.doc.toString() === '') {
    update.view.dom.setAttribute('data-placeholder', '从左侧知识树选择一个知识点，AI 自动生成代码...')
  } else {
    update.view.dom.removeAttribute('data-placeholder')
  }
})

onMounted(() => {
  if (!container.value) return
  try {
    const updateListener = EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        emit('code-change', update.state.doc.toString())
      }
    })

    const state = EditorState.create({
      doc: props.code || '',
      extensions: [
        lineNumbers(),
        highlightActiveLineGutter(),
        highlightSpecialChars(),
        history(),
        drawSelection(),
        dropCursor(),
        EditorState.allowMultipleSelections.of(true),
        indentOnInput(),
        bracketMatching(),
        syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
        highlightActiveLine(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        langExtension(props.language || 'java'),
        updateListener,
        cmTheme,
        placeholderExt,
      ],
    })

    editorView = new EditorView({ state, parent: container.value })
  } catch (e) {
    console.error('CodeMirror 初始化失败:', e)
    initError.value = true
  }
})

// 外部代码变化时同步到编辑器
watch(() => props.code, (val) => {
  if (val === undefined || !editorView) return
  const current = editorView.state.doc.toString()
  if (val !== current) {
    editorView.dispatch({
      changes: { from: 0, to: editorView.state.doc.length, insert: val }
    })
  }
})

// 设置编辑器内容
function setValue(val: string) {
  if (editorView) {
    editorView.dispatch({
      changes: { from: 0, to: editorView.state.doc.length, insert: val || '' }
    })
  } else if (fallback.value) {
    fallback.value.value = val || ''
  }
}

// 获取编辑器内容
function getValue(): string {
  if (editorView) return editorView.state.doc.toString()
  if (fallback.value) return fallback.value.value
  return ''
}

// 触发布局刷新
function layout() {
  editorView?.requestMeasure()
}

onBeforeUnmount(() => {
  editorView?.destroy()
})

defineExpose({ setValue, getValue, layout })
</script>
<template>
  <div class="editor-wrapper">
    <div v-if="initError" class="editor-fallback">
      <textarea ref="fallback" class="fallback-textarea" :value="code" @input="emit('code-change', ($event.target as HTMLTextAreaElement).value)" placeholder="从左侧知识树选择一个知识点，AI 自动生成代码..."></textarea>
    </div>
    <div ref="container" class="cm-container" :class="{ hidden: initError }"></div>
  </div>
</template>
<style scoped>
.editor-wrapper { flex: 1; display: flex; min-height: 0; }
.cm-container { flex: 1; overflow: hidden; }
.cm-container.hidden { display: none; }
.editor-fallback { flex: 1; display: flex; min-height: 0; }
.fallback-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
  font-size: 14px;
  line-height: 1.6;
  background: #faf6ed;
  color: #1e293b;
}
.fallback-textarea::placeholder { color: #a09880; }
</style>
<style>
/* 占位文本伪元素（CodeMirror 原生不支持 placeholder） */
.cm-container .cm-content[data-placeholder]::before {
  content: attr(data-placeholder);
  color: #a09880;
  font-style: italic;
  pointer-events: none;
  position: absolute;
  padding: 12px 0 0 8px;
  font-size: 14px;
}
</style>
