<template>
  <div class="register-container">
    <div class="register-form">
      <h2 class="register-title">用户注册</h2>
      
      <!-- 角色选择 -->
      <div class="form-group">
        <label for="role">角色类型</label>
        <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
          <el-option label="学生" value="STUDENT"></el-option>
          <el-option label="教师" value="TEACHER"></el-option>
        </el-select>
      </div>

      <!-- 基本信息 -->
      <div class="form-group">
        <label for="password">密码</label>
        <el-input 
          v-model="form.password" 
          type="password" 
          placeholder="请输入密码（至少6位）"
          prefix-icon="Lock"
          show-password
        />
      </div>

      <div class="form-group">
        <label for="confirmPassword">确认密码</label>
        <el-input 
          v-model="form.confirmPassword" 
          type="password" 
          placeholder="请再次输入密码"
          prefix-icon="Lock"
          show-password
        />
      </div>

      <div class="form-group">
        <label for="phone">手机号码</label>
        <el-input 
          v-model="form.phone" 
          placeholder="请输入手机号码"
          prefix-icon="Phone"
        />
      </div>

      <!-- 学生特有信息 -->
      <template v-if="form.role === 'STUDENT'">
        <el-divider content-position="left">学生信息</el-divider>
        
        <div class="form-group">
          <label for="studentNo">学号</label>
          <el-input 
            v-model="form.studentNo" 
            placeholder="请输入学号"
            prefix-icon="Ticket"
          />
        </div>
      </template>

      <!-- 教师特有信息 -->
      <template v-if="form.role === 'TEACHER'">
        <el-divider content-position="left">教师信息</el-divider>
        
        <div class="form-group">
          <label for="teacherNo">教师工号</label>
          <el-input 
            v-model="form.teacherNo" 
            placeholder="请输入教师工号"
            prefix-icon="Ticket"
          />
        </div>

        <div class="form-group">
          <label for="realName">姓名</label>
          <el-input 
            v-model="form.realName" 
            placeholder="请输入姓名"
            prefix-icon="Postcard"
          />
        </div>
      </template>

      <div class="form-actions">
        <el-button type="primary" :loading="loading" @click="handleRegister" style="width: 100%; height: 42px; font-size: 16px;">
          注册
        </el-button>
        <div class="login-link">
          已有账号？<el-button type="text" @click="goToLogin">立即登录</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  role: 'STUDENT',
  password: '',
  confirmPassword: '',
  phone: '',
  // 学生信息
  studentNo: '',
  // 教师信息
  teacherNo: '',
  realName: ''
})

const validateForm = () => {
  if (!form.role) {
    ElMessage.warning('请选择角色类型')
    return false
  }
  
  if (!form.password || form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return false
  }
  
  if (form.password !== form.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return false
  }

  // 手机号格式验证
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return false
  }

  // 学生必填信息
  if (form.role === 'STUDENT') {
    if (!form.studentNo) {
      ElMessage.warning('请输入学号')
      return false
    }
  }

  // 教师必填信息
  if (form.role === 'TEACHER') {
    if (!form.teacherNo) {
      ElMessage.warning('请输入教师工号')
      return false
    }
    if (!form.realName) {
      ElMessage.warning('请输入姓名')
      return false
    }
  }

  return true
}

const handleRegister = async () => {
  if (!validateForm()) {
    return
  }

  loading.value = true
  
  try {
    // 转换role为roleType：STUDENT=1, TEACHER=2
    const roleType = form.role === 'STUDENT' ? 1 : 2
    
    const registerData = {
      password: form.password,
      phone: form.phone,
      roleType: roleType,
      // 学生信息
      studentNo: form.role === 'STUDENT' ? form.studentNo : undefined,
      // 教师信息
      teacherNo: form.role === 'TEACHER' ? form.teacherNo : undefined,
      realName: form.role === 'TEACHER' ? form.realName : undefined
    }

    await api.register(registerData)
    
    ElMessage.success('注册成功！即将跳转到登录页面...')
    
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-form {
  width: 480px;
  max-width: 90vw;
  padding: 30px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.register-title {
  text-align: center;
  margin-bottom: 25px;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.form-group {
  margin-bottom: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.form-actions {
  margin-top: 25px;
}

.login-link {
  text-align: center;
  margin-top: 15px;
  color: #909399;
  font-size: 14px;
}

.login-link .el-button {
  color: #409EFF;
  font-weight: 500;
}
</style>
