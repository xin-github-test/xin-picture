<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.svg" alt="logo" />
            <div class="title">在线图库平台</div>
          </div>
        </RouterLink>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="120px">
        <!-- 若是用户已经登陆则显示用户登陆信息 -->
        <div v-if="loginUserStore.loginUser.id">
          {{ loginUserStore.loginUser.userName ?? '无名法师' }}
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login">登陆</a-button>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { h, ref } from 'vue'
import {
  MailOutlined,
  AppstoreOutlined,
  SettingOutlined,
  HomeOutlined,
} from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
//获取存储的全局状态（用户登陆信息）
const loginUserStore = useLoginUserStore()

const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
  {
    key: 'others',
    label: h('a', { href: 'https://xinmini.us.kg', target: '_blank' }, 'xin的博客'),
    title: '作者博客',
  },
])

const router = useRouter()
const current = ref<string[]>([])
//路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>
<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
}
.logo {
  height: 48px;
}
.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}
</style>
