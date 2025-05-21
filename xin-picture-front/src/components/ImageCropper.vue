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
      :img="imgUrl"
      :autoCrop="true"
      :fixedBox="false"
      :centerBox="true"
      :canMoveBox="true"
      :info="true"
      outputType="png"
    />
    <div style="margin-bottom: 16px" />
    <!-- 图片协同编辑操作 -->
    <div class="image-edit-actions" v-if="isTeamSpace">
      <a-space>
        <a-button v-if="editingUser" @click="changeScale(1)" disabled
          >{{ editingUser.userName }} 正在编辑</a-button
        >
        <a-button v-if="canEnterEdit" type="primary" ghost @click="enterEdit">进入编辑</a-button>
        <a-button v-if="canExitEdit" type="danger" ghost @click="exitEdit">退出编辑</a-button>
      </a-space>
    </div>
    <div style="margin-bottom: 16px" />
    <!-- 图片操作 -->
    <div class="image-cropper-actions">
      <a-space>
        <a-button @click="rotateLeft" :disabled="!canEdit">向左旋转</a-button>
        <a-button @click="rotateRight" :disabled="!canEdit">向右旋转</a-button>
        <a-button @click="changeScale(1)" :disabled="!canEdit">放大</a-button>
        <a-button @click="changeScale(-1)" :disabled="!canEdit">缩小</a-button>
        <a-button type="primary" :loading="loading" :disabled="!canEdit" @click="handleConfirm"
          >确认</a-button
        >
      </a-space>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { getPictureVoByIdUsingGet, uploadPictureUsingPost } from '@/api/pictureController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { computed, onUnmounted, reactive, ref, watch, watchEffect } from 'vue'
import PictureEditWebSocket from '@/utils/PictureEditWebSocket'
import { PICTURE_EDIT_ACTION_ENUM, PICTURE_EDIT_MESSAGE_TYPE_ENUM } from '@/constants/picture'
import { SPACE_TYPE_ENUM } from '@/constants/space'

interface Props {
  imageUrl?: string
  picture?: API.PictureVO
  spaceId?: number
  space?: API.SpaceVO
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const imgUrl = ref<string>(props.imageUrl || '')
//是否为团队空间
const isTeamSpace = computed(() => props.space?.spaceType === SPACE_TYPE_ENUM.TEAM)

// 编辑器组件的引用
const cropperRef = ref()

// 向左旋转
const rotateLeft = () => {
  cropperRef.value.rotateLeft()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT)
}

// 向右旋转
const rotateRight = () => {
  cropperRef.value.rotateRight()
  editAction(PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT)
}

// 缩放
const changeScale = (num: number) => {
  cropperRef.value.changeScale(num)
  if (num > 0) {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_IN)
  } else {
    editAction(PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT)
  }
}
const visible = ref(false)
const openModal = () => {
  imgUrl.value = props.imageUrl || ''
  visible.value = true
}
const closeModal = () => {
  visible.value = false
  message.info('已退出会话！')
  editingUser.value = undefined
  //关闭弹窗，同时断开 websocket 连接
  if (websocket) {
    //执行退出编辑
    websocket.disconnect()
  }
}

//监听 visible 变化，当 visible 为 true 时，初始化 websocket 连接
// watchEffect(() => {
//   if (visible.value) {
//     initWebSocket()
//   }
// })

//---------------协同编辑----------------
//监听 websocket 接收到的消息

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
      //发送通知给其他用户
      websocket?.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_ACTION })
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

//---------------实时编辑----------------
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

// 正在编辑的用户
const editingUser = ref<API.UserVO>()
// 当前用户是否可进入编辑
const canEnterEdit = computed(() => {
  return !editingUser.value
})
//当前用户是否可退出编辑
const canExitEdit = computed(() => {
  return editingUser.value?.id === loginUser?.id
})
//可以点击编辑图片按钮
const canEdit = computed(() => {
  //不是团队空间，默认可以使用编辑
  if (!isTeamSpace.value) return true
  //是团队空间，判断当前用户是否为正在编辑的用户
  return editingUser.value?.id === loginUser?.id
})

//编写 WebSocket 逻辑
let websocket: PictureEditWebSocket | null

//初始化 Websocket 连接，绑定监听事件
const initWebSocket = () => {
  const pictureId = props.picture?.id
  if (!pictureId || !visible.value) return

  //防止之前的连接未关闭
  if (websocket) {
    websocket.disconnect()
  }

  //创建websocket实例
  websocket = new PictureEditWebSocket(pictureId)
  websocket.connect()
  //TODO 同步问题需要解决，后进来的用户的无法看到当前用户的编辑状态以及图片状态
  //监听一系列事件
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.INFO, (msg) => {
    console.log('收到通知消息：', msg)
    message.info(msg.message)
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ERROR, (msg) => {
    console.log('收到错误通知：', msg)
    message.error(msg.message)
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT, (msg) => {
    console.log('收到进入编辑状态通知：', msg)
    message.info(msg.message)
    editingUser.value = msg.user
  })
  //获取当前正在编辑的用户
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.GET_EDITING_USER, (msg) => {
    console.log('收到获取正在编辑用户通知：', msg)
    message.info(msg.message)
    editingUser.value = msg.user
  })
  //用户保存图片的通知
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.SAVE_ACTION, async (msg) => {
    console.log('收到用户保存图片的通知：', msg)
    message.info(msg.message)
    editingUser.value = undefined
    //重新获取图片
    const res = await getPictureVoByIdUsingGet({ id: props.picture?.id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      const picture = { ...data, url: data.url?.split('?')[0] }
      imgUrl.value = picture.url || ''
      props.onSuccess?.(picture)
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION, (msg) => {
    console.log('收到编辑操作通知：', msg)
    message.info(msg.message)
    //根据收到的编辑操作，执行相应的操作
    switch (msg.editAction) {
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_LEFT:
        rotateLeft()
        break
      case PICTURE_EDIT_ACTION_ENUM.ROTATE_RIGHT:
        rotateRight()
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_IN:
        changeScale(1)
        break
      case PICTURE_EDIT_ACTION_ENUM.ZOOM_OUT:
        changeScale(-1)
        break
    }
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT, (msg) => {
    editingUser.value = undefined
    console.log('收到退出编辑状态通知：', msg)
    message.info(msg.message)
  })
  websocket.on(PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_SESSION, (msg) => {
    if (!(msg.user && editingUser.value?.id === msg.user.id)) {
      editingUser.value = undefined
    }
    console.log('收到退出会话通知：', msg)
    message.info(msg.message)
  })
}
watch(
  () => [isTeamSpace.value, visible.value],
  () => {
    //只有是团队空间，才需要初始化 WebSocket 连接
    if (isTeamSpace.value && visible.value) {
      initWebSocket()
    }
  },
)

//销毁 WebSocket 连接
onUnmounted(() => {
  if (websocket) {
    websocket.disconnect()
  }
  editingUser.value = undefined
})
//进入编辑
const enterEdit = () => {
  if (websocket) {
    websocket.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.ENTER_EDIT })
  }
}
//图片编辑
const editAction = (action: string) => {
  if (websocket) {
    websocket.sendMessage({
      type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EDIT_ACTION,
      editAction: action,
    })
  }
}
//退出编辑
const exitEdit = () => {
  if (websocket) {
    websocket.sendMessage({ type: PICTURE_EDIT_MESSAGE_TYPE_ENUM.EXIT_EDIT })
  }
}
//获取当前正在编辑的用户
</script>

<style>
.image-cropper {
  text-align: center;
}

.image-cropper .vue-cropper {
  height: 400px !important;
}
</style>
