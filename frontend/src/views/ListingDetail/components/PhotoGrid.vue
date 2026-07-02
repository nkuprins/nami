<script setup lang="ts">
import { computed, ref } from 'vue';
import PhotoLightBox from '../../../components/listing/PhotoLightBox.vue';
import IconPlayer from '../../../components/icons/IconPlayer.vue';
import { getVideoThumbnailUrl } from '../../../utils/video';

const props = defineProps<{
  photos: string[];
  alt: string;
  videoUrl?: string;
}>();

const emit = defineEmits<{ 'play-video': [] }>();

const GRID_MAX_VISIBLE = 9;

const videoThumb = computed(() =>
  props.videoUrl ? getVideoThumbnailUrl(props.videoUrl) : ''
);

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
        v-for="(src, i) in photos.slice(0, GRID_MAX_VISIBLE)"
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
          v-if="i === GRID_MAX_VISIBLE - 1 && photos.length > GRID_MAX_VISIBLE"
          class="absolute inset-0 bg-ink/60 backdrop-blur-sm flex items-center justify-center"
        >
          <span class="text-cream text-xl font-semibold tabular"
            >+{{ photos.length - GRID_MAX_VISIBLE }}</span
          >
        </div>
      </div>

      <!-- Video thumbnail tile -->
      <div
        v-if="videoUrl"
        class="relative overflow-hidden rounded-lg cursor-pointer bg-surface group aspect-square"
        @click="emit('play-video')"
      >
        <img
          v-if="videoThumb"
          :src="videoThumb"
          :alt="`${alt} — video tour`"
          class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.04]"
          loading="lazy"
        />
        <div
          v-else
          class="absolute inset-0 bg-linear-to-br from-surface via-bg to-cream"
        />
        <div
          class="absolute inset-0 bg-black/25 transition-opacity group-hover:bg-black/30"
        />
        <div class="absolute inset-0 flex items-center justify-center">
          <span
            class="flex size-10 items-center justify-center rounded-full bg-white/90 text-ink shadow-lg transition-transform group-hover:scale-105"
          >
            <IconPlayer />
          </span>
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
