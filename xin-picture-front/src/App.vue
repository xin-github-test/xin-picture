<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>
<script setup lang="ts">
import BasicLayout from './layouts/BasicLayout.vue'
import { useLoginUserStore } from './stores/useLoginUserStore'

const loginUserStore = useLoginUserStore()
//获取登陆用户信息
loginUserStore.fetchLoginUser()
</script>
<style scoped></style>
