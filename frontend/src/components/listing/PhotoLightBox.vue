<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue';
import IconChevron from '../icons/IconChevron.vue';
import IconClose from '../icons/IconClose.vue';
import { usePinchZoom } from '../../composables/usePinchZoom';

const props = defineProps<{
  open: boolean;
  photos: string[];
  alt: string;
  initialIndex?: number;
}>();

const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const index = ref(props.initialIndex ?? 0);
const direction = ref<'forward' | 'backward'>('forward');

const {
  scale,
  panX,
  panY,
  pinching,
  reset: resetZoom,
  onTouchStart,
  onTouchMove,
  onTouchEnd,
} = usePinchZoom({
  onSwipe(dir) {
    dir === 'left' ? next() : prev();
  },
});

watch(
  () => props.open,
  (val) => {
    if (val) {
      index.value = props.initialIndex ?? 0;
      resetZoom();
      document.addEventListener('keydown', onKey);
      document.body.style.overflow = 'hidden';
    } else {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    }
  }
);

watch(
  () => props.initialIndex,
  (val) => {
    if (props.open) index.value = val ?? 0;
  }
);

function close() {
  emit('update:open', false);
}

function goTo(i: number) {
  direction.value = i > index.value ? 'forward' : 'backward';
  index.value = i;
  resetZoom();
}

function next() {
  goTo((index.value + 1) % props.photos.length);
}

function prev() {
  goTo((index.value - 1 + props.photos.length) % props.photos.length);
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    if (scale.value > 1) resetZoom();
    else close();
  }
  if (e.key === 'ArrowRight' && scale.value <= 1) next();
  if (e.key === 'ArrowLeft' && scale.value <= 1) prev();
}

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey);
  document.body.style.overflow = '';
});
</script>

<template>
  <Teleport to="body">
    <Transition name="scrim">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex flex-col bg-ink/96 touch-none"
      >
        <!-- Header -->
        <div class="flex items-center justify-between px-4 py-3 shrink-0 z-10">
          <span class="micro-label text-cream/50 tabular">
            {{ index + 1 }} / {{ photos.length }}
          </span>
          <div class="flex items-center gap-2">
            <button
              v-if="scale > 1"
              class="focus-ring h-7 px-3 rounded-full bg-cream/10 hover:bg-cream/20 text-cream text-xs transition-colors"
              @click="resetZoom"
            >
              Reset zoom
            </button>
            <button
              class="focus-ring size-9 grid place-items-center rounded-full bg-cream/10 hover:bg-cream/20 text-cream transition-colors"
              aria-label="Close"
              @click="close"
            >
              <span class="size-5"><IconClose /></span>
            </button>
          </div>
        </div>

        <!-- Image area -->
        <div
          class="relative flex-1 min-h-0 overflow-hidden"
          :class="
            scale > 1 ? 'cursor-grab active:cursor-grabbing' : 'cursor-default'
          "
          @click.self="scale <= 1 && close()"
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
        >
          <Transition
            :name="
              scale <= 1
                ? direction === 'forward'
                  ? 'carousel-fwd'
                  : 'carousel-bwd'
                : ''
            "
          >
            <img
              :key="index"
              :src="photos[index]"
              :alt="`${alt} — photo ${index + 1} of ${photos.length}`"
              class="absolute inset-0 w-full h-full object-contain select-none pointer-events-none"
              :style="{
                transform: `scale(${scale}) translate(${panX / scale}px, ${panY / scale}px)`,
                transition: pinching ? 'none' : 'transform 0.1s ease',
              }"
              draggable="false"
            />
          </Transition>

          <!-- Desktop-only nav arrows -->
          <button
            v-if="photos.length > 1"
            class="hidden sm:grid focus-ring absolute left-4 top-1/2 -translate-y-1/2 size-10 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors z-10"
            aria-label="Previous"
            @click="prev"
          >
            <span class="size-5 inline-block"><IconChevron dir="left" /></span>
          </button>
          <button
            v-if="photos.length > 1"
            class="hidden sm:grid focus-ring absolute right-4 top-1/2 -translate-y-1/2 size-10 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors z-10"
            aria-label="Next"
            @click="next"
          >
            <span class="size-5 inline-block"><IconChevron dir="right" /></span>
          </button>
        </div>

        <!-- Thumbnail strip -->
        <div
          v-if="photos.length > 1"
          class="shrink-0 flex gap-1.5 px-4 py-3 overflow-x-auto scroll-snap-x"
        >
          <button
            v-for="(src, i) in photos"
            :key="i"
            class="shrink-0 size-14 rounded-md overflow-hidden transition-all duration-150"
            :class="
              i === index
                ? 'ring-2 ring-cream ring-offset-1 ring-offset-ink opacity-100'
                : 'opacity-35 hover:opacity-65'
            "
            @click="goTo(i)"
          >
            <img
              :src="src"
              :alt="`Thumbnail ${i + 1}`"
              class="size-full object-cover"
            />
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
