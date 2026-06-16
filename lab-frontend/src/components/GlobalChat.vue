<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { labApi } from '@/api/client'
import { useSSE } from '@/composables/useSSE'
import ConfigPanel from '@/components/ConfigPanel.vue'
import { renderMarkdown } from '@/utils/markdown'
import 'highlight.js/styles/github.css'

const emit = defineEmits<{ action: [action: any] }>()
const props = withDefaults(defineProps<{
  scenario?: any
  sessionId?: string | null
  currentKnowledgePoint?: string
  currentCode?: string
  originalCode?: string
  executionOutput?: string
}>(), {
  currentCode: '',
  originalCode: '',
  executionOutput: '',
})
const auth = useAuthStore()
const chatStore = useChatStore()

const showConfig = ref(false)
const inputMsg = ref('')
const chatMessages = ref<{ role: string; content: string }[]>([])
const msgContainer = ref<HTMLDivElement>()
const conversationChips = ref<any[]>([])
const currentConvId = ref<string | null>(null)

const sse = useSSE()
const currentSuggestions = ref<string[]>([])
const welcomeLoaded = ref(false)

onMounted(async () => {
  if (auth.isLoggedIn) chatStore.loadConversations(auth.userId)
  await loadWelcome()
})

async function loadWelcome() {
  if (welcomeLoaded.value) return
  try {
    const uid = auth.userId || 'anonymous'
    const res = await labApi.get<any>('/assistant/welcome?userId=' + encodeURIComponent(uid))
    if (res.code === 200 && res.data) {
      chatMessages.value.push({ role: 'assistant', content: res.data.reply || '' })
      currentSuggestions.value = res.data.suggestions || []
      welcomeLoaded.value = true
    }
  } catch {
    chatMessages.value.push({ role: 'assistant', content: '你好！我是小C助手，可以帮你学习 Java 编程。' })
    welcomeLoaded.value = true
  }
}

watch(() => chatStore.conversations, (val) => { conversationChips.value = val }, { immediate: true })

// 流式更新：后端已提取出纯 reply 文本，直接追加到消息末尾
watch(() => sse.text.value, () => {
  if (chatMessages.value.length) {
    const last = chatMessages.value[chatMessages.value.length - 1]
    if (last.role === 'assistant') {
      last.content = sse.text.value
    }
  }
})

watch(() => sse.metadata.value, (meta: any) => {
  if (meta.conversationId && !currentConvId.value) {
    currentConvId.value = meta.conversationId
    if (auth.isLoggedIn) chatStore.loadConversations(auth.userId)
  }
  if (meta.action) {
    // 导航/执行类操作：仅在没有文字内容时移除消息
    const silentTypes = ['switchTab', 'loadQuestion', 'loadToEditor', 'runCode', 'resetCode', 'generateScenario']
    if (silentTypes.includes(meta.action.type)) {
      const last = chatMessages.value[chatMessages.value.length - 1]
      // 已流式输出文字时不弹出，仅在无文字或 silent=true 时移除
      if (last?.role === 'assistant' && (!last.content || meta.silent)) {
        chatMessages.value.pop()
      }
    }
    emit('action', meta.action)
  }
  if (meta.suggestions && Array.isArray(meta.suggestions)) {
    currentSuggestions.value = meta.suggestions
  }
})

// 流式完成后，确保内容最终显示
watch(() => sse.state.value, (state) => {
  if (state === 'done' && chatMessages.value.length) {
    const last = chatMessages.value[chatMessages.value.length - 1]
    if (last.role === 'assistant' && !last.content) {
      last.content = sse.text.value || '（暂无回复）'
    }
    scrollToBottom()
  }
  if (state === 'error' && chatMessages.value.length) {
    const last = chatMessages.value[chatMessages.value.length - 1]
    if (last.role === 'assistant' && !last.content) {
      last.content = sse.error.value || '对话失败，请检查 API Key 配置'
    }
  }
})

/** 点击建议标签，自动填入输入框并发送 */
function clickSuggestion(text: string) {
  inputMsg.value = text
  sendMessage()
}

async function sendMessage() {
  if (!inputMsg.value.trim() || sse.state.value === 'streaming') return
  const msg = inputMsg.value; inputMsg.value = ''
  chatMessages.value.push({ role: 'user', content: msg })
  chatMessages.value.push({ role: 'assistant', content: '' })
  scrollToBottom()
  await sse.send('/api/lab/assistant/chat/stream', {
    conversationId: currentConvId.value,
    message: msg, userId: auth.userId || 'anonymous',
    currentCode: props.currentCode,
    knowledgePoint: props.currentKnowledgePoint || '',
    originalCode: props.originalCode,
    executionOutput: props.executionOutput,
  })
}

async function switchConv(conv: any) {
  currentConvId.value = conv.conversationId
  await chatStore.loadMessages(conv.conversationId)
  chatMessages.value = chatStore.messages.map(m => ({ role: m.role, content: m.content }))
}

function startNewConv() {
  currentConvId.value = null; chatMessages.value = []
}

function deleteConv(convId: string) {
  chatStore.deleteConv(convId)
  if (currentConvId.value === convId) startNewConv()
}

function scrollToBottom() {
  setTimeout(() => { if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight }, 50)
}

</script>
<template>
  <div class="chat-panel">
    <div class="chat-header">
      <span style="width:8px;height:8px;border-radius:50%;background:var(--success);display:inline-block;"></span>
      小C助手
      <div style="flex:1"></div>
      <button class="btn btn-ghost btn-sm" @click="showConfig = !showConfig">配置</button>
    </div>
    <ConfigPanel v-if="showConfig" />
    <div class="conv-selector">
      <span v-for="c in conversationChips" :key="c.conversationId" class="conv-chip" :class="{ active: c.conversationId === currentConvId }" @click="switchConv(c)">
        {{ c.title || c.conversationId?.slice(0,8) || '对话' }}
        <span class="conv-close" @click.stop="deleteConv(c.conversationId)">&times;</span>
      </span>
      <span class="conv-chip new" @click="startNewConv">+ 新对话</span>
    </div>
    <div class="chat-messages" ref="msgContainer">
      <div v-for="(msg, i) in chatMessages" :key="i" class="chat-msg" :class="msg.role">
        <div class="msg-content" v-html="renderMarkdown(msg.content)"></div>
        <div v-if="!msg.content && i === chatMessages.length - 1 && sse.state === 'streaming'" class="msg-content streaming">正在思考...</div>
        <div v-if="msg.role === 'assistant' && i === chatMessages.length - 1 && sse.state === 'done' && currentSuggestions.length > 0" class="suggestions-row">
          <span v-for="(s, si) in currentSuggestions" :key="si" class="suggestion-chip" @click="clickSuggestion(s)">{{ s }}</span>
        </div>
      </div>
    </div>
    <div class="chat-input-area">
      <input v-model="inputMsg" @keydown.enter="sendMessage" placeholder="问小C助手..." :disabled="sse.state === 'streaming'" />
      <button class="btn btn-primary btn-sm" @click="sendMessage" :disabled="sse.state === 'streaming' || !inputMsg.trim()">发送</button>
    </div>
  </div>
</template>

<style scoped>
.suggestions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px 4px;
}
.conv-close {
  margin-left: 2px;
  font-size: 16px;
  line-height: 1;
  color: #999;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  user-select: none;
}
.conv-close:hover {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.08);
}
:deep(.msg-content) {
  line-height: 1.7;
  padding: 0 12px;
  font-size: 14px;
  color: #1f2328;
}
:deep(.msg-content pre) {
  background: #1e293b;
  color: #e2e8f0;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.45;
}
:deep(.msg-content code) {
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 13px;
}
:deep(.msg-content p > code) {
  background: #f6f8fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #cf222e;
}
:deep(.msg-content p) {
  margin: 8px 0;
  white-space: pre-wrap;
}
:deep(.msg-content ul),
:deep(.msg-content ol) {
  padding-left: 20px;
  margin: 8px 0;
}
:deep(.msg-content h1),
:deep(.msg-content h2),
:deep(.msg-content h3),
:deep(.msg-content h4) {
  margin: 12px 0 6px;
  font-weight: 600;
}
:deep(.msg-content blockquote) {
  border-left: 3px solid #d0d7de;
  padding-left: 12px;
  color: #57606a;
  margin: 8px 0;
}
:deep(.msg-content a) {
  color: #0969da;
  text-decoration: none;
}
:deep(.msg-content a:hover) {
  text-decoration: underline;
}
</style>
