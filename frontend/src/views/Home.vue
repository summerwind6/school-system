<template>
  <div class="home-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <h3>高校课程管理系统</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/home/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <!-- 课程管理（仅教师和管理员可见） -->
        <el-sub-menu v-if="hasRole(['TEACHER', 'ADMIN'])" index="course">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>课程管理</span>
          </template>
          <el-menu-item index="/home/course">课程列表</el-menu-item>
          <el-menu-item v-if="hasRole(['TEACHER', 'ADMIN'])" index="/home/course/add">添加课程</el-menu-item>
          <el-menu-item v-if="hasRole(['ADMIN'])" index="/home/course/audit">课程审核</el-menu-item>
        </el-sub-menu>
        
        <!-- 选课管理（仅学生可见） -->
        <el-sub-menu v-if="hasRole(['STUDENT'])" index="selection">
          <template #title>
            <el-icon><Check /></el-icon>
            <span>选课管理</span>
          </template>
          <el-menu-item index="/home/selection">选课记录</el-menu-item>
          <el-menu-item index="/home/selection/select">选课</el-menu-item>
        </el-sub-menu>
        
        <!-- 成绩管理（仅教师和管理员可见） -->
        <el-sub-menu v-if="hasRole(['TEACHER', 'ADMIN'])" index="score">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>成绩管理</span>
          </template>
          <el-menu-item index="/home/score">成绩列表</el-menu-item>
          <el-menu-item v-if="hasRole(['TEACHER', 'ADMIN'])" index="/home/score/add">添加成绩</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-menu :default-active="''" mode="horizontal" class="header-menu">
            <el-menu-item index="">
              <el-icon><View /></el-icon>
              <span>首页</span>
            </el-menu-item>
          </el-menu>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="''">{{ user?.realName?.charAt(0) || 'U' }}</el-avatar>
              <span style="margin-left: 10px">{{ user?.realName || '用户' }}</span>
              <el-tag size="small" style="margin-left: 10px" :type="getRoleTagType()">
                {{ getRoleName() }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区 -->
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Document, Check, DataAnalysis, View, Stamp } from '@element-plus/icons-vue'
import store from '../store'
import { api } from '../api'

const router = useRouter()
const route = useRoute()
const user = computed(() => store.user)
const userRoles = computed(() => store.roles || [])

const activeMenu = computed(() => {
  return route.path
})

const hasRole = (roles) => {
  if (!userRoles.value || userRoles.value.length === 0) {
    return false
  }
  return roles.some(role => userRoles.value.includes(role))
}

const getRoleName = () => {
  if (userRoles.value.includes('ADMIN')) {
    return '管理员'
  } else if (userRoles.value.includes('TEACHER')) {
    return '教师'
  } else if (userRoles.value.includes('STUDENT')) {
    return '学生'
  }
  return '未知'
}

const getRoleTagType = () => {
  if (userRoles.value.includes('ADMIN')) {
    return 'danger'
  } else if (userRoles.value.includes('TEACHER')) {
    return 'warning'
  } else if (userRoles.value.includes('STUDENT')) {
    return 'success'
  }
  return 'info'
}

const handleMenuSelect = (key) => {
  router.push(key)
}

const handleLogout = async () => {
  try {
    await api.logout()
    store.clear()
    ElMessage.success('退出成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || '退出失败')
  }
}

onMounted(async () => {
  if (!store.user) {
    try {
      const response = await api.getCurrentUser()
      const userData = response.data
      store.setUser(userData)
      
      if (userData.roles) {
        store.setRoles(userData.roles)
      }
    } catch (error) {
      console.error('获取用户信息失败', error)
      router.push('/login')
    }
  }
})
</script>

<style scoped>
.home-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background-color: #303133;
  color: white;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid #409eff;
  margin-bottom: 20px;
}

.logo h3 {
  color: white;
  margin: 0;
  font-size: 18px;
}

.el-menu-vertical-demo {
  background-color: transparent;
  border-right: none;
}

.el-menu-vertical-demo :deep(.el-menu-item),
.el-menu-vertical-demo :deep(.el-sub-menu__title) {
  color: white;
}

.el-menu-vertical-demo :deep(.el-sub-menu .el-menu) {
  background-color: #1f2023 !important;
}

.el-menu-vertical-demo :deep(.el-sub-menu .el-menu .el-menu-item) {
  color: #c0c4cc;
  padding-left: 50px !important;
}

.el-menu-vertical-demo :deep(.el-sub-menu .el-menu .el-menu-item:hover) {
  background-color: #3a3d41 !important;
  color: white;
}

.el-menu-vertical-demo :deep(.el-sub-menu .el-menu .el-menu-item.is-active) {
  background-color: #409eff !important;
  color: white;
}

.el-menu-vertical-demo :deep(.el-menu-item.is-active) {
  background-color: #409eff;
}

.el-menu-vertical-demo :deep(.el-sub-menu__title:hover) {
  background-color: #3a3d41;
}

.el-menu-vertical-demo :deep(.el-menu-item:not(.is-active):hover) {
  background-color: #3a3d41;
}

.el-container {
  margin-left: 200px;
  width: calc(100% - 200px);
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  background-color: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-menu {
  border-bottom: none;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}
</style>
