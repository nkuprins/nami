<script setup lang="ts">
import { ref, watch } from 'vue';
import IconChevron from '../icons/IconChevron.vue';
import PhotoLightBox from './PhotoLightBox.vue';

const props = defineProps<{
  photos: string[];
  alt: string;
  zoomable?: boolean;
}>();

const index = ref(0);
const direction = ref<'forward' | 'backward'>('forward');
const lightboxOpen = ref(false);

let touchStartX = 0;
let touchEndX = 0;
let didSwipe = false; // guard: suppress click after a swipe
const SWIPE_THRESHOLD = 50;

watch(
  () => props.photos,
  () => {
    index.value = 0;
  }
);

function next(e?: Event) {
  e?.preventDefault();
  e?.stopPropagation();
  if (props.photos.length < 2) return;
  direction.value = 'forward';
  index.value = (index.value + 1) % props.photos.length;
}

function prev(e?: Event) {
  e?.preventDefault();
  e?.stopPropagation();
  if (props.photos.length < 2) return;
  direction.value = 'backward';
  index.value = (index.value - 1 + props.photos.length) % props.photos.length;
}

function handleTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX;
  touchEndX = 0;
  didSwipe = false;
}

function handleTouchMove(e: TouchEvent) {
  touchEndX = e.touches[0].clientX;
}

function handleTouchEnd() {
  if (!touchEndX) return;
  const d = touchStartX - touchEndX;
  if (Math.abs(d) >= SWIPE_THRESHOLD) {
    didSwipe = true;
    d > 0 ? next() : prev();
  }
  touchStartX = 0;
  touchEndX = 0;
}

function handleClick() {
  if (didSwipe) {
    didSwipe = false;
    return;
  }
  if (props.zoomable) lightboxOpen.value = true;
}
</script>

<template>
  <div
    :class="[
      'relative size-full overflow-hidden bg-surface group/carousel touch-pan-y',
      zoomable && 'cursor-zoom-in',
    ]"
    role="region"
    @click="handleClick"
    @touchstart="handleTouchStart"
    @touchmove="handleTouchMove"
    @touchend="handleTouchEnd"
  >
    <transition
      :name="direction === 'forward' ? 'carousel-fwd' : 'carousel-bwd'"
    >
      <img
        :key="index"
        :src="photos[index]"
        :alt="`${alt} — photo ${index + 1} of ${photos.length}`"
        class="absolute inset-0 size-full object-cover select-none pointer-events-none"
        loading="lazy"
      />
    </transition>

    <div
      class="absolute inset-0 pointer-events-none bg-linear-to-t from-ink/30 via-ink/0 to-ink/0"
      aria-hidden="true"
    />

    <button
      v-if="photos.length > 1"
      type="button"
      class="focus-ring absolute left-3 top-1/2 -translate-y-1/2 z-10 size-9 grid place-items-center rounded-full bg-bg/85 backdrop-blur text-ink opacity-0 group-hover/carousel:opacity-100 focus-visible:opacity-100 transition-opacity duration-200 hover:bg-bg md:grid"
      @click.stop="prev($event)"
    >
      <span class="size-4 inline-block"><IconChevron dir="left" /></span>
    </button>
    <button
      v-if="photos.length > 1"
      type="button"
      class="focus-ring absolute right-3 top-1/2 -translate-y-1/2 z-10 size-9 grid place-items-center rounded-full bg-bg/85 backdrop-blur text-ink opacity-0 group-hover/carousel:opacity-100 focus-visible:opacity-100 transition-opacity duration-200 hover:bg-bg md:grid"
      @click.stop="next($event)"
    >
      <span class="size-4 inline-block"><IconChevron dir="right" /></span>
    </button>

    <div
      v-if="photos.length > 1"
      class="absolute bottom-3 right-3 z-10 micro-label text-cream/85! tabular bg-ink/40 backdrop-blur px-2 h-6 inline-flex items-center rounded-sm"
    >
      {{ index + 1 }}<span class="mx-0.5">/</span>{{ photos.length }}
    </div>

    <div
      v-if="zoomable"
      class="absolute bottom-3 left-3 z-10 size-7 grid place-items-center rounded-md bg-ink/40 backdrop-blur opacity-0 group-hover/carousel:opacity-100 transition-opacity text-cream"
      aria-hidden="true"
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="13"
        height="13"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M3 7V3h4M17 3h4v4M21 17v4h-4M7 21H3v-4" />
      </svg>
    </div>
  </div>

  <PhotoLightBox
    v-if="zoomable"
    v-model:open="lightboxOpen"
    :photos="photos"
    :alt="alt"
    :initial-index="index"
  />
</template>
