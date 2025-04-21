<template>
  <div id="homePage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      ></a-input-search>
    </div>

    <!--分类和标签筛选  -->
    <a-tabs v-model:active-key="selectedCategory" size="large" @change="doSearch">
      <a-tab-pane tab="全部" key="all" />
      <a-tab-pane v-for="category in categoryList" :key="category" :tab="category" />
    </a-tabs>

    <!-- 图片标签 -->
    <div class="tag-bar">
      <span style="margin-right: 8px; font-size: 16px">标签:</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          style="font-size: 15px"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>

    <!-- 图片列表 -->
    <PictureList :loading="loading" :dataList="dataList" />
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
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
  listPictureVoByPageWithCaffeCacheUsingPost,
} from '@/api/pictureController'
import PictureList from '@/components/PictureList.vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

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
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  selectedTagList.value.forEach((checked, index) => {
    if (checked) {
      params.tags.push(tagList.value[index])
    }
  })
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
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}
//搜索
const doSearch = () => {
  //重置搜索条件
  searchParams.current = 1
  fetchData()
}
//从后端获取默认的分类和标签
const categoryList = ref<string[]>([])
const tagList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const selectedTagList = ref<boolean[]>([])

const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签列表失败，' + res.data.message)
  }
}
//当页面挂载完成之后，调用获取标签和分类的接口
onMounted(() => {
  getTagCategoryOptions()
})
</script>
<style scoped>
#homePage {
  margin-bottom: 16px;
}
#homePage .search-bar {
  max-width: 720px;
  margin: 0 auto 16px;
}
#homePage .tag-bar {
  margin-bottom: 20px;
}
</style>
