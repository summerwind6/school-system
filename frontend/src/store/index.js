import { reactive, ref } from 'vue'

const store = reactive({
  user: null,
  roles: [],
  permissions: [],
  menu: [],
  
  setUser(user) {
    this.user = user
  },
  
  setRoles(roles) {
    this.roles = roles
  },
  
  setPermissions(permissions) {
    this.permissions = permissions
  },
  
  setMenu(menu) {
    this.menu = menu
  },
  
  clear() {
    this.user = null
    this.roles = []
    this.permissions = []
    this.menu = []
    localStorage.removeItem('token')
  }
})

export default store