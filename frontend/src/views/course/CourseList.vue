<template>
  <div class="course-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>课程管理</span>
          <div class="header-actions">
            <el-button v-if="hasRole(['TEACHER', 'ADMIN'])" type="primary" @click="goToAddCourse">添加课程</el-button>
            <el-button v-if="hasRole(['TEACHER', 'ADMIN'])" type="success" @click="exportCourse">导出课程</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-input v-model="filter.courseName" placeholder="课程名称" clearable style="width: 200px; margin-right: 10px;"></el-input>
        <el-select v-model="filter.auditStatus" placeholder="审核状态" clearable style="width: 150px; margin-right: 10px;">
          <el-option label="待审核" :value="0"></el-option>
          <el-option label="已通过" :value="1"></el-option>
          <el-option label="已拒绝" :value="2"></el-option>
        </el-select>
        <el-select v-model="filter.status" placeholder="启用状态" clearable style="width: 150px; margin-right: 10px;">
          <el-option label="启用" :value="1"></el-option>
          <el-option label="停用" :value="0"></el-option>
        </el-select>
        <el-button type="primary" icon="Search" @click="getCourses">查询</el-button>
        <el-button icon="RefreshRight" @click="resetFilter">重置</el-button>
      </div>

      <!-- 课程列表 -->
      <el-table :data="courses" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column prop="courseId" label="ID" width="70"></el-table-column>
        <el-table-column prop="courseName" label="课程名称" min-width="150">
          <template #default="scope">
            <strong>{{ scope.row.courseName }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="courseCode" label="编码" width="100"></el-table-column>
        <el-table-column prop="credits" label="学分" width="70" sortable></el-table-column>
        <el-table-column prop="hours" label="课时" width="70"></el-table-column>
        <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip></el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.auditStatus === 0" type="warning" effect="dark">待审核</el-tag>
            <el-tag v-else-if="scope.row.auditStatus === 1" type="success" effect="dark">已通过</el-tag>
            <el-tag v-else-if="scope.row.auditStatus === 2" type="danger" effect="dark">已拒绝</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="启用状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <!-- 编辑按钮：教师可编辑自己的课程 -->
            <el-button 
              v-if="hasRole(['TEACHER', 'ADMIN'])" 
              type="primary" 
              size="small"
              @click="goToEditCourse(scope.row.courseId)"
            >
              编辑
            </el-button>

            <!-- 审核按钮：仅管理员可见 -->
            <template v-if="hasRole(['ADMIN']) && scope.row.auditStatus === 0">
              <el-button 
                type="success" 
                size="small"
                @click="approveCourse(scope.row.courseId, scope.row.courseName)"
              >
                通过
              </el-button>
              <el-button 
                type="danger" 
                size="small"
                @click="rejectCourse(scope.row.courseId, scope.row.courseName)"
              >
                拒绝
              </el-button>
            </template>

            <!-- 状态切换：审核通过的课程可以停用/启用 -->
            <el-button 
              v-if="hasRole(['ADMIN']) && scope.row.auditStatus === 1 && scope.row.status === 1"
              type="warning" 
              size="small"
              @click="updateStatus(scope.row.courseId, 0, scope.row.courseName)"
            >
              停用
            </el-button>
            <el-button 
              v-if="hasRole(['ADMIN']) && scope.row.status === 0"
              type="success" 
              size="small"
              @click="updateStatus(scope.row.courseId, 1, scope.row.courseName)"
            >
              启用
            </el-button>

            <!-- 删除按钮 -->
            <el-button 
              v-if="hasRole(['TEACHER', 'ADMIN'])" 
              type="danger" 
              size="small"
              @click="deleteCourse(scope.row.courseId, scope.row.courseName)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'
import store from '../../store'

const router = useRouter()
const loading = ref(false)
const courses = ref([])
const filter = ref({
  courseName: '',
  auditStatus: null,
  status: null
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

const getCourses = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.value.current,
      size: pagination.value.size,
      courseName: filter.value.courseName || undefined
    }
    
    const response = await api.getCourses(params)
    let records = response.data.records || []
    
    if (filter.value.auditStatus !== null && filter.value.auditStatus !== '') {
      records = records.filter(course => course.auditStatus === filter.value.auditStatus)
    }
    
    if (filter.value.status !== null && filter.value.status !== '') {
      records = records.filter(course => course.status === filter.value.status)
    }
    
    courses.value = records
    pagination.value.total = response.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '获取课程列表失败')
  } finally {
    loading.value = false
  }
}

const goToAddCourse = () => {
  router.push('/home/course/add')
}

const goToEditCourse = (id) => {
  router.push(`/home/course/edit/${id}`)
}

const approveCourse = async (courseId, courseName) => {
  try {
    await ElMessageBox.confirm(
      `确定要通过《${courseName}》的审核吗？`,
      '审核确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    await api.approveCourse(courseId)
    ElMessage.success(`《${courseName}》审核通过`)
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审核失败')
    }
  }
}

const rejectCourse = async (courseId, courseName) => {
  try {
    await ElMessageBox.confirm(
      `确定要拒绝《${courseName}》吗？`,
      '拒绝确认',
      {
        confirmButtonText: '确定拒绝',
        cancelButtonText: '取消',
        type: 'warning',
        inputPattern: /.+/,
        inputErrorMessage: '请输入拒绝原因',
        inputValidator: (value) => {
          if (!value || value.trim() === '') {
            return '请输入拒绝原因'
          }
          return true
        }
      }
    )
    
    await api.rejectCourse(courseId)
    ElMessage.success(`已拒绝《${courseName}》`)
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

const updateStatus = async (courseId, status, courseName) => {
  const action = status === 1 ? '启用' : '停用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}《${courseName}》吗？`,
      `${action}确认`,
      {
        confirmButtonText: `确定${action}`,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await api.updateCourseStatus(courseId, status)
    ElMessage.success(`${action}成功`)
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

const deleteCourse = async (courseId, courseName) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除《${courseName}》吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await api.deleteCourse(courseId)
    ElMessage.success('删除成功')
    getCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const resetFilter = () => {
  filter.value.courseName = ''
  filter.value.auditStatus = null
  filter.value.status = null
  pagination.value.current = 1
  getCourses()
}

const exportCourse = async () => {
  try {
    const response = await api.exportCourse()
    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `课程表_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
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
.course-list {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
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
