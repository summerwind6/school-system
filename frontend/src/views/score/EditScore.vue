<template>
  <div class="edit-score">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>编辑成绩</span>
        </div>
      </template>
      
      <el-form ref="scoreForm" :model="scoreForm" :rules="rules" label-width="100px">
        <el-form-item label="选课ID" prop="selectionId">
          <el-input v-model="scoreForm.selectionId" placeholder="请输入选课ID" disabled></el-input>
        </el-form-item>
        <el-form-item label="教师ID" prop="teacherId">
          <el-input v-model="scoreForm.teacherId" placeholder="请输入教师ID"></el-input>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const router = useRouter()
const route = useRoute()
const scoreId = route.params.id

const scoreForm = reactive({
  scoreId: '',
  selectionId: '',
  teacherId: '',
  usualScore: null,
  finalScore: null,
  totalScore: null,
  comment: '',
  status: 1
})

const rules = {
  teacherId: [
    { required: true, message: '请输入教师ID', trigger: 'blur' }
  ],
  usualScore: [
    { required: true, message: '请输入平时成绩', trigger: 'blur' }
  ],
  finalScore: [
    { required: true, message: '请输入期末成绩', trigger: 'blur' }
  ]
}

const getScoreInfo = async () => {
  try {
    const response = await api.getScoreById(scoreId)
    Object.assign(scoreForm, response.data)
  } catch (error) {
    ElMessage.error(error.message || '获取成绩信息失败')
  }
}

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
    await api.updateScore(scoreForm)
    ElMessage.success('更新成绩成功')
    router.push('/home/score')
  } catch (error) {
    ElMessage.error(error.message || '更新成绩失败')
  }
}

const resetForm = () => {
  getScoreInfo()
}

const goBack = () => {
  router.push('/home/score')
}

onMounted(() => {
  getScoreInfo()
})
</script>

<style scoped>
.edit-score {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>