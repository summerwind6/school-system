<template>
  <div class="score-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>成绩管理</span>
          <el-button v-if="hasRole(['TEACHER', 'ADMIN'])" type="primary" @click="goToAddScore">添加成绩</el-button>
        </div>
      </template>
      
      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-input v-model="filter.studentId" placeholder="学生ID" style="width: 150px; margin-right: 10px;"></el-input>
        <el-input v-model="filter.courseId" placeholder="课程ID" style="width: 150px; margin-right: 10px;"></el-input>
        <el-button type="primary" @click="getScores">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
        <el-button v-if="hasRole(['TEACHER', 'ADMIN'])" type="success" @click="exportScore">导出成绩</el-button>
      </div>
      
      <!-- 成绩列表 -->
      <el-table :data="scores" style="width: 100%" border>
        <el-table-column prop="scoreId" label="成绩ID" width="100"></el-table-column>
        <el-table-column prop="courseName" label="课程名称"></el-table-column>
        <el-table-column prop="studentName" label="学生姓名"></el-table-column>
        <el-table-column prop="usualScore" label="平时成绩"></el-table-column>
        <el-table-column prop="finalScore" label="期末成绩"></el-table-column>
        <el-table-column prop="totalScore" label="总评成绩"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
              {{ scope.row.status === 1 ? '已评定' : '未评定' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="hasRole(['TEACHER', 'ADMIN'])" label="操作" width="200">
          <template #default="scope">
            <el-button type="primary" size="small" @click="goToEditScore(scope.row.scoreId)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteScore(scope.row.scoreId)">删除</el-button>
            <el-button 
              type="info" 
              size="small" 
              @click="calculateTotalScore(scope.row.scoreId)"
            >
              计算总评
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination" style="margin-top: 20px; text-align: right;">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @update:current-page="handleCurrentChange"
          @update:page-size="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'
import store from '../../store'

const router = useRouter()
const scores = ref([])
const filter = ref({
  studentId: '',
  courseId: ''
})
const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

const hasRole = (roles) => {
  const userRoles = store.roles || []
  return roles.some(role => userRoles.includes(role))
}

const getScores = async () => {
  try {
    const response = await api.getScores({
      current: pagination.value.current,
      size: pagination.value.size,
      studentId: filter.value.studentId || undefined,
      courseId: filter.value.courseId || undefined
    })
    scores.value = response.data.records
    pagination.value.total = response.data.total
  } catch (error) {
    ElMessage.error(error.message || '获取成绩列表失败')
  }
}

const goToAddScore = () => {
  router.push('/home/score/add')
}

const goToEditScore = (id) => {
  router.push(`/home/score/edit/${id}`)
}

const deleteScore = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该成绩吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await api.deleteScore(id)
    ElMessage.success('删除成功')
    getScores()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const calculateTotalScore = async (id) => {
  try {
    await api.calculateTotalScore(id)
    ElMessage.success('计算总评成绩成功')
    getScores()
  } catch (error) {
    ElMessage.error(error.message || '计算总评成绩失败')
  }
}

const resetFilter = () => {
  filter.value.studentId = ''
  filter.value.courseId = ''
  getScores()
}

const exportScore = async () => {
  if (!filter.value.courseId) {
    ElMessage.warning('请输入课程ID')
    return
  }
  
  try {
    const response = await api.exportScore(filter.value.courseId)
    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '成绩表.xlsx'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const handleSizeChange = (size) => {
  pagination.value.size = size
  getScores()
}

const handleCurrentChange = (current) => {
  pagination.value.current = current
  getScores()
}

onMounted(() => {
  getScores()
})
</script>

<style scoped>
.score-list {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
