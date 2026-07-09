<script setup lang="ts">
import SearchPanel from './SearchPanel.vue';

const emit = defineEmits<{ search: []; openMore: [] }>();
</script>

<template>
  <section
    class="relative isolate overflow-hidden bg-ink flex items-center min-h-[300px] sm:min-h-[340px] lg:min-h-[380px]"
  >
    <div class="absolute inset-0 -z-10">
      <img
        src="https://picsum.photos/seed/baltnami-hero-rooftop/2400/1400"
        alt=""
        class="size-full object-cover"
      />
      <div
        class="absolute inset-0 bg-linear-to-b from-ink/30 via-ink/15 to-ink/85"
      />
    </div>
    <div class="absolute inset-0 -z-10 grain pointer-events-none" />

    <div class="relative w-full mx-auto max-w-7xl px-6 lg:px-10 py-6">
      <SearchPanel @search="emit('search')" @open-more="emit('openMore')" />
    </div>
  </section>
</template>
