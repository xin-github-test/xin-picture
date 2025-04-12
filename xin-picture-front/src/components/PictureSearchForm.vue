<template>
  <div class="picture-search-form">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词">
        <a-input v-model:value="searchParams.searchText" allow-clear placeholder="从名称和简介搜索">
        </a-input>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          style="width: 120px"
          v-model:value="searchParams.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        ></a-auto-complete>
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          style="width: 120px"
          v-model:value="searchParams.tags"
          mode="tags"
          :options="tagOptions"
          placeholder="请输入标签"
          allow-clear
        ></a-select>
      </a-form-item>
      <a-form-item label="日期" name="dataRange">
        <a-range-picker
          style="width: 400px"
          show-time
          v-model:value="dateRange"
          :placeholder="['编辑开始时间', '编辑结束时间']"
          format="YYYY/MM/DD HH:mm:ss"
          :preset="rangePreset"
          @change="onRangeChange"
        />
      </a-form-item>
      <a-form-item name="name" label="名称">
        <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear></a-input>
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <a-input v-model:value="searchParams.name" placeholder="请输入简介" allow-clear></a-input>
      </a-form-item>

      <a-form-item name="picWidth" label="宽度">
        <a-input-number style="width: 80px" v-model:value="searchParams.picWidth" />
      </a-form-item>
      <a-form-item name="picHeight" label="高度">
        <a-input-number style="width: 80px" v-model:value="searchParams.picHeight" />
      </a-form-item>
      <a-form-item name="picFormat" label="格式">
        <a-input v-model:value="searchParams.picFormat" placeholder="请输入格式" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-space style="font-size: 18px">
          <a-button type="primary" style="width: 96px" html-type="submit">搜索</a-button>
          <a-button html-type="reset" @click="doClear">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController'

interface Props {
  onSearch: (searchParams: API.PictureQueryRequest) => void
}

const props = defineProps<Props>()
//搜索条件
const searchParams = reactive<API.PictureQueryRequest>({})

//搜索
const doSearch = () => {
  props.onSearch?.(searchParams)
}

const dateRange = ref<[]>([])
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates?.length >= 2) {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  } else {
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
  }
}

const rangePreset = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])

//从后端获取默认的分类和标签
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签列表失败，' + res.data.message)
  }
}
//当页面挂载完成之后，调用获取标签和分类的接口
onMounted(() => {
  getTagCategoryOptions()
})

//重置
const doClear = () => {
  //取消searchParams中的所有值
  Object.keys(searchParams).forEach((key) => {
    searchParams[key] = undefined
  })
  //日期筛选项单独清空
  dateRange.value = []
  //清空后重新搜索
  props.onSearch?.(searchParams)
}
</script>

<style scoped>
.picture-search-form .ant-form-item {
  margin-top: 16px;
}
</style>
