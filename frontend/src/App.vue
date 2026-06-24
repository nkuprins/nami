<script setup lang="ts">
import TopNav from './components/layout/TopNav.vue';
import AppFooter from './components/layout/AppFooter.vue';
</script>

<template>
  <div class="min-h-screen flex flex-col bg-bg text-ink font-sans">
    <TopNav />
    <main class="flex-1">
      <RouterView />
    </main>
    <AppFooter />
  </div>
</template>
