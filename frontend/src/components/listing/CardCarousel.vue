<script setup lang="ts">
import {ref, watch} from 'vue';
import IconChevron from "../ui/IconChevron.vue";

const props = defineProps<{
  photos: string[];
  alt: string;
}>();

const index = ref(0);
const direction = ref<'forward' | 'backward'>('forward');

// Touch tracking state
let touchStartX = 0;
let touchEndX = 0;
const SWIPE_THRESHOLD = 50; // Minimum swipe distance in pixels

watch(() => props.photos, () => {
  index.value = 0;
});

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

function goto(i: number, e?: Event) {
  e?.preventDefault();
  e?.stopPropagation();
  direction.value = i > index.value ? 'forward' : 'backward';
  index.value = i;
}

// Touch Event Handlers
function handleTouchStart(e: TouchEvent) {
  touchStartX = e.touches[0].clientX;
}

function handleTouchMove(e: TouchEvent) {
  touchEndX = e.touches[0].clientX;
}

function handleTouchEnd() {
  const swipeDistance = touchStartX - touchEndX;

  // Ignore tiny movements or cases where touchMove didn't fire
  if (!touchEndX || Math.abs(swipeDistance) < SWIPE_THRESHOLD) return;

  if (swipeDistance > 0) {
    next(); // Swiped left -> Show next photo
  } else {
    prev(); // Swiped right -> Show previous photo
  }

  // Reset values
  touchStartX = 0;
  touchEndX = 0;
}
</script>

<template>
  <div
      class="relative size-full overflow-hidden bg-surface group/carousel touch-pan-y"
      role="region"
      aria-label="Listing photos"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="handleTouchEnd"
  >
    <transition
        :name="direction === 'forward' ? 'carousel-fwd' : 'carousel-bwd'"
        mode="out-in"
    >
      <img
          :key="index"
          :src="photos[index]"
          :alt="`${alt} — photo ${index + 1} of ${photos.length}`"
          class="absolute inset-0 size-full object-cover select-none pointer-events-none"
          loading="lazy"
          disabled-drag
      />
    </transition>

    <div
        class="absolute inset-0 pointer-events-none
             bg-linear-to-t from-ink/30 via-ink/0 to-ink/0"
        aria-hidden="true"
    />

    <button
        v-if="photos.length > 1"
        type="button"
        @click="prev"
        aria-label="Previous photo"
        class="focus-ring absolute left-3 top-1/2 -translate-y-1/2 z-10
             size-9 grid place-items-center rounded-full
             bg-bg/85 backdrop-blur text-ink
             opacity-0 group-hover/carousel:opacity-100
             focus-visible:opacity-100
             transition-opacity duration-200 hover:bg-bg hidden md:grid"
    >
      <span class="size-4 inline-block"><IconChevron dir="left"/></span>
    </button>
    <button
        v-if="photos.length > 1"
        type="button"
        @click="next"
        aria-label="Next photo"
        class="focus-ring absolute right-3 top-1/2 -translate-y-1/2 z-10
             size-9 grid place-items-center rounded-full
             bg-bg/85 backdrop-blur text-ink
             opacity-0 group-hover/carousel:opacity-100
             focus-visible:opacity-100
             transition-opacity duration-200 hover:bg-bg hidden md:grid"
    >
      <span class="size-4 inline-block"><IconChevron dir="right"/></span>
    </button>

    <div
        v-if="photos.length > 1"
        class="absolute bottom-3 left-1/2 -translate-x-1/2 z-10
             flex items-center gap-1.5"
    >
      <button
          v-for="(_, i) in photos"
          :key="i"
          type="button"
          @click="(e) => goto(i, e)"
          :aria-label="`Go to photo ${i + 1}`"
          :aria-current="i === index ? 'true' : undefined"
          class="focus-ring h-1 rounded-full transition-all duration-200"
          :class="i === index
          ? 'w-5 bg-bg'
          : 'w-1 bg-bg/55 hover:bg-bg/80'"
      />
    </div>

    <div
        class="absolute bottom-3 right-3 z-10 micro-label text-cream/85! tabular
             bg-ink/40 backdrop-blur px-2 h-6 inline-flex items-center rounded-sm"
        v-if="photos.length > 1"
    >
      <span>{{ index + 1 }}</span>
      <span class="mx-0.5">/</span>
      <span>{{ photos.length }}</span>
    </div>
  </div>
</template>