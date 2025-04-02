<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="title-bar">
            <img class="logo" src="@/assets/logo.png" alt="logo" />
            <div class="title">星图库</div>
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
      <a-col flex="auto">
        <img src="@/assets/svg/2025.svg" alt="2025" style="height: 64px" />
      </a-col>
      <a-col flex="120px">
        <!-- 若是用户已经登陆则显示用户登陆信息 -->
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space>
              <a-avatar
                style="width: 45px; height: 45px"
                :src="loginUserStore.loginUser.userAvatar"
              />
              <span style="font-size: 20px">{{
                loginUserStore.loginUser.userName ?? '无名法师'
              }}</span>
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <router-link to="/my_space">
                    <UserOutlined />
                    我的空间
                  </router-link>
                </a-menu-item>

                <a-menu-item
                  v-if="loginUserStore.loginUser && loginUserStore.loginUser.userRole === 'admin'"
                >
                  <router-link to="/admin/userManage">
                    <TeamOutlined />
                    用户管理
                  </router-link>
                </a-menu-item>
                <a-menu-item
                  v-if="loginUserStore.loginUser && loginUserStore.loginUser.userRole === 'admin'"
                >
                  <router-link to="/admin/pictureManage">
                    <PictureOutlined />
                    图片管理
                  </router-link>
                </a-menu-item>
                <a-menu-item
                  v-if="loginUserStore.loginUser && loginUserStore.loginUser.userRole === 'admin'"
                >
                  <router-link to="/admin/spaceManage">
                    <SwitcherOutlined />
                    空间管理
                  </router-link>
                </a-menu-item>

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
import {
  HomeOutlined,
  UserOutlined,
  TeamOutlined,
  LogoutOutlined,
  PictureOutlined,
  SwitcherOutlined,
  PlusOutlined,
  QuestionOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
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
  //图片相关
  {
    key: '/add_picture',
    icon: () => h(PlusOutlined, { style: { fontSize: '20px' } }),
    label: '创建图片',
    title: '创建图片',
    style: {
      fontSize: '20px',
    },
  },
  {
    key: '/about',
    icon: () => h(QuestionCircleOutlined, { style: { fontSize: '20px' } }),
    label: '关于',
    title: '关于',
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
  background-image: linear-gradient(
    -60deg,
    transparent,
    #0ab9cf 14%,
    #7535e7 42%,
    /* #f9132f 42%, #ffbd00 70%, */ /* #72a043 70%, #0ab9cf 84%, */ rgba(0, 196, 204, 0) 100%
  );
  /* border-radius: 28px; */
}
.logo {
  margin-left: 16px;
  height: 48px;
}
.title {
  /* color: rgb(63, 61, 86); */
  color: rgb(253, 101, 132);
  font-size: 35px;
  margin-left: 8px;
  font-family: 'Lucida Calligraphy', cursive, serif, sans-serif;
}
</style>
