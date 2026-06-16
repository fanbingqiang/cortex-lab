<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useTreeStore } from '@/stores/tree'
import TreeNode from '@/components/TreeNode.vue'

const emit = defineEmits<{ 'select-node': [node: any] }>()
const tree = useTreeStore()
const searchQuery = ref('')

onMounted(() => { tree.loadTree() })

const filteredNodes = computed(() => {
  if (!searchQuery.value) return tree.nodes
  return tree.nodes.map(n => filterNode(n, searchQuery.value.toLowerCase())).filter(Boolean) as any[]
})

function filterNode(node: any, query: string): any {
  const nameMatch = node.name.toLowerCase().includes(query)
  const descMatch = node.description?.toLowerCase().includes(query)
  if (nameMatch || descMatch) return { ...node }
  if (node.children) {
    const filtered = node.children.map((c: any) => filterNode(c, query)).filter(Boolean)
    if (filtered.length) return { ...node, children: filtered }
  }
  return null
}
</script>
<template>
  <div class="knowledge-tree-root">
    <div class="tree-search-box">
      <input v-model="searchQuery" placeholder="搜索知识点..." style="width:100%;padding:6px 10px;border:1px solid var(--border);border-radius:6px;font-size:12px;outline:none;box-sizing:border-box;" />
    </div>
    <div class="tree-scroll">
      <div style="padding:4px 0;">
        <div v-if="tree.loading" style="padding:20px;text-align:center;color:var(--muted);font-size:13px;">加载中...</div>
        <template v-else>
          <TreeNode v-for="node in filteredNodes" :key="node.id" :node="node" :depth="0" :search-query="searchQuery" :mastered-nodes="tree.masteredNodeIds" @select-node="(n: any) => emit('select-node', n)" />
        </template>
      </div>
    </div>
  </div>
</template>
<style scoped>
.knowledge-tree-root {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.tree-search-box {
  flex-shrink: 0;
  padding: 8px 10px;
}
.tree-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 0 8px;
}
</style>
