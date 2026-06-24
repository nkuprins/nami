<script setup lang="ts">
defineProps<{ dir?: 'down' | 'up' | 'left' | 'right' }>();
</script>

<template>
  <svg
    viewBox="0 0 24 24"
    aria-hidden="true"
    class="size-full transition-transform duration-150"
    :class="{
      'rotate-0': !dir || dir === 'down',
      'rotate-180': dir === 'up',
      'rotate-90': dir === 'left',
      '-rotate-90': dir === 'right',
    }"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <path d="m6 9 6 6 6-6" />
  </svg>
</template>
