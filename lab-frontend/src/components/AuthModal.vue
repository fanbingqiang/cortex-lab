<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
const emit = defineEmits<{ close: [] }>()
const auth = useAuthStore()
const isLogin = ref(true)
const form = ref({ username: '', password: '', confirmPassword: '', email: '' })
const errorMsg = ref('')
async function submit() {
  errorMsg.value = ''
  if (!form.value.username.trim() || !form.value.password.trim()) { errorMsg.value = '请填写用户名和密码'; return }
  if (!isLogin.value && form.value.password !== form.value.confirmPassword) { errorMsg.value = '两次密码不一致'; return }
  const res = isLogin.value ? await auth.login(form.value.username, form.value.password) : await auth.register(form.value.username, form.value.password, form.value.email)
  if (res.code === 200) emit('close'); else errorMsg.value = res.message || '操作失败'
}
</script>
<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-box">
      <button class="modal-close" @click="emit('close')">✕</button>
      <h3 style="margin:0 0 16px;">{{ isLogin ? '登录' : '注册' }}</h3>
      <div style="display:flex;flex-direction:column;gap:10px;">
        <input v-model="form.username" placeholder="用户名" style="padding:8px 12px;border:1px solid var(--border);border-radius:6px;font-size:13px;" />
        <input v-model="form.password" type="password" placeholder="密码" style="padding:8px 12px;border:1px solid var(--border);border-radius:6px;font-size:13px;" />
        <input v-if="!isLogin" v-model="form.confirmPassword" type="password" placeholder="确认密码" style="padding:8px 12px;border:1px solid var(--border);border-radius:6px;font-size:13px;" />
        <input v-if="!isLogin" v-model="form.email" placeholder="邮箱（可选）" style="padding:8px 12px;border:1px solid var(--border);border-radius:6px;font-size:13px;" />
      </div>
      <div v-if="errorMsg" style="color:var(--danger);font-size:12px;margin-top:8px;">{{ errorMsg }}</div>
      <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:16px;">
        <button class="btn btn-outline" @click="isLogin = !isLogin">{{ isLogin ? '去注册' : '去登录' }}</button>
        <button class="btn btn-primary" @click="submit">{{ isLogin ? '登录' : '注册' }}</button>
      </div>
    </div>
  </div>
</template>
