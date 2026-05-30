import { createRouter, createWebHistory } from 'vue-router';
import { eventBus } from '@/utils/eventBus';
import { SystemConstant } from '@/constants/SystemConstant';
import { verifyTokenApi, clearTokens } from '@/api/auth/authApi';
import { useUserStore } from '@/stores/userStore';

const refreshTokenApi = async (): Promise<boolean> => {
    const access_token = localStorage.getItem('access_token');
    const refresh_token = localStorage.getItem('refresh_token');

    if (!access_token || !refresh_token) {
        return false;
    }

    try {
        const response = await fetch('/api/v0/student/auth/token/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                access_token,
                refresh_token,
                captcha: null
            }),
        });
        const result = await response.json();
        if (result.success) {
            localStorage.setItem('access_token', result.access_token);
            localStorage.setItem('refresh_token', result.refresh_token);
            return true;
        }
        return false;
    } catch (error) {
        console.error('Token refresh failed:', error);
        return false;
    }
};

const isLoggedIn = async (): Promise<boolean> => {
    const access_token = localStorage.getItem('access_token');
    const refresh_token = localStorage.getItem('refresh_token');
    
    if (!access_token && !refresh_token) {
        return false;
    }

    const result = await verifyTokenApi();
    
    if (!result.access_token_valid && result.refresh_token_valid) {
        const refreshed = await refreshTokenApi();
        if (!refreshed) {
            clearTokens();
            return false;
        }
        return true;
    }
    
    if (!result.access_token_valid && !result.refresh_token_valid) {
        clearTokens();
        return false;
    }
    return true;
};

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            redirect: { name: 'homeView' },
        },
        {
            path: '/login',
            name: 'login',
            component: () => import('@/views/LoginView/LoginView.vue'),
            meta: {
                title: '登录',
                requiresAuth: false,
            },
        },
        {
            path: '/register',
            name: 'register',
            component: () => import('@/views/LoginView/LoginView.vue'),
            meta: {
                title: '注册',
                requiresAuth: false,
            },
        },
        {
            path: '/home',
            name: 'homeView',
            component: () => import('@/views/HomeView/HomeView.vue'),
            meta: {
                title: 'home_view.title',
            },
        },
        {
            path: '/motion_demo',
            name: 'motionDemoView',
            component: () =>
                import('@/views/MotionDemoView/MotionDemoView.vue'),
            meta: {
                title: 'motion_demo_view.title',
                requiresAuth: true,
            },
        },
        {
            path: '/ai_learn_friend',
            name: 'aiLearnFriendView',
            component: () =>
                import('@/views/AILearnFriendView/AILearnFriendView.vue'),
            meta: {
                title: 'ai_learn_friend_view.title',
                requiresAuth: true,
            },
        },

        {
            path: '/motion_assessment',
            name: 'motionAssessmentView',
            component: () =>
                import('@/views/PersonalAnalysisView/PersonalAnalysisView.vue'),
            meta: {
                title: 'motion_assessment_view.title',
                requiresAuth: true,
            },
        },

        {
            path: '/personal_homepage',
            name: 'personalHomepageView',
            component: () =>
                import('@/views/PersonalHomepageView/PersonalHomepage.vue'),
            meta: {
                title: 'personal_homepage_view.title',
                requiresAuth: true,
            },
        },


        {
            path: '/smart_class',
            name: 'SmartClassView',
            component: () =>
                import('@/views/SmartClassView/SmartClass.vue'),
            meta: {
                title: 'smart_class_view.title',
                requiresAuth: true,
            },
        },
    ],
});

router.beforeEach(async (to) => {
    eventBus.emit(
        SystemConstant.Event.DocumentTitleChangeEvent,
        to.meta.title as string,
    );

    if (to.meta.requiresAuth !== false) {
        if (!(await isLoggedIn())) {
            return { name: 'login' };
        }
        const userStore = useUserStore();
        if (!userStore.myInfo.userId) {
            await userStore.getMyInfo();
        }
    }

    return true;
});

export default router;