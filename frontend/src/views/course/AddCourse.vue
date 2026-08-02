<template>
  <div class="add-course">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>添加课程</span>
        </div>
      </template>
      
      <el-form ref="courseFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称"></el-input>
        </el-form-item>
        <el-form-item label="课程编码" prop="courseCode">
          <el-input v-model="form.courseCode" placeholder="请输入课程编码"></el-input>
        </el-form-item>
        <el-form-item label="学分" prop="credits">
          <el-input type="number" v-model.number="form.credits" placeholder="请输入学分"></el-input>
        </el-form-item>
        <el-form-item label="课时" prop="hours">
          <el-input type="number" v-model.number="form.hours" placeholder="请输入课时"></el-input>
        </el-form-item>
        <el-form-item label="所属院系" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%;">
            <el-option v-for="dept in departments" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select 
            v-if="isAdmin" 
            v-model="form.teacherId" 
            placeholder="请选择授课教师" 
            style="width: 100%;"
          >
            <el-option 
              v-for="teacher in teachers" 
              :key="teacher.teacherId" 
              :label="teacher.realName" 
              :value="teacher.teacherId"
            ></el-option>
          </el-select>
          <el-input 
            v-else 
            :model-value="currentTeacherName" 
            disabled
            placeholder="当前登录教师"
          ></el-input>
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="请输入课程描述" rows="3"></el-input>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isAdmin" label="审核说明">
          <el-alert
            title="课程提交后将进入管理员审核流程，审核通过后学生方可选课"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import store from '../../store'

const router = useRouter()
const courseFormRef = ref(null)

const isAdmin = computed(() => {
  const roles = store.roles || []
  return roles.includes('ADMIN')
})

const isTeacher = computed(() => {
  const roles = store.roles || []
  return roles.includes('TEACHER')
})

const currentTeacherName = computed(() => {
  if (store.user && store.user.realName) {
    return store.user.realName
  }
  return '当前登录教师'
})

const form = reactive({
  courseName: '',
  courseCode: '',
  credits: null,
  hours: null,
  deptId: '',
  teacherId: '',
  description: '',
  status: 1
})

const rules = {
  courseName: [
    { required: true, message: '请输入课程名称', trigger: 'blur' }
  ],
  courseCode: [
    { required: true, message: '请输入课程编码', trigger: 'blur' }
  ],
  credits: [
    { required: true, message: '请输入学分', trigger: 'blur' }
  ],
  hours: [
    { required: true, message: '请输入课时', trigger: 'blur' }
  ],
  deptId: [
    { required: true, message: '请选择院系', trigger: 'change' }
  ]
}

const departments = ref([])
const teachers = ref([])

const loadDepartments = async () => {
  try {
    const response = await api.getDepartmentList()
    departments.value = response.data || []
  } catch (error) {
    ElMessage.error('获取院系列表失败')
  }
}

const loadTeachers = async () => {
  try {
    const response = await api.getTeacherList()
    teachers.value = response.data || []
  } catch (error) {
    ElMessage.error('获取教师列表失败')
  }
}

const submitForm = async () => {
  if (!courseFormRef.value) return
  
  await courseFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      const submitData = {
        courseName: form.courseName,
        courseCode: form.courseCode,
        credits: form.credits,
        hours: form.hours,
        deptId: form.deptId,
        teacherId: form.teacherId,
        description: form.description,
        status: form.status
      }
      
      if (isTeacher.value && !isAdmin.value) {
        submitData.teacherId = store.user.teacherId
      }
      
      await api.addCourse(submitData)
      ElMessage.success('添加课程成功')
      router.push('/home/course')
    } catch (error) {
      ElMessage.error(error.message || '添加课程失败')
    }
  })
}

const resetForm = () => {
  form.courseName = ''
  form.courseCode = ''
  form.credits = null
  form.hours = null
  form.deptId = ''
  form.teacherId = ''
  form.description = ''
  form.status = 1
  
  if (isTeacher.value && !isAdmin.value && store.user) {
    form.teacherId = store.user.teacherId
  }
}

const goBack = () => {
  router.push('/home/course')
}

onMounted(async () => {
  await loadDepartments()
  await loadTeachers()
  
  if (isTeacher.value && !isAdmin.value && store.user) {
    form.teacherId = store.user.teacherId
  }
})
</script>

<style scoped>
.add-course {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
