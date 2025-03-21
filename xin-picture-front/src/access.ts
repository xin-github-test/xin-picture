import { message } from 'ant-design-vue'
import router from './router'
import { useLoginUserStore } from './stores/useLoginUserStore'
// 是否为首次获取登陆用户
let firstFetchLoginUser = true
/**
 * 全局权限校验
 */
//TODO 全局权限校验简化
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  //确保页面刷新时，首次加载时，能等待后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath

  //自定义权限校验逻辑
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限访问该页面')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  //其他页面，直接放行
  next()
})
