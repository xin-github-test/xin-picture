<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header"><GlobalHeader /></a-layout-header>
      <a-layout-content class="content"><RouterView /></a-layout-content>
      <a-layout-footer class="footer">
        <a href="https://xinmini.us.kg" target="_blank">xin-picture by xin</a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import type { CSSProperties } from 'vue'

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
  background: linear-gradient(to right, #fefefe, #fff);
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
}
#basicLayout .header {
  padding-inline: 20px;
  background: white;
  color: unset;
  margin-bottom: 16px;
}
</style>
