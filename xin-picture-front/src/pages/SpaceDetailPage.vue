<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }} {{ SPACE_TYPE_MAP[space.spaceType] }}</h2>
      <a-space size="middle">
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            :size="42"
            type="circle"
            :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
            :width="80"
          />
        </a-tooltip>
        <a-button v-if="canUploadPicture" type="primary" :href="`/add_picture?spaceId=${space.id}`"
          >创建图片</a-button
        >
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :href="`/spaceUserManager/${space.id}`"
          >成员管理</a-button
        >
        <a-button
          v-if="canManageSpaceUser"
          type="primary"
          ghost
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
          target="_blank"
          >空间分析</a-button
        >
        <a-button :icon="h(EditOutlined)" @click="doBatchEdit">批量编辑</a-button>
      </a-space>
    </a-flex>
    <!-- 搜索表单 -->
    <PictureSearchForm :onSearch="onSearch" />
    <div style="margin-bottom: 24px" />
    <!-- 按颜色搜索，跟其他搜索条件独立 -->
    <a-form-item label="按颜色搜索">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>

    <!-- 图片列表 -->
    <PictureList
      :canEdit="canDeletePicture"
      :canDelete="canDeletePicture"
      :loading="loading"
      :dataList="dataList"
      :showOp="true"
      :onReload="fetchData"
    />
    <!-- 分页 -->
    <a-pagination
      v-if="dataList.length > 0"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
      style="padding-bottom: 12px; text-align: right"
    />
    <BatchEditPictureModal
      ref="batchEditModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
  </div>
</template>
<script setup lang="ts">
import { BarChartOutlined, EditOutlined } from '@ant-design/icons-vue'
import {
  listPictureVoByPageUsingPost,
  listPictureVoByPageWithCaffeCacheUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { formatSize } from '@/utils'
import { message } from 'ant-design-vue'
import { onMounted, ref, h, computed, reactive, watch } from 'vue'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_MAP } from '@/constants/space'

interface Props {
  id: string | number
}
const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

//获取空间详情
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id: props.id as number })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败,' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间详情失败,' + error.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

//===============获取图片列表
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(false)

//搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})
//获取数据
const fetchData = async () => {
  loading.value = true
  //转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }

  const res = await listPictureVoByPageUsingPost(params)
  //使用缓存接口-redis
  // const res = await listPictureVoByPageWithCacheUsingPost(params)
  //使用本地缓存caffeine
  // const res = await listPictureVoByPageWithCaffeCacheUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
onMounted(() => {
  fetchData()
})
//定义分页器,当searchParams变化时，computed会重新计算并响应回页面
const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}
//触发搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  fetchData()
}
//颜色搜索
const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id,
  })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    dataList.value = data
    total.value = data.length
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

//=====批量编辑图片
const batchEditModalRef = ref()
const onBatchEditPictureSuccess = () => {
  fetchData()
}
const doBatchEdit = () => {
  if (batchEditModalRef.value) {
    batchEditModalRef.value?.openModal()
  }
}

//当空间id发生变化时，重新获取页面数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  margin-bottom: 16px;
}
#spaceDetailPage :deep(.ant-descriptions-item-label) {
  font-size: 22px;
}
#spaceDetailPage :deep(.ant-descriptions-item-content) {
  font-size: 22px;
}
#spaceDetailPage :deep(.ant-space) {
  font-size: 22px;
}
#spaceDetailPage :deep(.ant-tag) {
  font-size: 20px;
  height: 100%;
  color: green;
  vertical-align: middle;
  line-height: 37px;
}
</style>
