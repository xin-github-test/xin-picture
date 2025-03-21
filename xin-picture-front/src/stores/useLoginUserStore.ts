import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUserUsingGet } from '@/api/userController'

/**
 * 存储用户登陆信息的状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVo>({
    userName: '未登录',
  })
  /**
   * 设置登陆用户
   * @param newLoginUser 获取到的用户
   */
  function setLoginUser(newLoginUser: API.LoginUserVo) {
    loginUser.value = newLoginUser
  }
  /**
   * 获取登陆用户（调用后端接口）
   */
  async function fetchLoginUser() {
    // 调用后端接口获取登陆用户
    const res = await getLoginUserUsingGet()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})
