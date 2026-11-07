<script setup lang="ts">
import AppNavbar from '~/components/AppNavbar.vue';
import AppFooter from '~/components/AppFooter.vue';
</script>

<template>
  <div id="app-layout" class="min-vh-100 d-flex flex-column">
    <AppNavbar />
    <main class="main-content flex-grow-1 animated-gradient-background">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
#app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-height: 100dvh;
  margin: 0;
  overflow-x: hidden;
}
main.main-content {
  flex: 1 0 auto;
  display: flex;
  flex-direction: column;
  width: 100%;
}
</style>
