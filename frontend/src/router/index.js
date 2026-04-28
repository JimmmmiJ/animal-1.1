import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/login'
      },
      {
        path: 'health-data',
        name: 'HealthData',
        component: () => import('@/views/HealthDataCollection.vue')
      },
      {
        path: 'breeding',
        name: 'Breeding',
        component: () => import('@/views/BreedingPeriodDetection.vue')
      },
      {
        path: 'disease',
        name: 'Disease',
        component: () => import('@/views/DiseaseEarlyWarning.vue')
      },
      {
        path: 'behavior',
        name: 'Behavior',
        component: () => import('@/views/FeedingBehaviorAnalysis.vue')
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/HealthDashboard.vue')
      },
      {
        path: 'alerts',
        name: 'Alerts',
        component: () => import('@/views/AlertManagement.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/UserPermission.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
