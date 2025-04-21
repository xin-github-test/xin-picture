<template>
  <div id="searchPicturePage">
    <h2 style="margin-bottom: 16px">以图搜图</h2>
    <h3 style="margin-bottom: 16px">原图</h3>
    <!-- 单张图片 -->
    <a-card hoverable style="width: 240px">
      <template #cover>
        <img style="height: 180px; object-fit: cover" :alt="picture.name" :src="picture.url" />
      </template>
    </a-card>
    <hr style="margin: 16px 0" />
    <h2 style="margin: 16px 0; font-style: italic">识图结果</h2>
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 5 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a :href="picture.fromUrl" target="_blank">
            <a-card hoverable>
              <template #cover>
                <img
                  style="height: 180px; object-fit: cover"
                  :alt="picture.name ?? '图片'"
                  :src="picture.thumbUrl"
                />
              </template>
            </a-card>
          </a>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>
<script setup lang="ts">
import {
  deletePictureUsingPost,
  getPictureVoByIdUsingGet,
  searchPictureByPictureUsingPost,
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { onMounted, ref, h, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
//TODO 以图搜图 需要完善，现在只能搜索png图片
const route = useRoute()
const pictureId = computed(() => {
  return route.query?.pictureId
})
const picture = ref<API.PictureVO>({})
const dataList = ref<API.ImageSearchResult[]>([])
const loading = ref(false)

//获取搜图结果
const fetchReslutData = async () => {
  loading.value = true
  try {
    const res = await searchPictureByPictureUsingPost({ pictureId: pictureId.value })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
    } else {
      message.error('获取搜图结果失败,' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取搜图结果失败,' + error.message)
  }
  loading.value = false
}
onMounted(() => {
  fetchReslutData()
})

//获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({ id: pictureId.value })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      console.log(picture.value)
    } else {
      message.error('获取图片详情失败,' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取图片详情失败,' + error.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})
</script>
<style scoped>
#searchPicturePage {
  margin-bottom: 16px;
}
#searchPicturePage :deep(.ant-descriptions-item-label) {
  font-size: 22px;
}
#searchPicturePage :deep(.ant-descriptions-item-content) {
  font-size: 22px;
}
#searchPicturePage :deep(.ant-space) {
  font-size: 22px;
}
#searchPicturePage :deep(.ant-tag) {
  font-size: 20px;
  height: 100%;
  color: green;
  vertical-align: middle;
  line-height: 37px;
}
</style>
