<template>
  <div class="course-audit">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>课程审核</span>
          <el-tag type="warning" size="large">待审核课程 {{ pagination.total }} 门</el-tag>
        </div>
      </template>

      <div class="filter-section">
        <el-input 
          v-model="filter.courseName" 
          placeholder="搜索课程名称" 
          clearable 
          prefix-icon="Search"
          style="width: 220px; margin-right: 10px;"
          @keyup.enter="getCourses"
        ></el-input>
        <el-button type="primary" icon="Search" @click="getCourses">查询</el-button>
        <el-button icon="RefreshRight" @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="courses" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column prop="courseId" label="ID" width="70"></el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="150">
          <template #default="scope">
            <strong>{{ scope.row.courseName }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="courseCode" label="编码" width="120"></el-table-column>
        <el-table-column prop="credits" label="学分" width="80" sortable></el-table-column>
        <el-table-column prop="hours" label="课时" width="80"></el-table-column>
        <el-table-column prop="description" label="课程描述" min-width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.auditStatus === 0" type="warning" effect="dark">待审核</el-tag>
            <el-tag v-else-if="scope.row.auditStatus === 1" type="success" effect="dark">已通过</el-tag>
            <el-tag v-else-if="scope.row.auditStatus === 2" type="danger" effect="dark">已拒绝</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button 
              type="success" 
              size="small"
              @click="approveCourse(scope.row)"
            >
              通过
            </el-button>
            <el-button 
              type="danger" 
              size="small"
              @click="rejectCourse(scope.row)"
            >
              拒绝
            </el-button>
            <el-button 
              type="info" 
              size="small"
              @click="viewDetail(scope.row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="课程详情" width="600px">
      <el-descriptions :column="2" border v-if="currentCourse">
        <el-descriptions-item label="课程名称">{{ currentCourse.courseName }}</el-descriptions-item>
        <el-descriptions-item label="课程编码">{{ currentCourse.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ currentCourse.credits }}</el-descriptions-item>
        <el-descriptions-item label="课时">{{ currentCourse.hours }}</el-descriptions-item>
        <el-descriptions-item label="授课教师ID">{{ currentCourse.teacherId || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="所属院系ID">{{ currentCourse.deptId }}</el-descriptions-item>
        <el-descriptions-item label="课程描述" :span="2">{{ currentCourse.description || '无' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ formatTime(currentCourse.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="success" @click="approveCourse(currentCourse); detailVisible = false">通过</el-button>
        <el-button type="danger" @click="rejectCourse(currentCourse); detailVisible = false">拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const loading = ref(false)
const courses = ref([])
const detailVisible = ref(false)
const currentCourse = ref(null)

const filter = ref({
  courseName: ''
})

const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

const formatTime = (time) => {
  if (!time) return '-'
  if (typeof time === 'string') return time.replace('T', ' ').substring(0, 19)
  return time
}

const getCourses = async () => {
  loading.value = true
  try {
    const response = await api.getPendingCourses({
      current: pagination.value.current,
      size: pagination.value.size,
      courseName: filter.value.courseName || undefined
    })
    courses.value = response.data.records || []
    pagination.value.total = response.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '获取待审核课程失败')
  } finally {
    loading.value = false
  }
}

const approveCourse = async (course) => {
  if (!course) return
  try {
    await ElMessageBox.confirm(
      `确定要通过《${course.courseName}》的审核吗？通过后学生将可以选课。`,
      '审核确认',
      {
        confirmButtonText: '确定通过',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
    
    await api.approveCourse(course.courseId)
    ElMessage.success(`《${course.courseName}》审核通过`)
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

const rejectCourse = async (course) => {
  if (!course) return
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入拒绝《${course.courseName}》的原因：`,
      '拒绝确认',
      {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        type: 'warning',
        inputPattern: /.+/,
        inputErrorMessage: '请输入拒绝原因'
      }
    )
    
    await api.rejectCourse(course.courseId)
    ElMessage.success(`已拒绝《${course.courseName}》`)
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const viewDetail = (course) => {
  currentCourse.value = course
  detailVisible.value = true
}

const resetFilter = () => {
  filter.value.courseName = ''
  pagination.value.current = 1
  getCourses()
}

const handleSizeChange = (size) => {
  pagination.value.size = size
  getCourses()
}

const handleCurrentChange = (current) => {
  pagination.value.current = current
  getCourses()
}

onMounted(() => {
  getCourses()
})
</script>

<style scoped>
.course-audit {
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
  gap: 8px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
