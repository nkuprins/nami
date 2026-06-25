<script setup lang="ts">
defineProps<{ parts: string[] }>();
</script>

<template>
  <div class="flex items-center flex-wrap gap-x-3 gap-y-1 tabular">
    <span
      v-for="(part, i) in parts"
      :key="i"
      class="inline-flex items-center gap-3"
    >
      {{ part }}
      <span v-if="i < parts.length - 1" class="text-ink-3" aria-hidden="true"
        >&middot;</span
      >
    </span>
  </div>
</template>
