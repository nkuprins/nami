<script setup lang="ts">
defineProps<{ size?: number }>();
</script>

<template>
  <svg
      xmlns="http://www.w3.org/2000/svg"
      :width="15"
      :height="15"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
  >
    <path d="M3 6h18" />
    <path d="M19 6l-1 14H6L5 6" />
    <path d="M8 6V4h8v2" />
  </svg>
</template>