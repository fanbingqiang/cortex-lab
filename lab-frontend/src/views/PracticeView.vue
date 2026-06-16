<template>
  <div class="practice-layout">
    <!-- ====== 左侧知识树面板 ====== -->
    <div class="tree-panel" :class="{ open: treePanelOpen }">
      <div class="tree-header">
        <span style="font-weight:600;font-size:14px;">Java 后端知识树</span>
        <button class="btn btn-ghost btn-sm" @click="treePanelOpen = false" title="收起侧边栏">&times;</button>
      </div>
      <KnowledgeTree @select-node="selectTreeNode" />
    </div>

    <!-- ====== 中间编辑器面板 ====== -->
    <div class="editor-panel">
      <!-- 编辑器头部 -->
      <div class="editor-header">
        <span v-if="!treePanelOpen" class="btn btn-ghost btn-sm" @click="treePanelOpen = true" style="cursor:pointer;font-size:16px;padding:0 4px;" title="展开侧边栏">&#9776;</span>
        <span style="font-size:12px;color:var(--muted);">
          {{ currentKnowledgePoint || '从左侧知识树选择一个知识点' }}
          <span v-if="currentContentType && currentContentType !== 'trap'" :class="'content-badge content-badge--' + currentContentType" style="margin-left:6px;padding:1px 6px;border-radius:3px;font-size:10px;background:#e8f4fd;color:#1976d2;">{{ contentTypeLabel }}</span>
        </span>
        <div style="flex:1"></div>
        <button class="btn btn-primary" @click="runCode" :disabled="!currentScenario && !isProjectMode || currentContentType === 'concept' || currentContentType === 'command'">&#9654; 运行代码</button>
        <button class="btn btn-outline" @click="resetCode" :disabled="!currentScenario && !isProjectMode">&#8634; 重置</button>
      </div>

      <!-- 编辑器 + 终端 -->
      <CodeEditor
        ref="editorRef"
        :code="editorCode"
        :language="editorLanguage"
        @code-change="onCodeChange"
      />
      <div class="terminal-panel">
        <div class="terminal-label">{{ currentContentType === 'concept' ? '概念讲解' : currentContentType === 'command' ? '命令演示' : '输出终端' }}</div>
        <pre class="terminal-output" id="terminal-output">{{ terminalOutput || '从知识树选择一个知识点，AI 自动生成学习内容' }}</pre>
      </div>

    </div>

    <!-- ====== 右侧 AI 助手聊天面板 ====== -->
    <GlobalChat
      class="chat-panel"
      :scenario="currentScenario"
      :session-id="sessionId"
      :current-knowledge-point="currentKnowledgePoint"
      :current-code="editorCode"
      :original-code="originalCode"
      :execution-output="terminalOutput"
      @action="handleAction"
    />

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useTreeStore } from '@/stores/tree'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useQuestionsStore } from '@/stores/questions'
import { labApi } from '@/api/client'
import type { ScenarioDto, KnowledgeNode, AiAction, ProjectFileDTO, QuestionDto } from '@/types'
import { SHOW_TOAST } from '@/symbols'
import KnowledgeTree from '@/components/KnowledgeTree.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import GlobalChat from '@/components/GlobalChat.vue'

// ---------- stores ----------
const router = useRouter()
const treeStore = useTreeStore()
const chatStore = useChatStore()
const authStore = useAuthStore()
const questionsStore = useQuestionsStore()
const showToast = inject(SHOW_TOAST) as (msg: string) => void

// ---------- refs ----------
const editorRef = ref<InstanceType<typeof CodeEditor> | null>(null)

// ---------- state ----------
const currentScenario = ref<ScenarioDto | null>(null)
const currentQuestionId = ref<number | null>(null)
const sessionId = ref<string | null>(null)
const originalCode = ref('')
const currentKnowledgePoint = ref('')
const currentNodeId = ref('')
const terminalOutput = ref('')
const treePanelOpen = ref(true)
const editorCode = ref('')
const editorLanguage = ref('java')
const currentMode = ref('trap')
const currentContentType = ref<string | null>(null)

// 项目模式相关
const isProjectMode = ref(false)
const currentProjectFiles = ref<ProjectFileDTO[]>([])
const currentProjectDir = ref('')
const currentFilePath = ref('')

// 编辑器就绪标志
const editorReady = ref(false)

// 内容类型标记
const contentTypeLabel = computed(() => {
  const labels: Record<string, string> = {
    concept: '概念讲解',
    command: '命令演示',
    algorithm: '算法练习',
    trap: '陷阱代码',
  }
  return labels[currentContentType.value || 'trap'] || '代码'
})

const generatingNode = ref(false)

// ---------- 选择树节点 ----------
async function selectTreeNode(node: KnowledgeNode) {
  if (!node.leaf) return

  currentKnowledgePoint.value = node.name
  currentNodeId.value = node.id
  isProjectMode.value = false
  currentScenario.value = null
  currentQuestionId.value = null
  terminalOutput.value = '正在生成内容，请稍候...'
  editorCode.value = ''
  currentContentType.value = null
  generatingNode.value = true

  try {
    if (node.type === 'project') {
      const project = await treeStore.generateProject(node.id)
      if (project) {
        isProjectMode.value = true
        currentContentType.value = 'project'
        const files = project.files || []
        currentProjectFiles.value = files
        currentProjectDir.value = project.projectName || ''
        if (files.length > 0) {
          currentFilePath.value = files[0].path
          editorCode.value = files[0].content
          originalCode.value = files[0].content
        }
      }
    } else {
      const scenario = await treeStore.generateFromNode(node.id)
      if (scenario) {
        currentScenario.value = scenario
        currentContentType.value = scenario.type || 'trap'
        currentQuestionId.value = scenario.id || null

        if (scenario.type === 'concept' || scenario.type === 'command') {
          terminalOutput.value = scenario.generatedContent || scenario.trapCode || ''
          editorLanguage.value = scenario.type === 'command' ? 'shell' : 'java'
          editorCode.value = '/** ' + node.name + ' — 请查看下方输出终端 */\n// ' + scenario.trapCode?.substring(0, 60)
        } else if (scenario.type === 'algorithm') {
          editorCode.value = scenario.trapCode || scenario.generatedContent || ''
          originalCode.value = editorCode.value
          editorLanguage.value = 'java'
          terminalOutput.value = '📐 算法练习 — 阅读下方代码，理解算法思路和实现'
        } else {
          editorCode.value = scenario.trapCode || ''
          originalCode.value = scenario.trapCode || ''
          editorLanguage.value = 'java'
          terminalOutput.value = ''
        }
        sessionId.value = 'session_' + Date.now()
      } else {
        terminalOutput.value = '未能生成内容，请检查 API Key 配置（小C助手 → 配置）'
        showToast('内容生成失败')
      }
    }
  } catch (e: any) {
    console.error('选择节点失败:', e)
    const msg = e.message || '加载失败'
    terminalOutput.value = '错误: ' + msg
    showToast(msg)
  } finally {
    generatingNode.value = false
  }
}

// ---------- 运行代码 ----------
async function runCode() {
  if (!currentScenario.value && !isProjectMode.value) return
  if (currentContentType.value === 'concept' || currentContentType.value === 'command') return

  const code = editorRef.value?.getValue() || editorCode.value
  terminalOutput.value = '正在执行...\n'

  try {
    const res = await labApi.post<{ stdout: string; stderr: string }>('/execute', {
      code,
      language: editorLanguage.value,
      knowledgePoint: currentKnowledgePoint.value,
    })
    if (res.code === 200 && res.data) {
      const output = res.data.stderr
        ? '编译/运行错误:\n' + res.data.stderr
        : res.data.stdout || '(无输出)'
      terminalOutput.value = output
    } else {
      terminalOutput.value = '执行失败: ' + (res.message || '未知错误')
    }
  } catch (e: any) {
    terminalOutput.value = '执行异常: ' + (e.message || '请求失败')
  }
}

// ---------- 重置代码 ----------
function resetCode() {
  if (originalCode.value) {
    editorCode.value = originalCode.value
    if (editorRef.value) {
      editorRef.value.setValue(originalCode.value)
    }
  }
}

// ---------- 代码变更回调 ----------
function onCodeChange(code: string) {
  editorCode.value = code
}

// ---------- AI 动作处理 ----------
function handleAction(action: AiAction) {
  if (!action || !action.type) return
  switch (action.type) {
    case 'switchTab':
      if (action.payload && typeof action.payload === 'string') {
        const routeMap: Record<string, string> = { practice: '/practice', questions: '/questions', cards: '/cards', community: '/community', progress: '/progress' }
        const names: Record<string, string> = { practice: '练习', questions: '题库', cards: '知识卡片', community: '社区陷阱', progress: '学习中心' }
        router.push(routeMap[action.payload] || '/practice')
        showToast('已跳转到' + (names[action.payload] || action.payload))
      }
      break
    case 'selectQuestion':
      if (action.payload) {
        router.push('/questions')
        showToast('已打开题库')
      }
      break
    case 'loadToEditor':
      if (action.payload && typeof action.payload === 'string') {
        editorCode.value = action.payload
        if (editorRef.value) editorRef.value.setValue(action.payload)
        showToast('代码已加载到编辑器')
      }
      break
    case 'loadQuestion':
      if (action.payload) {
        const qId = typeof action.payload === 'object' ? action.payload.questionId : action.payload
        loadQuestionToEditor(qId)
        showToast('正在加载题目...')
      }
      break
    case 'generateScenario':
      if (action.payload && typeof action.payload === 'string') {
        selectTreeNode({ id: action.payload, name: '', leaf: true })
        showToast('正在生成场景...')
      }
      break
    case 'runCode': runCode(); break
    case 'resetCode': resetCode(); showToast('代码已重置'); break
    case 'modifyCode':
      if (action.payload && typeof action.payload === 'object') {
        const { target, replacement } = action.payload as any
        if (target !== undefined && replacement !== undefined) {
          const current = editorRef.value?.getValue() || editorCode.value
          const newCode = current.replace(target, replacement)
          editorCode.value = newCode
          if (editorRef.value) editorRef.value.setValue(newCode)
          showToast('代码已修改')
        }
      }
      break
    case 'insertCode':
      if (action.payload && typeof action.payload === 'string') {
        const current = editorRef.value?.getValue() || editorCode.value
        const newCode = current + '\n' + action.payload
        editorCode.value = newCode
        if (editorRef.value) editorRef.value.setValue(newCode)
        showToast('代码已插入')
      }
      break
    case 'deleteCode':
      if (action.payload && typeof action.payload === 'string') {
        const current = editorRef.value?.getValue() || editorCode.value
        const newCode = current.replace(action.payload, '')
        editorCode.value = newCode
        if (editorRef.value) editorRef.value.setValue(newCode)
        showToast('代码已删除')
      }
      break
    default:
      console.warn('未知 action 类型:', action.type)
  }
}

// 根据题目ID加载代码到编辑器
async function loadQuestionToEditor(questionId: number) {
  try {
    const res = await labApi.get<QuestionDto>('/questions/' + questionId + '?userId=' + encodeURIComponent(authStore.userId || ''))
    if (res.code === 200 && res.data) {
      const q = res.data
      isProjectMode.value = false
      currentContentType.value = 'trap'
      editorCode.value = q.trapCode || ''
      originalCode.value = q.trapCode || ''
      currentKnowledgePoint.value = q.title || ''
      terminalOutput.value = ''
      // 创建虚拟场景使运行按钮可用
      currentScenario.value = {
        id: q.id, knowledgePoint: q.title, trapCode: q.trapCode || '',
        expectedPitfall: q.expectedPitfall, correctExplanation: q.correctExplanation,
        hints: undefined, difficulty: q.difficulty ?? undefined, type: 'trap' as any, generatedContent: undefined
      }
    }
  } catch (e: any) {
    console.error('加载题目失败:', e)
    terminalOutput.value = '加载题目失败: ' + (e.message || '')
  }
}

// ---------- 布局更新 ----------
function layoutEditor() {
  editorRef.value?.layout()
}

// ---------- 生命周期 ----------
onMounted(async () => {
  window.addEventListener('load-code', onLoadCode)
  // 从题库跳转过来的，把题目加载到编辑器
  if (questionsStore.selectedQuestion) {
    const q = questionsStore.selectedQuestion
    isProjectMode.value = false
    currentContentType.value = 'trap'
    editorCode.value = q.trapCode || ''
    originalCode.value = q.trapCode || ''
    currentKnowledgePoint.value = q.title || ''
    terminalOutput.value = ''
    currentScenario.value = {
      id: q.id, knowledgePoint: q.title, trapCode: q.trapCode || '',
      expectedPitfall: q.expectedPitfall, correctExplanation: q.correctExplanation,
      hints: undefined, difficulty: q.difficulty ?? undefined, type: 'trap' as any, generatedContent: undefined
    }
    questionsStore.selectedQuestion = null
  }
  if (authStore.isLoggedIn && authStore.userId) {
    await chatStore.loadConversations(authStore.userId)
  }
})

function onLoadCode(e: any) {
  const detail = e.detail || {}
  if (detail.code) {
    isProjectMode.value = false
    currentContentType.value = 'trap'
    editorCode.value = detail.code
    originalCode.value = detail.code
    currentKnowledgePoint.value = detail.title || ''
    terminalOutput.value = ''
    currentScenario.value = {
      id: undefined, knowledgePoint: detail.title || '', trapCode: detail.code,
      expectedPitfall: '', correctExplanation: '', hints: undefined,
      difficulty: undefined, type: 'trap' as any, generatedContent: undefined
    }
  }
}
</script>

<style scoped>
/* ====== 面板分割线 ====== */
.practice-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

/* ====== 编辑器头部按钮禁用态 ====== */
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ====== 终端输出 ====== */
.terminal-output {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* ====== 响应式：小屏折叠聊天面板 ====== */
@media (max-width: 900px) {
  .chat-panel {
    width: 100% !important;
    height: 260px;
    border-left: none;
    border-top: 1px solid var(--border);
  }
}
</style>
