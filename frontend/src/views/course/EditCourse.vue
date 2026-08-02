<template>
  <div class="edit-course">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>编辑课程</span>
        </div>
      </template>
      
      <el-form ref="courseFormRef" :model="courseForm" :rules="rules" label-width="100px">
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="courseForm.courseName" placeholder="请输入课程名称"></el-input>
        </el-form-item>
        <el-form-item label="课程编码" prop="courseCode">
          <el-input v-model="courseForm.courseCode" placeholder="请输入课程编码"></el-input>
        </el-form-item>
        <el-form-item label="学分" prop="credits">
          <el-input type="number" v-model.number="courseForm.credits" placeholder="请输入学分"></el-input>
        </el-form-item>
        <el-form-item label="课时" prop="hours">
          <el-input type="number" v-model.number="courseForm.hours" placeholder="请输入课时"></el-input>
        </el-form-item>
        <el-form-item label="所属院系" prop="deptId">
          <el-select v-model="courseForm.deptId" placeholder="请选择院系" style="width: 100%;">
            <el-option v-for="dept in departments" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select 
            v-if="isAdmin" 
            v-model="courseForm.teacherId" 
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
            v-model="currentTeacherName" 
            disabled
            placeholder="当前登录教师"
          ></el-input>
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input type="textarea" v-model="courseForm.description" placeholder="请输入课程描述" rows="3"></el-input>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-tag v-if="courseForm.auditStatus === 0" type="warning" effect="dark">待审核</el-tag>
          <el-tag v-else-if="courseForm.auditStatus === 1" type="success" effect="dark">已通过</el-tag>
          <el-tag v-else-if="courseForm.auditStatus === 2" type="danger" effect="dark">已拒绝</el-tag>
          <el-tag v-else type="info">未知</el-tag>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="启用状态" prop="status">
          <el-radio-group v-model="courseForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
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
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import store from '../../store'

const router = useRouter()
const route = useRoute()
const courseId = route.params.id
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

const courseForm = reactive({
  courseId: '',
  courseName: '',
  courseCode: '',
  credits: null,
  hours: null,
  deptId: '',
  teacherId: '',
  description: '',
  auditStatus: 0,
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

const getCourseInfo = async () => {
  try {
    const response = await api.getCourseById(courseId)
    const data = response.data
    courseForm.courseId = data.courseId
    courseForm.courseName = data.courseName
    courseForm.courseCode = data.courseCode
    courseForm.credits = data.credits
    courseForm.hours = data.hours
    courseForm.deptId = data.deptId
    courseForm.teacherId = data.teacherId
    courseForm.description = data.description
    courseForm.auditStatus = data.auditStatus
    courseForm.status = data.status
  } catch (error) {
    ElMessage.error(error.message || '获取课程信息失败')
  }
}

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
      const submitData = { ...courseForm }
      
      if (isTeacher.value && !isAdmin.value) {
        submitData.teacherId = store.user.teacherId
      }
      
      await api.updateCourse(submitData)
      ElMessage.success('更新课程成功')
      router.push('/home/course')
    } catch (error) {
      ElMessage.error(error.message || '更新课程失败')
    }
  })
}

const resetForm = () => {
  getCourseInfo()
}

const goBack = () => {
  router.push('/home/course')
}

onMounted(async () => {
  await loadDepartments()
  await loadTeachers()
  await getCourseInfo()
})
</script>

<style scoped>
.edit-course {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
