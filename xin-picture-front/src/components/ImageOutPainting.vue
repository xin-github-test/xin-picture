<template>
  <a-modal
    class="image-out-painting"
    v-model:visible="visible"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
    style="width: 1400px; height: 1800px"
  >
    <a-row gutter="16">
      <a-col span="12">
        <h4>原始图片</h4>
        <img :src="picture?.url" :alt="picture?.name" style="max-width: 100%" />
      </a-col>
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          :alt="picture?.name"
          style="max-width: 100%"
        />
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px" />
    <a-flex justify="center" gap="16">
      <a-button type="primary" :loading="!!taskId" ghost @click="createTask">生成图片</a-button>
      <a-button v-if="resultImageUrl" type="primary" :loading="uploadLoading" @click="handleUpload"
        >应用结果</a-button
      >
    </a-flex>
  </a-modal>
</template>

<script setup lang="ts">
import {
  createPictureOutPaintingTaskUsingPost,
  getPictureOutPaintingTaskUsingGet,
  uploadPictureByUrlUsingPost,
  uploadPictureUsingPost,
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

interface Props {
  picture?: API.PictureVO
  spaceId?: string
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const resultImageUrl = ref<string>('')
//任务id
const taskId = ref<string>('')

const visible = ref(false)
const openModal = () => {
  visible.value = true
}
const closeModal = () => {
  visible.value = false
}

//暴露函数给父组件调用
defineExpose({
  openModal,
})

const uploadLoading = ref(false)
const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    //调用后端接口上传图片
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      //将上传的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败,' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败, ' + error.message)
  }

  uploadLoading.value = false
}

//创建AI扩图任务
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    //根据需要设置扩图参数
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('扩图任务创建成功,正在加速生成,请勿退出界面！')
    //保存任务id，后续需要通过任务id获取任务的执行结果
    taskId.value = res.data.data.output?.taskId
    //通过任务id,定时查询任务执行结果
    startPolling()
  } else {
    message.error('任务创建失败,' + res.data.message)
  }
}

//轮询定时器
let pollingTimer: NodeJS.Timeout = null

//开启轮询
const startPolling = () => {
  if (!taskId.value) {
    return
  }
  //记录定时任务的开始时间，只允许执行1分钟，超过自动清理定时任务
  const pollingStartTime = Date.now() // 记录任务开始时间
  const MAX_POLLING_TIME = 60 * 1000 // 最大轮询时间（1分钟）
  pollingTimer = setInterval(async () => {
    try {
      if (Date.now() - pollingStartTime > MAX_POLLING_TIME) {
        message.error('任务超时')
        clearPolling()
        return // 超时后停止轮询
      }
      const res = await getPictureOutPaintingTaskUsingGet({ taskId: taskId.value })
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult?.taskStatus === 'SUCCEEDED') {
          message.success('扩图任务执行成功')
          resultImageUrl.value = taskResult?.outputImageUrl as string
          //任务执行失败，清理轮询
          clearPolling()
        } else if (taskResult?.taskStatus === 'FAILED') {
          message.error('扩图任务执行失败！' + res.data.data.output?.message)
          //任务执行失败，清理轮询
          clearPolling()
        }
      }
    } catch (error) {
      console.error('扩图任务轮询失败', error)
      message.error('扩图任务轮询失败, ' + error.message)
      //任务执行失败，清理轮询
      clearPolling()
    }
  }, 3000) //每 3 秒查询一次任务执行结果
}
//清理轮询
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = ''
  }
}
</script>

<style>
.image-out-painting {
  text-align: center;
}
</style>
