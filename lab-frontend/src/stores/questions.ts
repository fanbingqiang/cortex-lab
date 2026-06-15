import { defineStore } from 'pinia'
import { ref } from 'vue'
import { labApi } from '@/api/client'
import type { QuestionDto, CardDto } from '@/types'

export const useQuestionsStore = defineStore('questions', () => {
  const questions = ref<QuestionDto[]>([])
  const selectedQuestion = ref<QuestionDto | null>(null)
  const cards = ref<CardDto[]>([])
  const loading = ref(false)

  async function loadQuestions(userId: string) {
    loading.value = true
    const res = await labApi.get<QuestionDto[]>(`/questions?userId=${userId}`)
    if (res.code === 200) questions.value = res.data || []
    loading.value = false
  }
  async function searchQuestions(keyword: string) {
    const res = await labApi.get<QuestionDto[]>(`/questions/search?keyword=${encodeURIComponent(keyword)}`)
    if (res.code === 200) questions.value = res.data || []
  }
  async function getQuestion(id: number, userId: string) {
    const res = await labApi.get<QuestionDto>(`/questions/${id}?userId=${userId}`)
    if (res.code === 200) { selectedQuestion.value = res.data; return res.data }
    return null
  }
  async function toggleMastered(id: number, mastered: boolean) {
    await labApi.post(`/questions/${id}/mastered`, { userId: localStorage.getItem('user_id') || 'anonymous', mastered })
  }
  async function deleteQuestion(id: number) {
    await labApi.del(`/questions/${id}`)
    questions.value = questions.value.filter(q => q.id !== id)
  }
  async function loadCards() {
    const res = await labApi.get<CardDto[]>('/cards')
    if (res.code === 200) cards.value = res.data || []
  }
  async function updateCard(id: number, dto: Partial<CardDto>) {
    await labApi.put(`/cards/${id}`, dto)
  }
  async function deleteCard(id: number) {
    await labApi.del(`/cards/${id}`)
    cards.value = cards.value.filter(c => c.id !== id)
  }
  return { questions, selectedQuestion, cards, loading, loadQuestions, searchQuestions, getQuestion, toggleMastered, deleteQuestion, loadCards, updateCard, deleteCard }
})
