<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuestionsStore } from '@/stores/questions'
import { useRouter } from 'vue-router'
import { labApi } from '@/api/client'
import type { CardDto } from '@/types'
const store = useQuestionsStore()
const router = useRouter()
const expandedId = ref<number | null>(null)
const editingId = ref<number | null>(null)
const editForm = ref<{ keyPoints: string; detailExplanation: string; commonPitfalls: string }>({ keyPoints: '', detailExplanation: '', commonPitfalls: '' })
onMounted(() => store.loadCards())
function toggle(id: number) { expandedId.value = expandedId.value === id ? null : id }
function startEdit(c: CardDto) {
  editingId.value = c.id; editForm.value = { keyPoints: c.keyPoints || '', detailExplanation: c.detailExplanation || '', commonPitfalls: c.commonPitfalls || '' }
}
async function saveEdit(c: CardDto) {
  const res = await labApi.put('/cards/' + c.id, { id: c.id, title: c.title, keyPoints: editForm.value.keyPoints, detailExplanation: editForm.value.detailExplanation, commonPitfalls: editForm.value.commonPitfalls })
  if (res.code === 200) { Object.assign(c, editForm.value); editingId.value = null }
}
function cancelEdit() { editingId.value = null }
function practice(c: CardDto) {
  router.push('/practice')
  setTimeout(() => window.dispatchEvent(new CustomEvent('load-code', { detail: { code: c.codeSnippet, title: c.title } })), 100)
}
</script>
<template>
  <div style="flex:1;overflow-y:auto;padding:20px 24px;max-width:960px;margin:0 auto;">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <h2 style="font-size:18px;font-weight:600;margin:0;">知识卡片</h2>
      <span style="font-size:12px;color:var(--muted);">{{ store.cards.length }} 张</span>
    </div>
    <div v-if="store.cards.length === 0" style="text-align:center;padding:60px;color:var(--muted);font-size:14px;">
      还没有知识卡片<br><span style="font-size:13px;">去题库中打开一道题，会自动生成卡片</span>
    </div>
    <div v-else style="display:flex;flex-direction:column;gap:8px;">
      <div v-for="c in store.cards" :key="c.id" style="background:var(--surface);border:1px solid var(--border);border-radius:10px;overflow:hidden;">
        <div @click="toggle(c.id)" style="padding:14px 16px;cursor:pointer;display:flex;align-items:center;gap:8px;">
          <span style="font-size:16px;transition:transform 0.2s;display:inline-block;" :style="expandedId === c.id ? 'transform:rotate(90deg)' : ''">▶</span>
          <div style="flex:1;min-width:0;">
            <div style="font-size:14px;font-weight:600;">{{ c.title }}</div>
            <div v-if="c.keyPoints" style="font-size:12px;color:var(--muted);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ c.keyPoints }}</div>
          </div>
        </div>
        <div v-if="expandedId === c.id" style="border-top:1px solid var(--border);padding:16px;">
          <template v-if="editingId === c.id">
            <div style="margin-bottom:12px;">
              <label style="font-size:12px;font-weight:600;color:var(--muted);">关键点</label>
              <input v-model="editForm.keyPoints" style="width:100%;padding:6px 8px;border:1px solid var(--border);border-radius:4px;font-size:13px;margin-top:4px;box-sizing:border-box;" />
            </div>
            <div style="margin-bottom:12px;">
              <label style="font-size:12px;font-weight:600;color:var(--muted);">详细解释</label>
              <textarea v-model="editForm.detailExplanation" rows="4" style="width:100%;padding:6px 8px;border:1px solid var(--border);border-radius:4px;font-size:13px;margin-top:4px;resize:vertical;box-sizing:border-box;"></textarea>
            </div>
            <div style="margin-bottom:12px;">
              <label style="font-size:12px;font-weight:600;color:var(--muted);">常见误区</label>
              <textarea v-model="editForm.commonPitfalls" rows="2" style="width:100%;padding:6px 8px;border:1px solid var(--border);border-radius:4px;font-size:13px;margin-top:4px;resize:vertical;box-sizing:border-box;"></textarea>
            </div>
            <div style="display:flex;gap:8px;">
              <button class="btn btn-sm btn-primary" @click="saveEdit(c)">保存</button>
              <button class="btn btn-sm btn-ghost" @click="cancelEdit">取消</button>
            </div>
          </template>
          <template v-else>
            <div v-if="c.detailExplanation" style="margin-bottom:12px;font-size:13px;line-height:1.7;white-space:pre-wrap;">{{ c.detailExplanation }}</div>
            <div v-if="c.codeSnippet" style="margin-bottom:12px;">
              <pre style="background:#1e293b;color:#e2e8f0;border-radius:6px;padding:10px;font-size:12px;overflow-x:auto;"><code>{{ c.codeSnippet }}</code></pre>
            </div>
            <div v-if="c.commonPitfalls" style="margin-bottom:12px;font-size:13px;color:var(--danger);white-space:pre-wrap;">⚠️ {{ c.commonPitfalls }}</div>
            <div style="display:flex;gap:6px;">
              <button class="btn btn-sm btn-primary" @click="practice(c)">▶ 去实战练习</button>
              <button class="btn btn-sm btn-ghost" @click="startEdit(c)">编辑</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
