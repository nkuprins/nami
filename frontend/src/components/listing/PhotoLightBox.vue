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
  didDrag,
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

function handleImageClick(e: MouseEvent) {
  if (didDrag()) return;

  const img = e.target as HTMLImageElement;
  const rect = img.getBoundingClientRect();

  if (scale.value <= 1 && img.naturalWidth && img.naturalHeight) {
    const imgAspect = img.naturalWidth / img.naturalHeight;
    const elemAspect = rect.width / rect.height;
    let onLetterbox = false;

    if (imgAspect > elemAspect) {
      const displayedHeight = rect.width / imgAspect;
      const bar = (rect.height - displayedHeight) / 2;
      const relY = e.clientY - rect.top;
      onLetterbox = relY < bar || relY > rect.height - bar;
    } else {
      const displayedWidth = rect.height * imgAspect;
      const bar = (rect.width - displayedWidth) / 2;
      const relX = e.clientX - rect.left;
      onLetterbox = relX < bar || relX > rect.width - bar;
    }

    if (onLetterbox) {
      close();
      return;
    }
  }

  if (scale.value > 1) {
    resetZoom();
  } else {
    const cx = e.clientX - rect.left - rect.width / 2;
    const cy = e.clientY - rect.top - rect.height / 2;
    scale.value = 2;
    panX.value = -cx;
    panY.value = -cy;
  }
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
        <div
          class="flex items-center justify-between px-4 sm:px-6 py-3 sm:py-4 shrink-0 z-10"
        >
          <span
            class="micro-label text-cream/85! tabular bg-cream/10 backdrop-blur px-2 sm:px-3 h-6 sm:h-8 sm:text-sm inline-flex items-center rounded-sm"
          >
            {{ index + 1 }}<span class="mx-0.5">/</span>{{ photos.length }}
          </span>
          <div class="flex items-center gap-2 sm:gap-3">
            <button
              v-if="scale > 1"
              class="focus-ring h-7 sm:h-9 px-3 sm:px-4 rounded-full bg-cream/10 hover:bg-cream/20 text-cream text-xs sm:text-sm transition-colors"
              @click="resetZoom"
            >
              Reset zoom
            </button>
            <button
              class="focus-ring size-9 sm:size-11 grid place-items-center rounded-full bg-cream/10 hover:bg-cream/20 text-cream transition-colors"
              aria-label="Close"
              @click="close"
            >
              <span class="size-5 sm:size-6"><IconClose /></span>
            </button>
          </div>
        </div>

        <!-- Image area -->
        <div
          class="relative flex-1 min-h-0 overflow-hidden"
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
              class="absolute inset-0 w-full h-full object-contain select-none"
              :class="scale > 1 ? 'cursor-zoom-out' : 'cursor-zoom-in'"
              :style="{
                transform: `scale(${scale}) translate(${panX / scale}px, ${panY / scale}px)`,
                transition: pinching ? 'none' : 'transform 0.2s ease',
              }"
              draggable="false"
              @click="handleImageClick"
            />
          </Transition>

          <!-- Desktop-only nav arrows -->
          <div
            v-if="photos.length > 1 && scale <= 1"
            class="absolute inset-0 max-w-6xl mx-auto pointer-events-none z-10"
          >
            <button
              class="hidden sm:grid pointer-events-auto focus-ring absolute left-2 top-1/2 -translate-y-1/2 size-10 md:size-12 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors"
              aria-label="Previous"
              @click="prev"
            >
              <span class="size-5 md:size-6 inline-block"
                ><IconChevron dir="left"
              /></span>
            </button>
            <button
              class="hidden sm:grid pointer-events-auto focus-ring absolute right-2 top-1/2 -translate-y-1/2 size-10 md:size-12 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors"
              aria-label="Next"
              @click="next"
            >
              <span class="size-5 md:size-6 inline-block"
                ><IconChevron dir="right"
              /></span>
            </button>
          </div>
        </div>

        <!-- Thumbnail strip -->
        <div
          v-if="photos.length > 1"
          class="shrink-0 flex justify-center gap-1.5 sm:gap-2 px-4 py-3 sm:py-4 overflow-x-auto scroll-snap-x"
        >
          <button
            v-for="(src, i) in photos"
            :key="i"
            class="shrink-0 size-14 sm:size-18 rounded-md overflow-hidden transition-all duration-150"
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
