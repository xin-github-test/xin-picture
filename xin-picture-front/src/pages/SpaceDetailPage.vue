<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}(私有空间)</h2>
      <a-space size="middle">
        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress :size="42" type="circle" :percent="20" :width="80" />
        </a-tooltip>
        <a-button type="primary" :href="`/add_picture?spaceId=${space.id}`">创建图片</a-button>
      </a-space>
    </a-flex>
    <!-- 图片列表 -->
    <PictureList :loading="loading" :dataList="dataList" :showOp="true" :onReload="fetchData" />
    <!-- 分页 -->
    <a-pagination
      v-if="dataList.length > 0"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
      style="padding-bottom: 12px; text-align: right"
    />
  </div>
</template>
<script setup lang="ts">
import { listPictureVoByPageWithCaffeCacheUsingPost } from '@/api/pictureController'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { formatSize } from '@/utils'
import { message } from 'ant-design-vue'
import { onMounted, ref, h, computed, reactive } from 'vue'

interface Props {
  id: string | number
}
const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

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
const searchParams = reactive<API.PictureQueryRequest>({
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
    ...searchParams,
  }

  // const res = await listPictureVoByPageUsingPost(params)
  //使用缓存接口-redis
  // const res = await listPictureVoByPageWithCacheUsingPost(params)
  //使用本地缓存caffeine
  const res = await listPictureVoByPageWithCaffeCacheUsingPost(params)
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
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}
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
