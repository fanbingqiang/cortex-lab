import { createRouter, createWebHashHistory } from 'vue-router'
const routes = [
  { path: '/', redirect: '/practice' },
  { path: '/practice', name: 'Practice', component: () => import('@/views/PracticeView.vue') },
  { path: '/questions', name: 'Questions', component: () => import('@/views/QuestionsView.vue') },
  { path: '/cards', name: 'Cards', component: () => import('@/views/CardsView.vue') },
  { path: '/community', name: 'Community', component: () => import('@/views/CommunityView.vue') },
  { path: '/progress', name: 'Progress', component: () => import('@/views/MyPage.vue') },
  { path: '/reports', name: 'Reports', component: () => import('@/views/MyPage.vue') },
]
export default createRouter({ history: createWebHashHistory('/lab/'), routes })
