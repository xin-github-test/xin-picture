import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

/**
 * 存储用户登陆信息的状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<any>({
    userName: '未登录',
  })
  /**
   * 设置登陆用户
   * @param newLoginUser 获取到的用户
   */
  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }
  /**
   * 获取登陆用户（调用后端接口）
   */
  async function fetchLoginUser() {}

  return { loginUser, setLoginUser, fetchLoginUser }
})
