<script setup lang="ts">
import { ref } from 'vue';
import PhotoLightBox from '../../../components/listing/PhotoLightBox.vue';

const props = defineProps<{
  photos: string[];
  alt: string;
}>();

const lightboxOpen = ref(false);
const lightboxIndex = ref(0);

function open(i: number) {
  lightboxIndex.value = i;
  lightboxOpen.value = true;
}
</script>

<template>
  <div>
    <div class="grid grid-cols-3 md:grid-cols-5 gap-1.5">
      <div
        v-for="(src, i) in photos"
        :key="i"
        class="relative overflow-hidden rounded-lg cursor-zoom-in bg-surface group aspect-square"
        @click="open(i)"
      >
        <img
          :src="src"
          :alt="`${alt} — photo ${i + 1}`"
          class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
          loading="lazy"
        />
        <div
          v-if="i === 8 && photos.length > 9"
          class="absolute inset-0 bg-ink/60 backdrop-blur-sm flex items-center justify-center"
        >
          <span class="text-cream text-xl font-semibold tabular"
            >+{{ photos.length - 9 }}</span
          >
        </div>
      </div>
    </div>

    <PhotoLightBox
      v-model:open="lightboxOpen"
      :photos="photos"
      :alt="alt"
      :initial-index="lightboxIndex"
    />
  </div>
</template>
