<template>
  <div class="selection-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>我的选课</span>
          <el-button type="primary" @click="goToSelectCourse">选课</el-button>
        </div>
      </template>
      
      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-select v-model="filter.semester" placeholder="学期" clearable style="width: 150px; margin-right: 10px;">
          <el-option label="春季学期" value="春季学期"></el-option>
          <el-option label="秋季学期" value="秋季学期"></el-option>
        </el-select>
        <el-select v-model="filter.year" placeholder="学年" clearable style="width: 150px; margin-right: 10px;">
          <el-option label="2023-2024" value="2023-2024"></el-option>
          <el-option label="2024-2025" value="2024-2025"></el-option>
          <el-option label="2025-2026" value="2025-2026"></el-option>
        </el-select>
        <el-input v-model="filter.courseName" placeholder="课程名称" clearable style="width: 200px; margin-right: 10px;"></el-input>
        <el-button type="primary" @click="getSelections">查询</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>
      
      <!-- 选课列表 -->
      <el-table :data="selections" style="width: 100%" border>
        <el-table-column prop="courseName" label="课程名称" min-width="150">
          <template #default="scope">
            {{ scope.row.courseName || '加载中...' }}
          </template>
        </el-table-column>
        <el-table-column prop="courseCode" label="课程编码" width="120"></el-table-column>
        <el-table-column prop="credits" label="学分" width="80">
          <template #default="scope">
            {{ scope.row.credits || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="100">
          <template #default="scope">
            {{ scope.row.teacherName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" width="100"></el-table-column>
        <el-table-column prop="year" label="学年" width="100"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '正常' : '已退' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button 
              v-if="scope.row.status === 1" 
              type="danger" 
              size="small" 
              @click="dropCourse(scope.row.selectionId)"
            >
              退课
            </el-button>
            <span v-else class="text-muted">已退课</span>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination" style="margin-top: 20px; text-align: right;">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="pagination.total"
          @update:current-page="handleCurrentChange"
          @update:page-size="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import store from '../../store'
import { api } from '../../api'

const router = useRouter()
const selections = ref([])
const filter = ref({
  semester: '',
  year: '',
  courseName: ''
})
const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

const getStudentId = () => {
  return store.user?.studentId || null
}

const getSelections = async () => {
  const studentId = getStudentId()
  if (!studentId) {
    ElMessage.error('无法获取学生信息，请重新登录')
    return
  }
  
  try {
    const response = await api.getStudentSelections(studentId)
    let data = response.data || []
    
    if (Array.isArray(data)) {
      if (filter.value.semester) {
        data = data.filter(item => item.semester === filter.value.semester)
      }
      if (filter.value.year) {
        data = data.filter(item => item.year === filter.value.year)
      }
      if (filter.value.courseName) {
        data = data.filter(item => 
          item.courseName && item.courseName.includes(filter.value.courseName)
        )
      }
      
      pagination.value.total = data.length
      const start = (pagination.value.current - 1) * pagination.value.size
      selections.value = data.slice(start, start + pagination.value.size)
    } else {
      selections.value = []
      pagination.value.total = 0
    }
  } catch (error) {
    ElMessage.error(error.message || '获取选课列表失败')
  }
}

const goToSelectCourse = () => {
  router.push('/home/selection/select')
}

const dropCourse = async (id) => {
  try {
    await ElMessageBox.confirm('确定要退课吗？退课后需要重新选课', '提示', {
      confirmButtonText: '确定退课',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await api.dropCourse(id)
    ElMessage.success('退课成功')
    getSelections()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退课失败')
    }
  }
}

const resetFilter = () => {
  filter.value.semester = ''
  filter.value.year = ''
  filter.value.courseName = ''
  pagination.value.current = 1
  getSelections()
}

const handleSizeChange = (size) => {
  pagination.value.size = size
  pagination.value.current = 1
  getSelections()
}

const handleCurrentChange = (current) => {
  pagination.value.current = current
  getSelections()
}

onMounted(() => {
  getSelections()
})
</script>

<style scoped>
.selection-list {
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
  flex-wrap: wrap;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.text-muted {
  color: #909399;
  font-size: 12px;
}
</style>
