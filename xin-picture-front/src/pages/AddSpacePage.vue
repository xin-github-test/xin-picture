<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px; font-size: 24px">
      {{ route.query.id ? '修改' : '创建' }} {{ SPACE_TYPE_MAP[spaceType] }}
    </h2>

    <!-- 空间信息表单 -->
    <!-- 搜索表单 -->
    <a-form name="spaceForm" layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input v-model:value="spaceForm.spaceName" allow-clear placeholder="请输入空间名称">
        </a-input>
      </a-form-item>
      <a-form-item name="spaceLevel" lable="空间级别">
        <a-select
          style="min-width: 180px"
          v-model:value="spaceForm.spaceLevel"
          placeholder="请选择空间级别"
          :options="SPACE_LEVEL_OPTIONS"
          allow-clear
        ></a-select>
      </a-form-item>
      <a-form-item>
        <a-button :loading="loading" style="width: 100%" type="primary" html-type="submit"
          >提交</a-button
        >
      </a-form-item>
    </a-form>
    <!-- 空间级别介绍 -->
    <a-card title="空间级别介绍">
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系：
        <a href="blog.xinmix.ddns-ip.net" target="_blank">xin的博客</a>
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}: 大小{{ formatSize(spaceLevel.maxSize) }}, 数量{{
          spaceLevel.maxCount
        }}
      </a-typography-paragraph>
    </a-card>
  </div>
</template>
<script setup lang="ts">
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceLevelUsingGet,
  updateSpaceUsingPost,
} from '@/api/spaceController'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatSize } from '@/utils'
import {
  SPACE_LEVEL_MAP,
  SPACE_LEVEL_OPTIONS,
  SPACE_TYPE_ENUM,
  SPACE_TYPE_MAP,
} from '@/constants/space'
const space = ref<API.SpaceVO>()
const router = useRouter()
const loading = ref(false)
const spaceLevelList = ref<API.SpaceLevel[]>([])
const route = useRoute()
//根据路径的参数，判断是创建私人空间还是团队空间
const spaceType = computed(() => {
  if (route.query.type) {
    return Number(route.query.type)
  } else {
    return SPACE_TYPE_ENUM.PRIVATE
  }
})

//获取空间级别介绍
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别介绍失败,' + res.data.message)
  }
}
//加载完页面获取空间级别介绍
onMounted(() => {
  fetchSpaceLevelList()
})

const spaceForm = reactive<API.SpaceAddRequest | API.SpaceEditRequest>({})
const handleSubmit = async (values: any) => {
  //判断是更新表单，还是创建表单
  const spaceId = space.value?.id
  loading.value = true
  let res
  // 更新
  if (spaceId) {
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...values,
    })
  } else {
    // 创建
    res = await addSpaceUsingPost({
      spaceType: spaceType.value,
      ...values,
    })
  }

  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    //跳转到空间详情页
    router.push(`/space/${res.data.data}`)
  } else {
    message.error('操作失败，' + res.data.message)
  }
  loading.value = false
}

//空间的编辑（获取老数据）
const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data
      //填充表单
      spaceForm.spaceName = data.spaceName
      spaceForm.spaceLevel = data.spaceLevel
    }
  }
}
//页面刚挂载完之后就判断是新增还是编辑
onMounted(() => {
  getOldSpace()
})
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
  background-color: rgba(123, 90, 242, 0.1);
  padding: 32px;
  border-radius: 5%;
}
</style>
