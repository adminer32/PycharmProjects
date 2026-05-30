import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { SystemConstant } from '@/constants/SystemConstant';
import ProfileApi from '@/api/ProfileApi';
import UserApi from '@/api/UserApi';
import type { TeacherProfile } from '@/types/TeacherProfile';

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem(SystemConstant.TOKEN) || null);
  const userInfo = ref<any>(null); // 后端登录返回的原始账号信息 (包含 username)
  const profile = ref<TeacherProfile | null>(null); // 后端获取到的教师详情档案

  // 动态头像
  const userAvatar = computed(() => {
    return profile.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
  });

  // 动态姓名显示
  const userName = computed(() => {
    return profile.value?.name || userInfo.value?.username || '未设置姓名';
  });

  // 动态账号显示 (用于 @username)
  const accountName = computed(() => {
    return userInfo.value?.username || 'unknown';
  });

  function setToken(newToken: string) {
    token.value = newToken;
    localStorage.setItem(SystemConstant.TOKEN, newToken);
  }

  function setUserInfo(info: any) {
    userInfo.value = info;
  }

  // 获取教师详细档案
  async function fetchMyProfile() {
    try {
      const res = await ProfileApi.getMyProfile();
      profile.value = res.data;
    } catch (e) {
      console.error('获取个人资料失败', e);
    }
  }

  // 获取基本账号信息 (username 等)
  async function fetchUserInfo() {
    try {
      const res = await UserApi.getUserInfo();
      userInfo.value = res.data;
    } catch (e) {
      console.error('获取基本账号信息失败', e);
    }
  }

  function logout() {
    token.value = null;
    userInfo.value = null;
    profile.value = null;
    localStorage.removeItem(SystemConstant.TOKEN);
    localStorage.removeItem('sa-token');
  }

  return { 
    token, 
    userInfo, 
    profile, 
    userAvatar, 
    userName, 
    accountName,
    setToken, 
    setUserInfo, 
    fetchMyProfile, 
    fetchUserInfo,
    logout 
  };
});
