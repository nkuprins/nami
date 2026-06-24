<script setup lang="ts">
defineProps<{ filled?: boolean }>();
</script>

<template>
  <svg
    viewBox="0 0 24 24"
    aria-hidden="true"
    class="size-full"
    :fill="filled ? 'currentColor' : 'none'"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <path
      d="M12 20.4S3.6 14.7 3.6 9.1A4.6 4.6 0 0 1 12 6.3a4.6 4.6 0 0 1 8.4 2.8c0 5.6-8.4 11.3-8.4 11.3Z"
    />
  </svg>
</template>
