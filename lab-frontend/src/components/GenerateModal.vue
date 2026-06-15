<script setup lang="ts">
import { ref } from 'vue'
import { labApi } from '@/api/client'
const emit = defineEmits<{ close: [] }>()
const question = ref(''); const error = ref(''); const loading = ref(false)
async function submit() {
  if (!question.value.trim()) { error.value = '请输入问题'; return }
  loading.value = true; error.value = ''
  const res = await labApi.post('/questions/generate', { question: question.value })
  loading.value = false
  if (res.code === 200) emit('close'); else error.value = res.message || '生成失败'
}
</script>
<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <button class="modal-close" @click="emit('close')">✕</button>
      <h3 style="margin:0 0 12px;">AI 生成题目</h3>
      <textarea v-model="question" rows="4" placeholder="例如：HashMap 的 put 方法原理" style="width:100%;padding:8px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:inherit;resize:vertical;box-sizing:border-box;"></textarea>
      <div v-if="error" style="color:var(--danger);font-size:12px;margin-top:6px;">{{ error }}</div>
      <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
        <button class="btn btn-outline" @click="emit('close')">取消</button>
        <button class="btn btn-primary" @click="submit" :disabled="loading">{{ loading ? '生成中...' : '生成' }}</button>
      </div>
    </div>
  </div>
</template>
