<template>
  <div id="spaceManagePage">
    <a-flex justify="space-between">
      <h2>空间成员管理</h2>
      <!-- <a-space>
        <a-button type="primary" href="/add_space" target="_blank">+ 创建空间</a-button>
        <a-button type="primary" ghost href="/space_analyze?queryPublic=1" target="_blank"
          >分析公共图库</a-button
        >
        <a-button type="primary" ghost href="/space_analyze?queryAll=1" target="_blank"
          >分析全部空间</a-button
        >
      </a-space> -->
    </a-flex>
    <div style="margin-bottom: 16px" />
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="formData" @finish="handleSubmit">
      <a-form-item label="用户 id" name="userId">
        <a-input v-model:value="formData.userId" placeholder="请输入用户 id" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">添加用户</a-button>
      </a-form-item>
    </a-form>

    <div style="margin-bottom: 32px"></div>

    <!-- 数据展示表格 -->
    <a-table :columns="columns" :data-source="dataList">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userInfo'">
          <a-space>
            <a-avatar :src="record.user?.userAvatar" />
            {{ record.user?.userName
            }}<a-tag color="green" v-if="record.userId == record.space.userId">创建者</a-tag>
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'spaceRole'">
          <a-select
            :disabled="record.userId == record.space.userId"
            v-model:value="record.spaceRole"
            :options="SPACE_ROLE_OPTIONS"
            @change="(value) => editSpaceRole(value, record)"
          />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap v-if="record.userId != record.space.userId">
            <a-button type="link" danger @click="doDelete(record.id)">退队</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { deleteSpaceUsingPost, listSpaceByPageUsingPost } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { SPACE_LEVEL_MAP, SPACE_LEVEL_OPTIONS } from '@/constants/space'
import { formatSize } from '@/utils'
import { SPACE_ROLE_MAP, SPACE_ROLE_OPTIONS } from '@/constants/space'
import {
  addSpaceUserUsingPost,
  deleteSpaceUserUsingPost,
  editSpaceUserUsingPost,
  listSpaceUserUsingPost,
} from '@/api/spaceUserController'
// 表格列
const columns = [
  {
    title: '用户',
    dataIndex: 'userInfo',
  },
  {
    title: '角色',
    dataIndex: 'spaceRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]
interface Props {
  id: string
}
//定义数据
const dataList = ref<API.SpaceUserVO[]>([])
const props = defineProps<Props>()
//获取数据
const fetchData = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await listSpaceUserUsingPost({
    spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

//声明周期函数
onMounted(() => {
  //页面加载时获取数据
  fetchData()
})
//编辑
const editSpaceRole = async (value: number, record: API.SpaceUserVO) => {
  const res = await editSpaceUserUsingPost({ id: record.id, spaceRole: value })
  if (res.data.code === 0) {
    message.success('修改成功')
  } else {
    message.error('修改失败，' + res.data.message)
  }
}
//新增
// 添加用户
const formData = reactive<API.SpaceUserAddRequest>({})

const handleSubmit = async () => {
  const spaceId = props.id
  if (!spaceId) {
    return
  }
  const res = await addSpaceUserUsingPost({
    spaceId,
    ...formData,
  })
  if (res.data.code === 0) {
    message.success('添加成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('添加失败，' + res.data.message)
  }
}

//删除
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteSpaceUserUsingPost({ id })

  if (res.data.code === 0) {
    message.success('删除成功')
    //删除成功后重新获取数据
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}
</script>
