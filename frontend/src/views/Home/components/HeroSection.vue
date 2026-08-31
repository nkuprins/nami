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
        src="/hero-riga.jpg"
        alt=""
        class="size-full object-cover saturate-[0.85]"
      />
      <div class="absolute inset-0 bg-accent-2/15 mix-blend-overlay" />
      <div
        class="absolute inset-0 bg-linear-to-b from-ink/40 via-ink/25 to-ink/85"
      />
    </div>
    <div class="absolute inset-0 -z-10 grain pointer-events-none" />

    <div class="relative w-full mx-auto max-w-7xl px-6 lg:px-10 py-6">
      <SearchPanel @search="emit('search')" @open-more="emit('openMore')" />
    </div>

    <!-- CC BY-SA 3.0 image; the visible credit is a license requirement -->
    <a
      href="https://commons.wikimedia.org/wiki/File:Riga_Skyline_Panorama,_Latvia_-_Diliff.jpg"
      target="_blank"
      rel="noopener"
      class="absolute bottom-1.5 right-2 text-[0.625rem] text-cream/50 hover:text-cream/80 transition-colors"
    >
      Photo: Diliff / CC BY-SA 3.0
    </a>
  </section>
</template>
