<template>
  <div class="select-course">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>选课中心</span>
          <div class="header-info">
            <el-tag type="info" size="large">已选 {{ selectedCount }} 门课</el-tag>
            <el-tag type="warning" size="large">{{ totalCredits }} 学分</el-tag>
          </div>
        </div>
      </template>

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部课程" name="all">
          <!-- 筛选条件 -->
          <div class="filter-section">
            <el-input 
              v-model="filter.courseName" 
              placeholder="搜索课程名称" 
              clearable 
              prefix-icon="Search"
              style="width: 220px; margin-right: 10px;"
              @keyup.enter="searchCourses"
            ></el-input>
            <el-select v-model="filter.deptId" placeholder="所属院系" clearable style="width: 180px; margin-right: 10px;">
              <el-option label="全部院系" value=""></el-option>
              <el-option v-for="dept in departments" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId"></el-option>
            </el-select>
            <el-select v-model="filter.creditsRange" placeholder="学分范围" clearable style="width: 150px; margin-right: 10px;">
              <el-option label="不限" value=""></el-option>
              <el-option label="1-2学分" value="1-2"></el-option>
              <el-option label="3-4学分" value="3-4"></el-option>
              <el-option label="5学分以上" value="5+"></el-option>
            </el-select>
            <el-button type="primary" icon="Search" @click="searchCourses">查询</el-button>
            <el-button icon="RefreshRight" @click="resetFilter">重置</el-button>
          </div>

          <!-- 课程列表 -->
          <el-table :data="filteredCourses" v-loading="loading" style="width: 100%" border stripe>
            <el-table-column prop="courseName" label="课程名称" min-width="180">
              <template #default="scope">
                <span :class="{ 'selected-course': isCourseSelected(scope.row.courseId) }">
                  {{ scope.row.courseName }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="courseCode" label="课程编码" width="120"></el-table-column>
            <el-table-column prop="credits" label="学分" width="80" sortable>
              <template #default="scope">
                <strong>{{ scope.row.credits }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="hours" label="课时" width="80"></el-table-column>
            <el-table-column prop="deptName" label="所属院系" width="160"></el-table-column>
            <el-table-column prop="teacherName" label="授课教师" width="100">
              <template #default="scope">
                {{ scope.row.teacherName || '待定' }}
              </template>
            </el-table-column>
            <el-table-column label="选课状态" width="110" fixed="right">
              <template #default="scope">
                <el-tag v-if="isCourseSelected(scope.row.courseId)" type="success" effect="dark" size="small">
                  ✓ 已选
                </el-tag>
                <el-tag v-else type="info" effect="plain" size="small">
                  未选
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="scope">
                <el-button 
                  v-if="isCourseSelected(scope.row.courseId)"
                  type="danger" 
                  size="small"
                  @click="dropCourse(scope.row)"
                >
                  退课
                </el-button>
                <el-button 
                  v-else
                  type="primary" 
                  size="small"
                  :disabled="!selectionForm.semester || !selectionForm.year"
                  @click="selectCourse(scope.row)"
                >
                  选课
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
        </el-tab-pane>

        <el-tab-pane label="我的已选" name="selected">
          <!-- 已选课程列表 -->
          <el-table :data="mySelections" v-loading="loadingSelections" style="width: 100%" border stripe>
            <el-table-column prop="courseName" label="课程名称" min-width="180">
              <template #default="scope">
                <strong>{{ scope.row.courseName }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="courseCode" label="课程编码" width="120"></el-table-column>
            <el-table-column prop="credits" label="学分" width="80" sortable>
              <template #default="scope">
                <el-tag type="warning">{{ scope.row.credits }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="teacherName" label="授课教师" width="100">
              <template #default="scope">
                {{ scope.row.teacherName || '待定' }}
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
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="scope">
                <el-button 
                  v-if="scope.row.status === 1"
                  type="danger" 
                  size="small"
                  link
                  @click="dropCourseById(scope.row.selectionId, scope.row.courseName)"
                >
                  退课
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 已选课程为空时 -->
          <el-empty v-if="!loadingSelections && mySelections.length === 0" description="暂未选择任何课程">
            <el-button type="primary" @click="activeTab = 'all'">去选课</el-button>
          </el-empty>

          <!-- 统计信息 -->
          <div v-if="mySelections.length > 0" class="selection-stats">
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="已选课程数">
                <strong>{{ mySelections.length }} 门</strong>
              </el-descriptions-item>
              <el-descriptions-item label="总学分">
                <strong>{{ totalCredits }} 分</strong>
              </el-descriptions-item>
              <el-descriptions-item label="当前学期">
                {{ selectionForm.semester }} {{ selectionForm.year }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 选课设置（固定在底部） -->
      <div class="semester-settings">
        <el-divider content-position="left">选课设置</el-divider>
        <el-form :inline="true" :model="selectionForm" label-width="60px">
          <el-form-item label="学期">
            <el-select v-model="selectionForm.semester" placeholder="请选择学期" style="width: 150px;">
              <el-option label="春季学期" value="春季学期"></el-option>
              <el-option label="秋季学期" value="秋季学期"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="学年">
            <el-select v-model="selectionForm.year" placeholder="请选择学年" style="width: 150px;">
              <el-option label="2024-2025" value="2024-2025"></el-option>
              <el-option label="2025-2026" value="2025-2026"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-alert 
              v-if="!selectionForm.semester || !selectionForm.year"
              title="请先选择学期和学年再进行选课操作" 
              type="warning" 
              :closable="false" 
              show-icon
              style="margin-left: 20px;"
            />
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import store from '../../store'
import { api } from '../../api'

const router = useRouter()

const loading = ref(false)
const loadingSelections = ref(false)
const activeTab = ref('all')
const courses = ref([])
const departments = ref([])
const mySelections = ref([])

const filter = ref({
  courseName: '',
  deptId: '',
  creditsRange: ''
})

const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})

const selectionForm = reactive({
  semester: '春季学期',
  year: '2025-2026'
})

const getStudentId = () => {
  return store.user?.studentId || null
}

const isCourseSelected = (courseId) => {
  return mySelections.value.some(item => item.courseId === courseId && item.status === 1)
}

const selectedCount = computed(() => {
  return mySelections.value.filter(item => item.status === 1).length
})

const totalCredits = computed(() => {
  return mySelections.value
    .filter(item => item.status === 1 && item.credits)
    .reduce((sum, item) => sum + Number(item.credits), 0)
})

const filteredCourses = computed(() => {
  let result = courses.value
  
  if (filter.value.courseName) {
    result = result.filter(course => 
      course.courseName.toLowerCase().includes(filter.value.courseName.toLowerCase())
    )
  }
  
  if (filter.value.deptId) {
    result = result.filter(course => course.deptId == filter.value.deptId)
  }
  
  if (filter.value.creditsRange) {
    const range = filter.value.creditsRange
    if (range === '1-2') {
      result = result.filter(course => course.credits >= 1 && course.credits <= 2)
    } else if (range === '3-4') {
      result = result.filter(course => course.credits >= 3 && course.credits <= 4)
    } else if (range === '5+') {
      result = result.filter(course => course.credits >= 5)
    }
  }
  
  pagination.value.total = result.length
  const start = (pagination.value.current - 1) * pagination.value.size
  return result.slice(start, start + pagination.value.size)
})

const getCourses = async () => {
  loading.value = true
  try {
    const response = await api.getCourses({
      current: 1,
      size: 1000
    })
    let allCourses = response.data.records || []
    courses.value = allCourses.filter(course => course.auditStatus === 1 && course.status === 1)
    pagination.value.total = courses.value.length
  } catch (error) {
    ElMessage.error(error.message || '获取课程列表失败')
  } finally {
    loading.value = false
  }
}

const getMySelections = async () => {
  const studentId = getStudentId()
  if (!studentId) {
    ElMessage.error('无法获取学生信息，请重新登录')
    return
  }

  loadingSelections.value = true
  try {
    const response = await api.getStudentSelections(studentId)
    mySelections.value = response.data || []
  } catch (error) {
    console.error('获取选课记录失败:', error)
    mySelections.value = []
  } finally {
    loadingSelections.value = false
  }
}

const selectCourse = async (course) => {
  const studentId = getStudentId()
  if (!studentId) {
    ElMessage.error('无法获取学生信息，请重新登录')
    return
  }

  if (!selectionForm.semester || !selectionForm.year) {
    ElMessage.warning('请先选择学期和学年')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要选择《${course.courseName}》吗？`,
      '选课确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    await api.selectCourse({
      courseId: course.courseId,
      semester: selectionForm.semester,
      year: selectionForm.year
    })

    ElMessage.success(`成功选择《${course.courseName}》`)
    
    await getMySelections()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '选课失败')
    }
  }
}

const dropCourse = async (course) => {
  const studentId = getStudentId()
  const selection = mySelections.value.find(item => 
    item.courseId === course.courseId && item.status === 1
  )

  if (!selection) {
    ElMessage.error('找不到选课记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要退选《${course.courseName}》吗？`,
      '退课确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.dropCourse(selection.selectionId)
    ElMessage.success(`成功退选《${course.courseName}》`)
    
    await getMySelections()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退课失败')
    }
  }
}

const dropCourseById = async (selectionId, courseName) => {
  try {
    await ElMessageBox.confirm(
      `确定要退选《${courseName}》吗？`,
      '退课确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await api.dropCourse(selectionId)
    ElMessage.success(`成功退选《${courseName}》`)
    
    await getMySelections()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '退课失败')
    }
  }
}

const searchCourses = () => {
  pagination.value.current = 1
}

const resetFilter = () => {
  filter.value.courseName = ''
  filter.value.deptId = ''
  filter.value.creditsRange = ''
  pagination.value.current = 1
}

const handleTabChange = (tab) => {
  if (tab === 'selected') {
    getMySelections()
  }
}

const handleSizeChange = (val) => {
  pagination.value.size = val
  pagination.value.current = 1
}

const handleCurrentChange = (val) => {
  pagination.value.current = val
}

onMounted(async () => {
  await Promise.all([
    getCourses(),
    getMySelections()
  ])

  departments.value = [
    { deptId: 1, deptName: '计算机科学与技术学院' },
    { deptId: 2, deptName: '软件学院' },
    { deptId: 3, deptName: '信息工程学院' },
    { deptId: 4, deptName: '数学与统计学院' },
    { deptId: 5, deptName: '外国语学院' },
    { deptId: 6, deptName: '电子工程学院' },
    { deptId: 7, deptName: '经济管理学院' }
  ]
})
</script>

<style scoped>
.select-course {
  width: 100%;
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info {
  display: flex;
  gap: 12px;
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

.selected-course {
  color: #409EFF;
  font-weight: bold;
}

.semester-settings {
  margin-top: 24px;
  padding-top: 16px;
  background-color: #fafafa;
  border-radius: 8px;
}

.selection-stats {
  margin-top: 20px;
}
</style>
