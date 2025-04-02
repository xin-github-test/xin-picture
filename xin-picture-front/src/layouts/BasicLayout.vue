<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header"><GlobalHeader /></a-layout-header>
      <a-layout-content class="content"><RouterView /></a-layout-content>
      <a-layout-footer class="footer">
        <a href="https://blog.xinmix.ddns-ip.net" target="_blank">View the author Blog! </a>
        <img src="@/assets/svg/web.svg" style="width: 70px; height: 40px" />
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import type { CSSProperties } from 'vue'
import { SmileOutlined } from '@ant-design/icons-vue'

const headerStyle: CSSProperties = {
  textAlign: 'center',
  color: '#fff',
  height: 64,
  paddingInline: 50,
  lineHeight: '64px',
  backgroundColor: '#7dbcea',
}
</script>
<style scoped>
#basicLayout .content {
  padding: 20px;

  /* background: linear-gradient(to right, #fefefe, #fff); */
  background-image: url('/src/assets/svg/tree.svg');
  background-repeat: no-repeat;
  background-size: cover;

  margin-bottom: 28px;
}
#basicLayout .footer {
  background: #efefef;
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 16px;
  font-family: 'Lucida Calligraphy', cursive, serif, sans-serif;
}
#basicLayout .header {
  padding-inline: 20px;
  background: white;
  color: unset;
  margin-bottom: 16px;
}
</style>
