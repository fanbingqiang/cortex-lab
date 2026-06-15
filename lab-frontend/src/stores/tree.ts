import { defineStore } from 'pinia'
import { ref } from 'vue'
import { labApi } from '@/api/client'
import type { KnowledgeNode, ScenarioDto, ProjectInfoDTO } from '@/types'

export const useTreeStore = defineStore('tree', () => {
  const nodes = ref<KnowledgeNode[]>([])
  const masteredNodeIds = ref<Set<string>>(new Set())
  const loading = ref(false)

  async function loadTree() {
    loading.value = true
    const [treeRes, masterRes] = await Promise.all([
      labApi.get<KnowledgeNode[]>('/knowledge-tree'),
      labApi.get<string[]>('/knowledge-tree/mastered')
    ])
    if (treeRes.code === 200) nodes.value = treeRes.data || []
    if (masterRes.code === 200) masteredNodeIds.value = new Set(masterRes.data || [])
    loading.value = false
  }
  async function toggleMaster(nodeId: string, mastered: boolean) {
    await labApi.post('/knowledge-tree/master', { nodeId, mastered, userId: localStorage.getItem('user_id') || 'anonymous' })
    if (mastered) masteredNodeIds.value.add(nodeId); else masteredNodeIds.value.delete(nodeId)
  }
  async function generateFromNode(nodeId: string) {
    const res = await labApi.post<ScenarioDto>('/knowledge-tree/generate', { nodeId })
    return res.code === 200 ? res.data : null
  }
  async function generateProject(nodeId: string) {
    const res = await labApi.post<ProjectInfoDTO>('/knowledge-tree/project', { nodeId })
    return res.code === 200 ? res.data : null
  }
  return { nodes, masteredNodeIds, loading, loadTree, toggleMaster, generateFromNode, generateProject }
})
