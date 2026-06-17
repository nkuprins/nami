<script setup lang="ts">
import {onMounted} from 'vue';
import TopNav from './components/layout/TopNav.vue';
import {initAuth} from './composables/useAuth';

onMounted(initAuth);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-bg text-ink font-sans">
    <TopNav/>
    <main class="flex-1">
      <RouterView/>
    </main>
  </div>
</template>
