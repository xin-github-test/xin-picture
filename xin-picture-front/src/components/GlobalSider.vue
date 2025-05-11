<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      breakpoint="lg"
      collapsed-width="0"
      width="200"
      style="background: white; margin-top: 0px"
    >
      <a-menu v-model:selectedKeys="current" mode="inline" :items="menuItems" @click="doMenuClick">
      </a-menu>
    </a-layout-sider>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue'
import { PictureOutlined, StarOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { SPACE_TYPE_ENUM } from '@/constants/space'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController'
import { message } from 'ant-design-vue'
//获取存储的全局状态（用户登陆信息）
const loginUserStore = useLoginUserStore()
//固定菜单项
const fixedMenuItems = [
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
  {
    key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM,
    icon: () => h(TeamOutlined, { style: { fontSize: '20px' } }),
    label: '创建团队',
    style: {
      fontSize: '20px',
    },
  },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])
const menuItems = computed(() => {
  // 没有团队空间，只展示固定菜单
  if (teamSpaceList.value.length < 1) {
    return fixedMenuItems
  }
  // 展示团队空间分组
  const teamSpaceSubMenus = teamSpaceList.value.map((spaceUser) => {
    const space = spaceUser.space
    return {
      key: '/space/' + spaceUser.spaceId,
      label: space?.spaceName,
      icon: () => h(StarOutlined, { style: { fontSize: '16px' } }),
      style: {
        fontSize: '16px',
      },
    }
  })
  const teamSpaceMenuGroup = {
    type: 'group',
    label: '我的团队',
    key: 'teamSpace',
    children: teamSpaceSubMenus,
  }
  return [...fixedMenuItems, teamSpaceMenuGroup]
})

// 加载团队空间列表
const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  } else {
    message.error('加载我的团队空间失败，' + res.data.message)
  }
}

/**
 * 监听变量，改变时触发数据的重新加载
 */
watchEffect(() => {
  // 登录才加载
  if (loginUserStore.loginUser.id) {
    fetchTeamSpaceList()
  }
})

const router = useRouter()
const current = ref<string[]>([])
//路由跳转事件
const doMenuClick = ({ key }: any) => {
  router.push(key)
}
//路由守卫
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>

<style scoped>
:deep(.ant-menu-item-group-title) {
  font-size: 20px !important;
}
</style>
