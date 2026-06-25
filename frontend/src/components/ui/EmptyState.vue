<script setup lang="ts">
defineProps<{
  message: string;
}>();
</script>

<template>
  <div
    class="flex flex-col items-center justify-center gap-3 py-16 text-center"
  >
    <slot name="icon" />
    <p class="text-sm text-ink-2">{{ message }}</p>
    <slot name="action" />
  </div>
</template>
