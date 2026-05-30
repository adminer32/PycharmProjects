import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import AppLayout from '@/components/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '用户登录' }
    },
    {
      path: '/',
      redirect: '/home',
      component: AppLayout,
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/Home/index.vue'),
          meta: { title: '首页 - 我的学习工作台', requireAuth: true }
        },
        {
          path: 'classroom',
          name: 'Classroom',
          component: () => import('@/views/Classroom/index.vue'),
          meta: { title: '课堂管理 - 视频回放', requireAuth: true }
        },
        {
          path: 'learning',
          name: 'Learning',
          component: () => import('@/views/Learning/index.vue'),
          meta: { title: '学习管理 - AI课堂', requireAuth: true }
        },
        {
          path: 'analysis',
          name: 'Analysis',
          component: () => import('@/views/analysis/index.vue'),
          meta: { title: '学情分析', requireAuth: true }
        },
        {
          path: 'homework',
          name: 'Homework',
          component: () => import('@/views/Homework/index.vue'),
          meta: { title: '作业管理', requireAuth: true }
        },
        {
          path: 'homework/grade',
          name: 'GradeHomework',
          component: () => import('@/views/Homework/GradeHomework.vue'),
          meta: { title: '批改作业', requireAuth: true }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/Profile.vue'),
          meta: { title: '个人信息', requireAuth: true }
        }
      ]
    }
  ],
})

// 路由守卫：标题设置与权限校验
router.beforeEach((to, from, next) => {
  // 设置标题
  if (to.meta.title) {
    document.title = (to.meta.title as string) + ' - 毽球智能平台';
  }

  // 检查是否需要鉴权
  const userStore = useUserStore();
  if (to.matched.some(record => record.meta.requireAuth)) {
    if (!userStore.token) {
      next({ path: '/login' });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router
