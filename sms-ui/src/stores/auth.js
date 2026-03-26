import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const permissions = computed(() => user.value?.permissions || [])
  const roleCode = computed(() => user.value?.roleCode || '')
  const realName = computed(() => user.value?.realName || user.value?.username || '')

  function setLogin(data) {
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      roleCode: data.roleCode,
      roleName: data.roleName,
      permissions: data.permissions
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function hasPermission(perm) {
    if (roleCode.value === 'SUPER_ADMIN') return true
    return permissions.value.includes(perm)
  }

  return { token, user, isLoggedIn, permissions, roleCode, realName, setLogin, logout, hasPermission }
})
