<script setup lang="ts">
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue';
import { useI18n } from 'vue-i18n';
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

const { t } = useI18n();

const index = ref(props.initialIndex ?? 0);
const direction = ref<'forward' | 'backward'>('forward');
const thumbnailStrip = ref<HTMLElement>();
const imageAreaEl = ref<HTMLElement>();
const barWidth = ref(0); // px of black pillarbox bar on each side (0 when image fills full width)

// object-contain centers the image in the container, leaving black bars on the sides
// when the image is narrower than the container (portrait photo on wide screen).
// barWidth drives both the nav arrow positions and the thumbnail strip insets.
function computeImageLayout() {
  const el = imageAreaEl.value;
  const img = el?.querySelectorAll<HTMLImageElement>('img')[0];
  if (!el || !img?.naturalWidth) return;
  const iAspect = img.naturalWidth / img.naturalHeight;
  const cAspect = el.clientWidth / el.clientHeight;
  barWidth.value =
    iAspect < cAspect
      ? Math.max(
          0,
          Math.round((el.clientWidth - el.clientHeight * iAspect) / 2)
        )
      : 0;
}

// Arrow sits just outside the image edge: bar minus button(48) minus gap(8)
const arrowInset = computed(() => Math.max(8, barWidth.value - 56));

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

function scrollActiveThumbnail() {
  nextTick(() => {
    const thumb = thumbnailStrip.value?.children[index.value] as HTMLElement;
    thumb?.scrollIntoView({
      behavior: 'smooth',
      inline: 'center',
      block: 'nearest',
    });
  });
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      index.value = props.initialIndex ?? 0;
      resetZoom();
      document.addEventListener('keydown', onKey);
      window.addEventListener('resize', computeImageLayout);
      document.body.style.overflow = 'hidden';
      scrollActiveThumbnail();
    } else {
      document.removeEventListener('keydown', onKey);
      window.removeEventListener('resize', computeImageLayout);
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

watch(index, () => {
  nextTick(computeImageLayout);
  scrollActiveThumbnail();
});

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
  window.removeEventListener('resize', computeImageLayout);
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
              {{ t('common.resetZoom') }}
            </button>
            <button
              class="focus-ring size-9 sm:size-11 grid place-items-center rounded-full bg-cream/10 hover:bg-cream/20 text-cream transition-colors"
              :aria-label="t('common.close')"
              @click="close"
            >
              <span class="size-5 sm:size-6"><IconClose /></span>
            </button>
          </div>
        </div>

        <!-- Image area -->
        <div
          ref="imageAreaEl"
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
              :alt="`${alt} — ${index + 1} of ${photos.length}`"
              class="absolute inset-0 w-full h-full object-contain select-none"
              :class="scale > 1 ? 'cursor-zoom-out' : 'cursor-zoom-in'"
              :style="{
                transform: `scale(${scale}) translate(${panX / scale}px, ${panY / scale}px)`,
                transition: pinching ? 'none' : 'transform 0.2s ease',
              }"
              draggable="false"
              @click="handleImageClick"
              @load="computeImageLayout"
            />
          </Transition>

          <!-- Desktop nav arrows -->
          <template v-if="photos.length > 1 && scale <= 1">
            <button
              class="hidden sm:grid focus-ring size-10 md:size-12 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors absolute top-1/2 -translate-y-1/2 z-10"
              :style="{ left: `${arrowInset}px` }"
              :aria-label="t('common.previous')"
              @click="prev"
            >
              <span class="size-5 md:size-6 inline-block"
                ><IconChevron dir="left"
              /></span>
            </button>
            <button
              class="hidden sm:grid focus-ring size-10 md:size-12 place-items-center rounded-full bg-ink/50 hover:bg-ink/80 text-cream transition-colors absolute top-1/2 -translate-y-1/2 z-10"
              :style="{ right: `${arrowInset}px` }"
              :aria-label="t('common.next')"
              @click="next"
            >
              <span class="size-5 md:size-6 inline-block"
                ><IconChevron dir="right"
              /></span>
            </button>
          </template>
        </div>

        <!-- Spacer when single photo: matches thumbnail strip height -->
        <div v-if="photos.length === 1" class="shrink-0 py-3 sm:py-4">
          <div class="h-20 sm:h-[72px] p-1" />
        </div>

        <!-- Thumbnail strip: bounded to image width, scrolls when thumbnails overflow -->
        <div
          v-else
          class="shrink-0 py-3 sm:py-4"
          :style="{
            paddingLeft: `${barWidth}px`,
            paddingRight: `${barWidth}px`,
          }"
        >
          <div
            ref="thumbnailStrip"
            class="flex gap-1.5 sm:gap-2 overflow-x-auto p-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          >
            <button
              v-for="(src, i) in photos"
              :key="i"
              class="shrink-0 size-20 sm:size-18 rounded-md overflow-hidden transition-all duration-150"
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
      </div>
    </Transition>
  </Teleport>
</template>
