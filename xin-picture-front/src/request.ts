import { message } from 'ant-design-vue'
import axios from 'axios'
//创建axios实例
const myAxios = axios.create({
  baseURL: 'http://localhost:8081/',
  timeout: 60000, //超时时间
  withCredentials: true, //是否允许携带cookie
})

// 添加请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // 在发送请求之前做些什么
    return config
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error)
  },
)

// 添加响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    //未登录
    if (data.code === 40100) {
      // 不是获取用户信息的请求，并且目前用户不在登陆页，则跳转到登陆页
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登陆！')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
      return response
    }
    if (data.code === 50000) {
      //处理服务器异常
      message.error('服务器异常,请稍后再试！')
    }
    return response
  },
  function (error) {
    // 对响应错误做点什么
    return Promise.reject(error)
  },
)
export default myAxios
