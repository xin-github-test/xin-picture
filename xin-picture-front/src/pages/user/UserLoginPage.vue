<template>
  <div id="userLoginPage">
    <div class="title">星图库 - 用户登录</div>
    <div style="margin-bottom: 16px" />
    <div class="desc">智能协同云图库</div>
    <div style="margin-bottom: 16px" />
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号!' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码!' },
          { min: 8, message: '密码长度不能小于8位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <div class="tips">
        没有账号？
        <RouterLink to="/user/register">立即注册</RouterLink>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登陆</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { userLoginUsingPost } from '@/api/userController'
import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'

//TODO 登陆页面不太美观，需要美化一下

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})
//全局状态管理
const loginUserStore = useLoginUserStore()
/**
 * 提交表单
 * @param values 表单数据
 */
const handleSubmit = async (values: any) => {
  const res = await userLoginUsingPost(values)
  //登陆成功,后端会将用户信息放到session中
  if (res.data.code === 0 && res.data.data) {
    //通过调用该接口，再次从session中获取用户信息，同时将用户信息保存到全局状态管理中
    await loginUserStore.fetchLoginUser()
    message.success('登陆成功')
    //跳转到首页
    router.push({
      path: '/',
      replace: true, //覆盖掉当前页面
    })
  } else {
    message.error('登陆失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userLoginPage {
  max-width: 420px;
  margin: 0 auto;
  margin-top: 32px;
  background-color: rgba(66, 68, 70, 0.1);
  padding: 32px;
  border-radius: 8%;
}
.title {
  text-align: center;
  font-size: 24px;
  margin-bottom: 16px;
  font-weight: bold;
}
.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}
.tips {
  color: #bbb;
  text-align: right;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
