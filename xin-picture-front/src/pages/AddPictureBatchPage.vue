<template>
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px; font-size: 24px">批量创建图片</h2>

    <!-- 图片信息表单 -->
    <!-- 搜索表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="searchText" label="关键词">
        <a-input v-model:value="formData.searchText" allow-clear placeholder="请输入关键词">
        </a-input>
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <a-input-number
          v-model:value="formData.count"
          allow-clear
          :auto-size="{ minRows: 2, maxRows: 5 }"
          placeholder="请输入数量"
          style="min-width: 180px"
          :min="1"
          :max="30"
        ></a-input-number>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="formData.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        ></a-auto-complete>
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="formData.tags"
          mode="tags"
          :options="tagOptions"
          placeholder="请输入标签"
          allow-clear
        ></a-select>
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <a-input
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀(会在名称前缀后自动补充序号)"
          allow-clear
        ></a-input>
      </a-form-item>
      <a-form-item>
        <a-button :loading="loading" style="width: 100%" type="primary" html-type="submit"
          >执行任务</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import {
  editPictureUsingPost,
  getPictureByIdUsingGet,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
  uploadPictureByBatchUsingPost,
} from '@/api/pictureController'
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const formData = reactive<API.PictureUploadByBatchRequest>({ count: 10, searchText: '壁纸' })

const handleSubmit = async (values: any) => {
  loading.value = true
  const res = await uploadPictureByBatchUsingPost({ ...formData })
  if (res.data.code === 0 && res.data.data) {
    message.success(`创建成功`)
    //跳转到主页
    router.push(`/`)
  } else {
    message.error('创建失败，' + res.data.message)
  }
  loading.value = false
}

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
</script>

<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
  background-color: rgba(123, 90, 242, 0.1);
  padding: 32px;
  border-radius: 5%;
}
</style>
