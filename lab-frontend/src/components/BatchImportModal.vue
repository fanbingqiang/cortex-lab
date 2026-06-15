<script setup lang="ts">
import { ref } from 'vue'
import { labApi } from '@/api/client'
const emit = defineEmits<{ close: [] }>()
const text = ref(''); const category = ref(''); const error = ref(''); const loading = ref(false); const result = ref<number | null>(null)
async function submit() {
  if (!text.value.trim()) { error.value = '请输入题目内容'; return }
  loading.value = true; error.value = ''
  const res = await labApi.post<{count: number}>('/questions/batch-import', { text: text.value, category: category.value || '批量导入' })
  loading.value = false
  if (res.code === 200 && res.data) result.value = res.data.count; else error.value = res.message || '导入失败'
}
</script>
<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <button class="modal-close" @click="emit('close')">✕</button>
      <h3 style="margin:0 0 12px;">批量导入</h3>
      <input v-model="category" placeholder="分类（默认：批量导入）" style="width:100%;padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:13px;margin-bottom:8px;box-sizing:border-box;" />
      <textarea v-model="text" rows="10" placeholder="粘贴题目内容..." style="width:100%;padding:8px;border:1px solid var(--border);border-radius:6px;font-size:13px;font-family:inherit;resize:vertical;box-sizing:border-box;"></textarea>
      <div v-if="result !== null" style="font-size:13px;margin-top:8px;color:var(--success);">成功导入 {{ result }} 道题</div>
      <div v-if="error" style="color:var(--danger);font-size:12px;margin-top:6px;">{{ error }}</div>
      <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px;">
        <button v-if="result === null" class="btn btn-outline" @click="emit('close')">取消</button>
        <button v-if="result === null" class="btn btn-primary" @click="submit" :disabled="loading">{{ loading ? '导入中...' : '导入' }}</button>
        <button v-if="result !== null" class="btn btn-primary" @click="emit('close')">完成</button>
      </div>
    </div>
  </div>
</template>
