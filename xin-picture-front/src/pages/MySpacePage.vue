<template>
  <div id="mySpacePage">
    <p>正在跳转，请稍后...</p>
  </div>
</template>
<script setup lang="ts">
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    //重定向到登陆页
    router.replace('/user/login')
    return
  }

  //用户登陆，则获取用户空间
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    current: 1,
    pageSize: 1,
  })
  if (res.data.code === 0) {
    //有空间则进入第一个空间
    if (res.data.data?.records && res.data.data?.records.length > 0) {
      const space = res.data.data.records[0]
      router.replace(`/space/${space.id}`)
    } else {
      //没有空间则跳转到创建空间页面
      router.replace('/add_space')
      message.warn('请先创建个人空间')
    }
  } else {
    message.error('加载个人空间失败，' + res.data.message)
  }
}
//页面挂载完成之后，校验用户
onMounted(() => {
  checkUserSpace()
})
</script>
<style scoped></style>
