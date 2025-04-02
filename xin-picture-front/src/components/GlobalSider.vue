<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      breakpoint="lg"
      collapsed-width="0"
      width="200"
    >
      <a-menu v-model:selectedKeys="current" mode="inline" :items="menuItems" @click="doMenuClick">
      </a-menu>
    </a-layout-sider>
  </div>
</template>

<script lang="ts" setup>
import { h, ref } from 'vue'
import { PictureOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
//获取存储的全局状态（用户登陆信息）
const loginUserStore = useLoginUserStore()
//原始菜单
const menuItems = [
  {
    key: '/',
    icon: () => h(PictureOutlined, { style: { fontSize: '20px' } }),
    label: '公共图库',
    style: {
      fontSize: '20px',
    },
  },
  {
    key: '/my_space',
    icon: () => h(UserOutlined, { style: { fontSize: '20px' } }),
    label: '我的空间',
    style: {
      fontSize: '20px',
    },
  },
]

const router = useRouter()
const current = ref<string[]>([])
//路由跳转事件
const doMenuClick = ({ key }: any) => {
  router.push({
    path: key,
  })
}
//路由守卫
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>

<style scoped></style>
