<template>
  <div class="dashboard">
    <el-card shadow="hover" class="dashboard-card">
      <template #header>
        <div class="card-header">
          <span>系统概览</span>
        </div>
      </template>
      <div class="dashboard-stats">
        <el-statistic class="stat-item" title="总课程数" value="100"></el-statistic>
        <el-statistic class="stat-item" title="选课人数" value="2000"></el-statistic>
        <el-statistic class="stat-item" title="成绩录入率" value="85" suffix="%"></el-statistic>
        <el-statistic class="stat-item" title="系统用户" value="3000"></el-statistic>
      </div>
    </el-card>
    
    <div class="dashboard-content">
      <el-card shadow="hover" class="content-card">
        <template #header>
          <div class="card-header">
            <span>最近课程</span>
            <el-button type="primary" size="small" @click="goToCourseList">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentCourses" style="width: 100%">
          <el-table-column prop="courseName" label="课程名称" width="200"></el-table-column>
          <el-table-column prop="courseCode" label="课程编码"></el-table-column>
          <el-table-column prop="credits" label="学分"></el-table-column>
          <el-table-column prop="hours" label="课时"></el-table-column>
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
      
      <el-card shadow="hover" class="content-card">
        <template #header>
          <div class="card-header">
            <span>最近成绩</span>
            <el-button type="primary" size="small" @click="goToScoreList">查看全部</el-button>
          </div>
        </template>
        <el-table :data="recentScores" style="width: 100%">
          <el-table-column prop="courseName" label="课程名称" width="200"></el-table-column>
          <el-table-column prop="studentName" label="学生姓名"></el-table-column>
          <el-table-column prop="usualScore" label="平时成绩"></el-table-column>
          <el-table-column prop="finalScore" label="期末成绩"></el-table-column>
          <el-table-column prop="totalScore" label="总评成绩"></el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const recentCourses = ref([])
const recentScores = ref([])

const goToCourseList = () => {
  router.push('/home/course')
}

const goToScoreList = () => {
  router.push('/home/score')
}

onMounted(async () => {
  // 模拟数据
  recentCourses.value = [
    { courseName: 'Java程序设计', courseCode: 'CS101', credits: 4, hours: 64, status: 1 },
    { courseName: '数据结构', courseCode: 'CS102', credits: 3, hours: 48, status: 1 },
    { courseName: '操作系统', courseCode: 'CS103', credits: 4, hours: 64, status: 1 },
    { courseName: '计算机网络', courseCode: 'CS104', credits: 3, hours: 48, status: 1 }
  ]
  
  recentScores.value = [
    { courseName: 'Java程序设计', studentName: '张三', usualScore: 85, finalScore: 90, totalScore: 88.5 },
    { courseName: '数据结构', studentName: '李四', usualScore: 90, finalScore: 85, totalScore: 86.5 },
    { courseName: '操作系统', studentName: '王五', usualScore: 80, finalScore: 88, totalScore: 85.6 },
    { courseName: '计算机网络', studentName: '赵六', usualScore: 92, finalScore: 95, totalScore: 94.1 }
  ]
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dashboard-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dashboard-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stat-item {
  text-align: center;
}

.dashboard-content {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.content-card {
  flex: 1;
  min-width: 400px;
}

@media (max-width: 768px) {
  .dashboard-content {
    flex-direction: column;
  }
  
  .content-card {
    min-width: 100%;
  }
}
</style>