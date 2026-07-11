<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  normalizeVideoEmbedUrl,
  getVideoThumbnailUrl,
} from '../../../utils/video';
import IconPlayer from '../../../components/icons/IconPlayer.vue';

const props = defineProps<{
  videoUrl: string;
  alt: string;
}>();

const { t } = useI18n();

const videoHasError = ref(false);
const videoExpanded = ref(false);

function handleVideoError() {
  videoHasError.value = true;
}

const videoEmbedUrl = computed(() => normalizeVideoEmbedUrl(props.videoUrl));

const videoThumbnailUrl = computed(() => getVideoThumbnailUrl(props.videoUrl));
</script>

<template>
  <div
    v-if="videoHasError"
    class="relative aspect-video overflow-hidden rounded-xl bg-surface flex flex-col items-center justify-center text-center p-6"
  >
    <i class="ti ti-video-off text-2xl text-ink-3 mb-2" aria-hidden="true" />
    <p class="text-sm font-medium text-ink">
      {{ t('listing.videoUnavailable') }}
    </p>
    <p class="text-xs text-ink-2 mt-1 max-w-xs">
      {{ t('listing.videoUnavailableDesc') }}
    </p>
  </div>

  <div
    v-else
    class="relative aspect-video overflow-hidden rounded-xl bg-black"
  >
    <Transition name="fade" mode="out-in">
      <button
        v-if="!videoExpanded"
        type="button"
        class="group absolute inset-0 h-full w-full bg-surface text-left focus-ring border-none p-0 cursor-pointer"
        :aria-expanded="videoExpanded"
        @click="videoExpanded = true"
      >
        <img
          v-if="videoThumbnailUrl"
          :src="videoThumbnailUrl"
          :alt="`${alt} — ${t('listing.videoTour')}`"
          class="absolute inset-0 h-full w-full object-cover"
          @error="handleVideoError"
        />
        <div
          v-else
          class="absolute inset-0 bg-linear-to-br from-surface via-bg to-cream"
        />
        <div
          class="absolute inset-0 bg-black/20 transition-opacity group-hover:bg-black/25"
        />

        <div class="absolute inset-0 flex items-center justify-center">
          <span
            class="flex size-14 items-center justify-center rounded-full bg-white/90 text-ink shadow-lift transition-transform group-hover:scale-105"
          >
            <IconPlayer />
          </span>
        </div>
      </button>

      <iframe
        v-else
        :src="`${videoEmbedUrl}&autoplay=1`"
        class="h-full w-full border-none"
        :title="t('listing.videoTour')"
        allow="
          accelerometer;
          autoplay;
          clipboard-write;
          encrypted-media;
          gyroscope;
          picture-in-picture;
          web-share;
        "
        allowfullscreen
      />
    </Transition>
  </div>
</template>
