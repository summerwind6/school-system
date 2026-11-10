import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true // 关键：允许跨域携带Cookie
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    console.log('err' + error)
    const message = error.response?.data?.message
    if (message) {
      error.message = message
    }
    return Promise.reject(error)
  }
)

// API接口
export const api = {
  // 认证相关
  login: (data) => service.post('/auth/login', data),
  register: (data) => service.post('/auth/register', data),
  logout: () => service.post('/auth/logout'),
  getCurrentUser: () => service.get('/auth/current'),
  
  // 课程相关
  getCourses: (params) => service.get('/course/list', { params }),
  getSelectableCourses: (params) => service.get('/course/selectable', { params }),
  getAllCourses: () => service.get('/course/all'),
  getCourseById: (id) => service.get(`/course/detail/${id}`),
  addCourse: (data) => service.post('/course/add', data),
  updateCourse: (data) => service.post('/course/update', data),
  deleteCourse: (id) => service.delete(`/course/delete/${id}`),
  approveCourse: (id) => service.post(`/course/approve/${id}`),
  rejectCourse: (id) => service.post(`/course/reject/${id}`),
  updateCourseStatus: (id, status) => service.post(`/course/status/${id}`, null, { params: { status } }),
  getPendingCourses: (params) => service.get('/course/pending', { params }),
  getApprovedCourses: () => service.get('/course/approved'),
  
  // 选课相关
  getSelections: (params) => service.get('/selection/list', { params }),
  getStudentSelections: (studentId) => service.get(`/selection/student/${studentId}`),
  getCourseSelections: (courseId) => service.get(`/selection/course/${courseId}`),
  selectCourse: (data) => service.post('/selection/select', data),
  dropCourse: (id) => service.delete(`/selection/drop/${id}`),
  checkConflict: (params) => service.get('/selection/check', { params }),
  
  // 成绩相关
  getScores: (params) => service.get('/score/list', { params }),
  getStudentScores: (studentId) => service.get(`/score/student/${studentId}`),
  getCourseScores: (courseId) => service.get(`/score/course/${courseId}`),
  getTeacherScores: (teacherId) => service.get(`/score/teacher/${teacherId}`),
  getScoreById: (id) => service.get(`/score/detail/${id}`),
  addScore: (data) => service.post('/score/add', data),
  updateScore: (data) => service.post('/score/update', data),
  deleteScore: (id) => service.delete(`/score/delete/${id}`),
  calculateTotalScore: (id) => service.post(`/score/calculate/${id}`),
  
  // 导出相关
  exportScore: (courseId) => service.get(`/export/score/${courseId}`, { responseType: 'blob' }),
  exportCourse: () => service.get('/export/course', { responseType: 'blob' }),
  
  // 教师相关
  getTeacherList: () => service.get('/teacher/list'),
  
  // 院系相关
  getDepartmentList: () => service.get('/department/list'),
  getDepartmentOptions: () => service.get('/department/options')
}

export default service
