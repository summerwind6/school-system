<template>
  <div class="login-container">
    <div class="login-form">
      <h2 class="login-title">高校课程管理系统</h2>
      <div class="form-group">
        <label for="username">学号/工号</label>
        <input 
          type="text" 
          id="username" 
          v-model="username" 
          placeholder="请输入学号或教师工号"
          class="form-input"
        />
      </div>
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          type="password" 
          id="password" 
          v-model="password" 
          placeholder="请输入密码"
          class="form-input"
        />
      </div>
      <div class="form-group">
        <button @click="login" class="login-button">登录</button>
      </div>
      <div class="form-group">
        <button @click="register" class="register-button">注册</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import store from '../store'

const router = useRouter()
const username = ref('')
const password = ref('')

const login = async () => {
  if (!username.value) {
    ElMessage.error('请输入用户名')
    return
  }
  if (!password.value) {
    ElMessage.error('请输入密码')
    return
  }
  
  try {
    const response = await api.login({ username: username.value, password: password.value })
    localStorage.setItem('token', 'token')
    store.setUser(response.data)
    store.setRoles(response.data.roles || [])
    store.setPermissions(response.data.permissions || [])
    ElMessage.success('登录成功')
    
    const roles = response.data.roles || []
    if (roles.includes('ADMIN')) {
      router.push('/home/course')
    } else if (roles.includes('TEACHER')) {
      router.push('/home/score')
    } else if (roles.includes('STUDENT')) {
      router.push('/home/selection/select')
    } else {
      router.push('/home/dashboard')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  }
}

const register = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-form {
  width: 400px;
  padding: 30px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
}

.form-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.login-button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin-bottom: 10px;
}

.login-button:hover {
  background: #5a6fd8;
}

.register-button {
  width: 100%;
  padding: 12px;
  background: #f0f0f0;
  color: #333;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.register-button:hover {
  background: #e0e0e0;
}
</style>