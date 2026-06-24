<script setup lang="ts">
defineProps<{ phoneRevealed: boolean }>();
defineEmits<{ (e: 'reveal-phone'): void }>();
</script>

<template>
  <div>
    <div class="flex items-center gap-3 mb-4">
      <div
        class="w-10 h-10 rounded-full bg-surface border border-line flex items-center justify-center text-sm font-medium text-ink-2 shrink-0"
      >
        AV
      </div>
      <p class="text-sm font-medium text-ink">Andris Veinbergs</p>
    </div>
    <button
      class="w-full flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-ink text-sm font-medium rounded-lg border border-line cursor-pointer hover:bg-surface transition-colors"
      @click="$emit('reveal-phone')"
    >
      <i class="ti ti-phone text-sm" aria-hidden="true" />
      {{ phoneRevealed ? '+371 29 XXX XXX' : 'Show number' }}
    </button>
  </div>
</template>
