import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import store from '../store'
import { api } from '../api'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { requireAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { requireAuth: false }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requireAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requireAuth: true }
      },
      // 课程管理
      {
        path: 'course',
        name: 'Course',
        component: () => import('../views/course/CourseList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'course/add',
        name: 'AddCourse',
        component: () => import('../views/course/AddCourse.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'course/edit/:id',
        name: 'EditCourse',
        component: () => import('../views/course/EditCourse.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'course/audit',
        name: 'CourseAudit',
        component: () => import('../views/course/CourseAudit.vue'),
        meta: { requireAuth: true, roles: ['ADMIN'] }
      },
      // 选课管理（仅学生）
      {
        path: 'selection',
        name: 'Selection',
        component: () => import('../views/selection/SelectionList.vue'),
        meta: { requireAuth: true, roles: ['STUDENT'] }
      },
      {
        path: 'selection/select',
        name: 'SelectCourse',
        component: () => import('../views/selection/SelectCourse.vue'),
        meta: { requireAuth: true, roles: ['STUDENT'] }
      },
      // 成绩管理
      {
        path: 'score',
        name: 'Score',
        component: () => import('../views/score/ScoreList.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'score/add',
        name: 'AddScore',
        component: () => import('../views/score/AddScore.vue'),
        meta: { requireAuth: true }
      },
      {
        path: 'score/edit/:id',
        name: 'EditScore',
        component: () => import('../views/score/EditScore.vue'),
        meta: { requireAuth: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from) => {
  if (!to.meta.requireAuth) return true
  if (!localStorage.getItem('token')) return '/login'

  if (!store.user) {
    try {
      const { data: user } = await api.getCurrentUser()
      store.setUser(user)
      store.setRoles(user.roles || [])
      store.setPermissions(user.permissions || [])
    } catch {
      store.clear()
      return '/login'
    }
  }

  if (to.meta.roles && !to.meta.roles.some(role => store.roles.includes(role))) {
    ElMessage.error('您没有权限访问该页面')
    return from.matched.length ? false : '/home/dashboard'
  }
  return true
})

export default router
