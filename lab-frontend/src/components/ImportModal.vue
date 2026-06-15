<script setup lang="ts">
import { ref, reactive } from 'vue'
import { labApi } from '@/api/client'
const emit = defineEmits<{ close: [] }>()
const form = reactive({ title: '', description: '', trapCode: '', expectedPitfall: '', correctExplanation: '' })
const error = ref(''); const loading = ref(false)
async function submit() {
  if (!form.title.trim()) { error.value = '请输入标题'; return }
  loading.value = true; error.value = ''
  const res = await labApi.post('/questions/import', form)
  loading.value = false
  if (res.code === 200) emit('close'); else error.value = res.message || '导入失败'
}
</script>
<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <button class="modal-close" @click="emit('close')">✕</button>
      <h3 style="margin:0 0 12px;">手动导入题目</h3>
      <div style="display:flex;flex-direction:column;gap:8px;">
        <input v-model="form.title" placeholder="标题" style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:13px;" />
        <textarea v-model="form.description" rows="2" placeholder="描述" style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:inherit;resize:vertical;"></textarea>
        <textarea v-model="form.trapCode" rows="6" placeholder="陷阱代码" style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:12px;font-family:monospace;resize:vertical;"></textarea>
        <textarea v-model="form.expectedPitfall" rows="2" placeholder="预期陷阱" style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:inherit;resize:vertical;"></textarea>
        <textarea v-model="form.correctExplanation" rows="3" placeholder="正确解释" style="padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:inherit;resize:vertical;"></textarea>
      </div>
      <div v-if="error" style="color:var(--danger);font-size:12px;margin-top:6px;">{{ error }}</div>
      <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
        <button class="btn btn-outline" @click="emit('close')">取消</button>
        <button class="btn btn-primary" @click="submit" :disabled="loading">{{ loading ? '导入中...' : '导入' }}</button>
      </div>
    </div>
  </div>
</template>
