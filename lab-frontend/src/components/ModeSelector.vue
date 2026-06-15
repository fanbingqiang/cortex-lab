<template>
  <div class="mode-selector" v-if="visible">
    <div class="mode-tabs">
      <button
        v-for="mode in availableModes"
        :key="mode.key"
        class="mode-tab"
        :class="{ active: currentMode === mode.key }"
        @click="$emit('change', mode.key)"
      >
        {{ mode.icon }} {{ mode.label }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ModeInfo } from '@/types'

const props = defineProps<{
  currentMode: string
  hasQuestion: boolean
  hasKnowledgePoint: boolean
  hasScenario: boolean
  visible: boolean
}>()

defineEmits<{
  (e: 'change', mode: string): void
}>()

const allModes: ModeInfo[] = [
  { key: 'trap', label: '改错', icon: '✏️' },
]

const availableModes = computed(() => {
  return allModes
})
</script>

<style scoped>
.mode-selector { padding: 4px 0; border-bottom: 1px solid var(--border); }
.mode-tabs { display: flex; gap: 4px; overflow-x: auto; padding: 0 8px; }
.mode-tab { padding: 6px 14px; border: none; background: transparent; color: var(--muted); font-size: 13px; cursor: pointer; border-radius: 6px 6px 0 0; white-space: nowrap; transition: all 0.15s; }
.mode-tab:hover { background: #f1f5f9; color: var(--text); }
.mode-tab.active { background: #eff6ff; color: #2563eb; font-weight: 500; }
</style>
