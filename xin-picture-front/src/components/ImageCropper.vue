<template>
  <a-modal
    class="image-cropper"
    v-model:visible="visible"
    title="编辑图片"
    :footer="false"
    @cancel="closeModal"
  >
    <vue-cropper
      ref="cropperRef"
      :img="imageUrl"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 图片操作 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft">向左旋转</a-button>
        <a-button @click="rotateRight">向右旋转</a-button>
        <a-button @click="changeScale(1)">放大</a-button>
        <a-button @click="changeScale(-1)">缩小</a-button>
        <a-button type="primary" :loading="loading" @click="handleConfirm">确认</a-button>
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { uploadPictureUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { ref } from 'vue'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
}
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

const handleConfirm = () => {
  cropperRef.value.getCropBlob((blob: Blob) => {
    //blob为已裁剪好的文件
    const fileName = (props.picture?.name || 'image') + '.png'
    const file = new File([blob], fileName, { type: blob.type })
    //上传图片
    handleUpload({ file })
  })
}

/**
 * 上传图片
 * @param file 上传的文件
 */
const loading = ref(false)
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture?.id ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId
    //调用后端接口上传图片
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      //将上传的图片信息传递给父组件
      const picture = { ...res.data.data, url: res.data?.data.url.split('?')[0] }
      props.onSuccess?.(picture)
      closeModal()
    } else {
      message.error('图片上传失败,' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败, ' + error.message)
  }

  loading.value = false
}
</script>

<style>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}
</style>
