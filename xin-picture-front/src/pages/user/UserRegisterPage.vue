<template>
  <div id="userRegisterPage">
    <div class="title">xin-云图库 - 用户注册</div>
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
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码!' },
          { min: 8, message: '确认密码长度不能小于8位' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">立即登陆</RouterLink>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { userRegisterUsingPost } from '@/api/userController'
import router from '@/router'
import { message } from 'ant-design-vue'
import { reactive } from 'vue'

//TODO 注册页面不太美观，需要美化一下

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})
/**
 * 提交表单
 * @param values 表单数据
 */
const handleSubmit = async (values: any) => {
  //做一些校验，俩次输入的密码是否一致
  if (values.password !== values.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }

  const res = await userRegisterUsingPost(values)
  //注册成功,tiao转到登录页面
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    //跳转到登陆页面
    router.push({
      path: '/user/login',
      replace: true, //覆盖掉当前页面
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  max-width: 360px;
  margin: 0 auto;
}
.title {
  text-align: center;
  margin-bottom: 16px;
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
