import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('portalAuth', () => {
  const token = ref(localStorage.getItem('portal_token') || '')
  const user  = ref(JSON.parse(localStorage.getItem('portal_user') || 'null'))

  const isLoggedIn    = computed(() => !!token.value)
  const companyName   = computed(() => user.value?.companyName || user.value?.customerName || '')
  const balance       = computed(() => user.value?.balance ?? 0)

  function setAuth(t, u) {
    token.value = t
    user.value  = u
    localStorage.setItem('portal_token', t)
    localStorage.setItem('portal_user', JSON.stringify(u))
  }

  function updateBalance(val) {
    if (user.value) {
      user.value.balance = val
      localStorage.setItem('portal_user', JSON.stringify(user.value))
    }
  }

  function logout() {
    token.value = ''
    user.value  = null
    localStorage.removeItem('portal_token')
    localStorage.removeItem('portal_user')
  }

  return { token, user, isLoggedIn, companyName, balance, setAuth, updateBalance, logout }
})
