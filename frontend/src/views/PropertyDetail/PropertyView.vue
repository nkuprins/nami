<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { PropertyItem } from '../../types/propertyItem';
import { FEATURE_LABELS } from '../../types/propertyLabels';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';
import {
  normalizeVideoEmbedUrl,
  getVideoThumbnailUrl,
} from '../../utils/video';
import SaveHeart from '../../components/listing/SaveHeart.vue';
import CardCarousel from '../../components/listing/CardCarousel.vue';
import { getProperty } from '../../api/propertiesApi';
import LocationMap from '../../components/listing/LocationMap.vue';
import PhotoGrid from './components/PhotoGrid.vue';
import IconPlayer from '../../components/icons/IconPlayer.vue';

const props = defineProps<{ id: string }>();
const router = useRouter();

const property = ref<PropertyItem | null>(null);
onMounted(async () => {
  property.value = (await getProperty(props.id)) ?? null;
});

const price = computed(() =>
  property.value ? formatPrice(property.value.price, property.value.type) : ''
);
const pricePerM2 = computed(() =>
  property.value
    ? formatPricePerM2(property.value.price / property.value.m2)
    : ''
);

const videoHasError = ref(false);

function handleVideoError() {
  videoHasError.value = true;
}

const specRow = computed(() => {
  if (!property.value) return [];
  const { rooms, m2, floor, totalFloors, landM2, propertyKind } =
    property.value;
  const parts: string[] = [];
  parts.push(`${rooms} rm`);
  parts.push(`${m2} m²`);
  if (propertyKind === 'house' && landM2) {
    parts.push(`${landM2.toLocaleString()} m² land`);
  } else if (floor) {
    parts.push(formatFloor(floor, totalFloors));
  }
  if (property.value.yearBuilt) parts.push(`Built ${property.value.yearBuilt}`);
  return parts;
});

const phoneRevealed = ref(false);
const videoExpanded = ref(false);

const videoTourUrl = computed(() => {
  const raw = property.value?.videoUrl;
  return typeof raw === 'string' ? raw.trim() : '';
});

const videoEmbedUrl = computed(() =>
  videoTourUrl.value ? normalizeVideoEmbedUrl(videoTourUrl.value) : ''
);

const videoThumbnailUrl = computed(() =>
  videoTourUrl.value ? getVideoThumbnailUrl(videoTourUrl.value) : ''
);
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-6">
    <button
      v-if="!property"
      class="micro-label text-[--color-ink-3] bg-transparent border-none p-0 cursor-pointer mb-6"
      @click="router.back()"
    >
      ← Back
    </button>
    <p v-if="!property" class="text-sm text-[--color-ink-2]">Loading…</p>

    <template v-if="property">
      <button
        class="inline-flex items-center gap-1.5 micro-label text-[--color-ink-3] hover:text-[--color-ink-2] transition-colors mb-6 bg-transparent border-none p-0 cursor-pointer"
        @click="router.back()"
      >
        <i class="ti ti-arrow-left" aria-hidden="true" />
        Back
      </button>

      <div class="relative aspect-4/3 rounded-xl overflow-hidden mb-6">
        <CardCarousel :photos="property.photos" :alt="property.title" />
        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id" />
        </div>
      </div>

      <div class="flex items-start justify-between gap-4 mb-1">
        <div class="min-w-0">
          <p class="micro-label">
            {{ property.district }} · {{ property.city }}
          </p>
          <h1 class="mt-1 text-xl leading-snug text-[--color-ink] font-medium">
            {{ property.title }}
          </h1>
          <p class="mt-1 text-sm text-[--color-ink-2]">
            {{ property.address }}
          </p>
        </div>
        <div class="text-right shrink-0">
          <p
            class="display-price text-2xl text-[--color-ink] whitespace-nowrap"
          >
            {{ price }}
          </p>
          <p class="text-xs text-[--color-ink-2] tabular">
            {{ pricePerM2 }}
          </p>
        </div>
      </div>

      <div
        class="flex items-center flex-wrap gap-x-3 gap-y-1 text-sm text-[--color-ink-2] tabular mt-3"
      >
        <span
          v-for="(part, i) in specRow"
          :key="i"
          class="inline-flex items-center gap-3"
        >
          {{ part }}
          <span
            v-if="i < specRow.length - 1"
            class="text-[--color-ink-3]"
            aria-hidden="true"
            >·</span
          >
        </span>
      </div>

      <div v-if="property.features.length" class="flex flex-wrap gap-2 mt-4">
        <span
          v-for="f in property.features"
          :key="f"
          class="micro-label bg-[--color-surface] border border-[--color-line] rounded-md px-2 py-1"
        >
          {{ FEATURE_LABELS[f] }}
        </span>
      </div>

      <hr class="border-none border-t border-[--color-line] my-5" />

      <p class="text-sm text-[--color-ink-2] leading-relaxed">
        {{ property.description }}
      </p>
      <hr class="border-none border-t border-[--color-line] my-5" />

      <PhotoGrid :photos="property.photos" :alt="property.title" />

      <div v-if="videoTourUrl">
        <div
          v-if="videoHasError"
          class="relative aspect-video overflow-hidden rounded-xl border border-dashed border-[--color-line] bg-[--color-surface] flex flex-col items-center justify-center text-center p-6"
        >
          <i
            class="ti ti-video-off text-2xl text-[--color-ink-3] mb-2"
            aria-hidden="true"
          />
          <p class="text-sm font-medium text-[--color-ink]">
            Video tour unavailable
          </p>
          <p class="text-xs text-[--color-ink-2] mt-1 max-w-xs">
            This video cannot be loaded or has been removed by the provider.
          </p>
        </div>

        <div
          v-else
          class="relative aspect-video overflow-hidden rounded-xl border border-[--color-line] bg-black shadow-sm"
        >
          <Transition name="fade" mode="out-in">
            <button
              v-if="!videoExpanded"
              type="button"
              class="group absolute inset-0 h-full w-full bg-[--color-surface] text-left focus-ring border-none p-0 cursor-pointer"
              :aria-expanded="videoExpanded"
              @click="videoExpanded = true"
            >
              <img
                v-if="videoThumbnailUrl"
                :src="videoThumbnailUrl"
                :alt="`${property.title} video tour thumbnail`"
                class="absolute inset-0 h-full w-full object-cover"
                @error="handleVideoError"
              />
              <div
                v-else
                class="absolute inset-0 bg-linear-to-br from-[--color-surface] via-bg to-[--color-cream]"
              />
              <div
                class="absolute inset-0 bg-black/20 transition-opacity group-hover:bg-black/25"
              />

              <div class="absolute inset-0 flex items-center justify-center">
                <span
                  class="flex size-14 items-center justify-center rounded-full bg-white/90 text-[--color-ink] shadow-lg transition-transform group-hover:scale-105"
                >
                  <IconPlayer />
                </span>
              </div>
            </button>

            <iframe
              v-else
              :src="`${videoEmbedUrl}&autoplay=1`"
              class="h-full w-full border-none"
              title="Video tour"
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
      </div>

      <div v-if="property.coords" class="my-5">
        <p class="micro-label mb-3">Location</p>
        <LocationMap
          :model-value="property.coords"
          :address="property.address"
          :district="property.district"
          :city="property.city"
          readonly
        />
      </div>

      <hr class="border-none border-t border-[--color-line] my-5" />

      <div>
        <p class="micro-label mb-4">Contact details</p>
        <div class="flex items-center gap-3 mb-4">
          <div
            class="w-10 h-10 rounded-full bg-[--color-surface] border border-[--color-line] flex items-center justify-center text-sm font-medium text-[--color-ink-2] shrink-0"
          >
            AV
          </div>
          <div>
            <p class="text-sm font-medium text-[--color-ink]">
              Andris Veinbergs
            </p>
          </div>
        </div>
        <div class="flex">
          <button
            class="flex-1 flex items-center justify-center gap-1.5 py-2.5 bg-transparent text-[--color-ink] text-sm font-medium rounded-lg border border-[--color-line] cursor-pointer hover:bg-[--color-surface] transition-colors"
            @click="phoneRevealed = true"
          >
            <i class="ti ti-phone text-sm" aria-hidden="true" />
            {{ phoneRevealed ? '+371 29 XXX XXX' : 'Show number' }}
          </button>
        </div>
      </div>
    </template>
  </div>
</template>
