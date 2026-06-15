import type { ApiResponse } from '@/types'

const LAB_PREFIX = '/api/lab'
const AUTH_PREFIX = '/api/auth'

function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('auth_token')
  if (token) headers['Authorization'] = 'Bearer ' + token
  return headers
}

async function request<T>(method: string, path: string, body?: unknown, prefix = LAB_PREFIX): Promise<ApiResponse<T>> {
  const res = await fetch(`${prefix}${path}`, { method, headers: getAuthHeaders(), body: body ? JSON.stringify(body) : undefined })
  return res.json()
}

export const labApi = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  del: <T>(path: string) => request<T>('DELETE', path),
  stream: (path: string, body: unknown) => fetch(`${LAB_PREFIX}${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json', ...getAuthHeaders() }, body: JSON.stringify(body) })
}

export const authApi = {
  get: <T>(path: string) => request<T>('GET', path, undefined, AUTH_PREFIX),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body, AUTH_PREFIX),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body, AUTH_PREFIX),
  del: <T>(path: string) => request<T>('DELETE', path, undefined, AUTH_PREFIX),
}

export { getAuthHeaders }


// ========== Task / Agent API ==========
const TASK_PREFIX = '/api'

export const taskApi = {
  create: (requirement: string, userId: string) =>
    request<any>('POST', '/task/create', { requirement, userId }, TASK_PREFIX),
  execute: (taskId: string, userId: string) =>
    request<any>('POST', '/task/execute', { taskId, userId }, TASK_PREFIX),
  run: (requirement: string, userId: string) =>
    request<any>('POST', '/task/run', { requirement, userId }, TASK_PREFIX),
  get: (taskId: string) =>
    request<any>('GET', `/task/${taskId}`, undefined, TASK_PREFIX),
}
