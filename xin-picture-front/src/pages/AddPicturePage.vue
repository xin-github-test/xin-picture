<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px; font-size: 24px">
      {{ route.query.id ? '修改图片' : '创建图片' }}
    </h2>
    <!-- 选择上传方式 -->
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <!-- 图片上传组件 -->
        <PictureUpload :picture="picture" :onSuccess="onSuccess"
      /></a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传"
        ><UrlPictureUpload :picture="picture" :onSuccess="onSuccess" />
      </a-tab-pane>
    </a-tabs>

    <!-- 图片信息表单 -->
    <!-- 搜索表单 -->
    <a-form
      v-if="picture"
      name="pictureForm"
      layout="vertical"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" allow-clear placeholder="请输入名称"> </a-input>
      </a-form-item>
      <a-form-item name="introduction" lable="简介">
        <a-textarea
          v-model:value="pictureForm.introduction"
          allow-clear
          :auto-size="{ minRows: 2, maxRows: 5 }"
          placeholder="请输入简介"
        ></a-textarea>
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete
          v-model:value="pictureForm.category"
          placeholder="请输入分类"
          :options="categoryOptions"
          allow-clear
        ></a-auto-complete>
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          :options="tagOptions"
          placeholder="请输入标签"
          allow-clear
        ></a-select>
      </a-form-item>
      <a-form-item>
        <a-button style="width: 100%" type="primary" html-type="submit">创建</a-button>
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
} from '@/api/pictureController'
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const picture = ref<API.PictureVO>()
const uploadType = ref<'file' | 'url'>('file')
const router = useRouter()

const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}
const pictureForm = reactive<API.PictureEditRequest>({})
const handleSubmit = async (values: any) => {
  //从picture中获取图片id
  const pictureId = picture.value?.id
  if (!pictureId) {
    message.error('请先上传图片')
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    ...values,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')
    //跳转到图片详情页
    router.push(`/picture/${pictureId}`)
  } else {
    message.error('创建失败，' + res.data.message)
  }
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

const route = useRoute()
//图片的编辑（获取老数据）
const getOldPictiure = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}
//页面刚挂载完之后就判断是新增还是编辑
onMounted(() => {
  getOldPictiure()
})
</script>

<style scoped>
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
  background-color: rgba(123, 90, 242, 0.1);
  padding: 32px;
  border-radius: 5%;
}
</style>
