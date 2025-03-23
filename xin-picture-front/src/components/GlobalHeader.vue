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
          <a-dropdown>
            <a-space>
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              <span style="font-size: 20px">{{
                loginUserStore.loginUser.userName ?? '无名法师'
              }}</span>
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登陆
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" href="/user/login">登陆</a-button>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import { message, type MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { userLogoutUsingPost } from '@/api/userController'
//获取存储的全局状态（用户登陆信息）
const loginUserStore = useLoginUserStore()
//原始菜单
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined, { style: { fontSize: '20px' } }),
    label: '主页',
    title: '主页',
    style: {
      fontSize: '20px',
    },
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
    style: {
      fontSize: '20px',
    },
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
    style: {
      fontSize: '20px',
    },
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
    style: {
      fontSize: '20px',
    },
  },

  //图片相关
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
    style: {
      fontSize: '20px',
    },
  },

  {
    key: 'others',
    label: h('a', { href: 'https://blog.xinmix.ddns-ip.net', target: '_blank' }, 'xin的博客'),
    title: '作者博客',
    style: {
      fontSize: '20px',
    },
  },
]
//过滤后的菜单
const items = computed(() => filterMenu(originItems))

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
//过滤菜单（根据权限）
const filterMenu = (menus = [] as MenuProps['items']) => {
  //管理员才能看到
  return menus?.filter((menu) => {
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}
//退出登陆
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    //清除状态信息
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登陆成功')
    await router.push('/user/login')
  } else {
    message.error('退出登陆失败，' + res.data.message)
  }
}
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
