<template>
  <div class="add-score">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>添加成绩</span>
        </div>
      </template>
      
      <el-form ref="scoreFormRef" :model="scoreForm" :rules="rules" label-width="100px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="scoreForm.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option
              v-for="course in courses"
              :key="course.courseId"
              :label="`${course.courseName} (${course.courseCode})`"
              :value="course.courseId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学生学号" prop="studentNo">
          <el-input v-model="scoreForm.studentNo" placeholder="请输入学生学号"></el-input>
        </el-form-item>
        <el-form-item label="平时成绩" prop="usualScore">
          <el-input type="number" v-model.number="scoreForm.usualScore" placeholder="请输入平时成绩"></el-input>
        </el-form-item>
        <el-form-item label="期末成绩" prop="finalScore">
          <el-input type="number" v-model.number="scoreForm.finalScore" placeholder="请输入期末成绩"></el-input>
        </el-form-item>
        <el-form-item label="总评成绩" prop="totalScore">
          <el-input type="number" v-model.number="scoreForm.totalScore" placeholder="请输入总评成绩" disabled></el-input>
        </el-form-item>
        <el-form-item label="评语" prop="comment">
          <el-input type="textarea" v-model="scoreForm.comment" placeholder="请输入评语" rows="3"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="scoreForm.status">
            <el-radio label="1">已评定</el-radio>
            <el-radio label="0">未评定</el-radio>
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'
import store from '../../store'

const router = useRouter()
const scoreFormRef = ref()
const courses = ref([])

const scoreForm = reactive({
  courseId: '',
  studentNo: '',
  usualScore: null,
  finalScore: null,
  totalScore: null,
  comment: '',
  status: 1
})

const rules = {
  courseId: [
    { required: true, message: '请选择课程', trigger: 'change' }
  ],
  studentNo: [
    { required: true, message: '请输入学生学号', trigger: 'blur' }
  ],
  usualScore: [
    { required: true, message: '请输入平时成绩', trigger: 'blur' }
  ],
  finalScore: [
    { required: true, message: '请输入期末成绩', trigger: 'blur' }
  ]
}

// 加载课程列表
onMounted(async () => {
  try {
    const response = await api.getAllCourses()
    const allCourses = response.data || []
    
    // 根据用户角色过滤课程
    const userRoles = store.roles || []
    const userTeacherId = store.user?.teacherId
    
    if (userRoles.includes('ADMIN')) {
      // 管理员显示全部课程
      courses.value = allCourses
    } else if (userRoles.includes('TEACHER') && userTeacherId) {
      // 教师只显示自己开设的课程
      courses.value = allCourses.filter(course => course.teacherId === userTeacherId)
    } else {
      // 其他用户（如学生）不应该访问此页面，但以防万一
      courses.value = []
    }
  } catch (error) {
    ElMessage.error('加载课程列表失败')
    console.error(error)
  }
})

// 监听成绩变化，自动计算总评成绩
watch([() => scoreForm.usualScore, () => scoreForm.finalScore], ([usual, finalScore]) => {
  if (usual !== null && usual !== undefined && finalScore !== null && finalScore !== undefined) {
    scoreForm.totalScore = usual * 0.3 + finalScore * 0.7
  } else {
    scoreForm.totalScore = null
  }
})

const submitForm = async () => {
  try {
    // 验证表单
    await scoreFormRef.value.validate()
    
    // 转换数据类型
    const submitData = {
      courseId: scoreForm.courseId ? Number(scoreForm.courseId) : null,
      studentNo: scoreForm.studentNo || '',
      usualScore: scoreForm.usualScore !== null && scoreForm.usualScore !== undefined ? Number(scoreForm.usualScore) : null,
      finalScore: scoreForm.finalScore !== null && scoreForm.finalScore !== undefined ? Number(scoreForm.finalScore) : null,
      totalScore: scoreForm.totalScore !== null && scoreForm.totalScore !== undefined ? Number(scoreForm.totalScore) : null,
      comment: scoreForm.comment || '',
      status: scoreForm.status ? Number(scoreForm.status) : 1
    }
    
    await api.addScore(submitData)
    ElMessage.success('添加成绩成功')
    router.push('/home/score')
  } catch (error) {
    if (error.message && error.message.includes('validation')) {
      ElMessage.error('表单验证失败，请检查输入')
    } else {
      ElMessage.error(error.message || '添加成绩失败')
    }
  }
}

const resetForm = () => {
  if (scoreFormRef.value) {
    scoreFormRef.value.resetFields()
  }
  Object.assign(scoreForm, {
    courseId: '',
    studentNo: '',
    usualScore: null,
    finalScore: null,
    totalScore: null,
    comment: '',
    status: 1
  })
}

const goToSelectionList = () => {
  router.push('/home/selection')
}

const goBack = () => {
  router.push('/home/score')
}
</script>

<style scoped>
.add-score {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-help {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>