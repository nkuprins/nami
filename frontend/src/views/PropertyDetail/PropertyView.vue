<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
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
import ContactCard from './components/ContactCard.vue';
import IconPlayer from '../../components/icons/IconPlayer.vue';
import IconChevron from '../../components/icons/IconChevron.vue';
import IconPhone from '../../components/icons/IconPhone.vue';
import PhotoLightBox from '../../components/listing/PhotoLightBox.vue';
import SpecDots from '../../components/listing/SpecDots.vue';

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
const phonePopoverOpen = ref(false);
const phonePopoverEl = ref<HTMLElement | null>(null);

function onClickOutsidePhone(e: MouseEvent) {
  if (
    phonePopoverEl.value &&
    !phonePopoverEl.value.contains(e.target as Node)
  ) {
    phonePopoverOpen.value = false;
  }
}

watch(phonePopoverOpen, (open) => {
  if (open) {
    document.addEventListener('mousedown', onClickOutsidePhone, true);
  } else {
    document.removeEventListener('mousedown', onClickOutsidePhone, true);
  }
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onClickOutsidePhone, true);
});
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

const bentoLightboxOpen = ref(false);
const bentoLightboxIndex = ref(0);

function openBento(i: number) {
  bentoLightboxIndex.value = i;
  bentoLightboxOpen.value = true;
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-6 lg:px-6">
    <button
      v-if="!property"
      class="micro-label text-ink-3 bg-transparent border-none p-0 cursor-pointer mb-6"
      @click="router.back()"
    >
      &larr; Back
    </button>
    <p v-if="!property" class="text-sm text-ink-2">Loading&hellip;</p>

    <template v-if="property">
      <button
        class="inline-flex items-center gap-1.5 micro-label text-ink-3 hover:text-ink-2 transition-colors mb-6 bg-transparent border-none p-0 cursor-pointer"
        @click="router.back()"
      >
        <i class="ti ti-arrow-left" aria-hidden="true" />
        Back
      </button>

      <!-- Desktop bento photo grid (lg+, 3+ photos) -->
      <div
        v-if="property.photos.length >= 3"
        class="hidden lg:grid grid-cols-[2fr_1fr] grid-rows-2 gap-1.5 rounded-xl overflow-hidden h-[420px] mb-8"
      >
        <div
          class="relative row-span-2 cursor-zoom-in overflow-hidden group"
          @click="openBento(0)"
        >
          <img
            :src="property.photos[0]"
            :alt="`${property.title} — photo 1`"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
          />
          <div class="absolute top-3 right-3 z-10">
            <SaveHeart :property-id="property.id" />
          </div>
        </div>
        <div class="cursor-zoom-in overflow-hidden group" @click="openBento(1)">
          <img
            :src="property.photos[1]"
            :alt="`${property.title} — photo 2`"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
          />
        </div>
        <div
          class="relative cursor-zoom-in overflow-hidden group"
          @click="openBento(2)"
        >
          <img
            :src="property.photos[2]"
            :alt="`${property.title} — photo 3`"
            class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
          />
          <button
            v-if="property.photos.length > 3"
            type="button"
            class="absolute bottom-3 right-3 z-10 text-xs font-medium text-ink bg-bg/90 backdrop-blur px-3 py-1.5 rounded-lg border border-line cursor-pointer hover:bg-bg transition-colors"
            @click.stop="openBento(0)"
          >
            View all {{ property.photos.length }} photos
          </button>
        </div>
      </div>

      <!-- Desktop fallback carousel (lg+, < 3 photos) -->
      <div
        v-else
        class="hidden lg:block relative aspect-[2.2/1] rounded-xl overflow-hidden mb-8"
      >
        <CardCarousel
          :photos="property.photos"
          :alt="property.title"
          zoomable
        />
        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id" />
        </div>
      </div>

      <!-- Mobile carousel -->
      <div
        class="lg:hidden relative aspect-4/3 rounded-xl overflow-hidden mb-6"
      >
        <CardCarousel
          :photos="property.photos"
          :alt="property.title"
          zoomable
        />
        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id" />
        </div>
      </div>

      <PhotoLightBox
        v-model:open="bentoLightboxOpen"
        :photos="property.photos"
        :alt="property.title"
        :initial-index="bentoLightboxIndex"
      />

      <!-- Two-column layout on desktop -->
      <div class="lg:grid lg:grid-cols-[1fr_320px] lg:gap-10">
        <!-- Main content -->
        <div>
          <div class="flex items-start justify-between gap-4 mb-1">
            <div class="min-w-0">
              <p class="micro-label">
                {{ property.district }} · {{ property.city }}
              </p>
              <h1 class="mt-1 text-xl leading-snug text-ink font-medium">
                {{ property.title }}
              </h1>
              <p class="mt-1 text-sm text-ink-2">
                {{ property.address }}
              </p>
            </div>
            <!-- Price inline on mobile, hidden on desktop (shown in sidebar) -->
            <div class="text-right shrink-0 lg:hidden">
              <p class="display-price text-2xl text-ink whitespace-nowrap">
                {{ price }}
              </p>
              <p class="text-xs text-ink-2 tabular">
                {{ pricePerM2 }}
              </p>
            </div>
          </div>

          <SpecDots :parts="specRow" class="text-sm text-ink-2 mt-3" />

          <div
            v-if="property.features.length"
            class="flex flex-wrap gap-2 mt-4"
          >
            <span
              v-for="f in property.features"
              :key="f"
              class="micro-label bg-surface border border-line rounded-md px-2 py-1"
            >
              {{ FEATURE_LABELS[f] }}
            </span>
          </div>

          <hr class="border-none border-t border-line my-5" />

          <p class="text-sm text-ink-2 leading-relaxed">
            {{ property.description }}
          </p>

          <hr class="border-none border-t border-line my-5" />

          <PhotoGrid :photos="property.photos" :alt="property.title" />

          <div v-if="videoTourUrl">
            <div
              v-if="videoHasError"
              class="relative aspect-video overflow-hidden rounded-xl border border-dashed border-line bg-surface flex flex-col items-center justify-center text-center p-6"
            >
              <i
                class="ti ti-video-off text-2xl text-ink-3 mb-2"
                aria-hidden="true"
              />
              <p class="text-sm font-medium text-ink">Video tour unavailable</p>
              <p class="text-xs text-ink-2 mt-1 max-w-xs">
                This video cannot be loaded or has been removed by the provider.
              </p>
            </div>

            <div
              v-else
              class="relative aspect-video overflow-hidden rounded-xl border border-line bg-black shadow-sm"
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
                    :alt="`${property.title} video tour thumbnail`"
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

                  <div
                    class="absolute inset-0 flex items-center justify-center"
                  >
                    <span
                      class="flex size-14 items-center justify-center rounded-full bg-white/90 text-ink shadow-lg transition-transform group-hover:scale-105"
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

          <!-- Contact section: mobile only -->
          <div class="lg:hidden">
            <hr class="border-none border-t border-line my-5" />
            <div>
              <p class="micro-label mb-4">Contact details</p>
              <ContactCard
                :phones="property.phones"
                :phone-revealed="phoneRevealed"
                @reveal-phone="phoneRevealed = true"
              />
            </div>
          </div>
        </div>

        <!-- Sidebar: desktop only -->
        <aside class="hidden lg:block">
          <div class="sticky top-20 space-y-4">
            <div class="rounded-xl border border-line p-5 shadow-soft">
              <p class="display-price text-2xl text-ink">
                {{ price }}
              </p>
              <p class="text-xs text-ink-2 tabular mt-1">
                {{ pricePerM2 }}
              </p>

              <SpecDots
                :parts="specRow"
                class="text-xs text-ink-2 mt-3 pb-5 border-b border-line"
              />

              <ContactCard
                class="mt-5"
                :phones="property.phones"
                :phone-revealed="phoneRevealed"
                @reveal-phone="phoneRevealed = true"
              />
            </div>
          </div>
        </aside>
      </div>

      <!-- Mobile sticky bottom bar -->
      <div
        class="lg:hidden fixed bottom-0 left-0 right-0 z-30 bg-bg/95 backdrop-blur border-t border-line px-4 py-3"
      >
        <div class="flex items-center justify-between gap-4 relative">
          <div>
            <p class="display-price text-lg text-ink">{{ price }}</p>
            <p class="text-xs text-ink-2 tabular">{{ pricePerM2 }}</p>
          </div>

          <div
            v-if="phoneRevealed && property.phones?.length"
            ref="phonePopoverEl"
            class="relative"
          >
            <div class="flex items-stretch bg-ink rounded-lg overflow-hidden">
              <a
                :href="`tel:${property.phones[0].replace(/\s/g, '')}`"
                class="flex items-center gap-1.5 px-5 py-2.5 text-cream text-sm font-medium hover:opacity-90 transition-opacity"
              >
                <span class="size-4 shrink-0"><IconPhone /></span>
                {{ property.phones[0] }}
              </a>
              <button
                v-if="property.phones.length > 1"
                class="flex items-center px-2.5 border-l border-cream/20 text-cream cursor-pointer hover:bg-white/10 transition-colors"
                @click="phonePopoverOpen = !phonePopoverOpen"
                aria-label="More phone numbers"
              >
                <span class="size-4"
                  ><IconChevron :dir="phonePopoverOpen ? 'down' : 'up'"
                /></span>
              </button>
            </div>

            <Transition name="fade">
              <div
                v-if="phonePopoverOpen"
                class="absolute bottom-full right-0 mb-2 w-56 bg-bg border border-line rounded-xl shadow-lift p-2"
              >
                <a
                  v-for="(phone, i) in property.phones"
                  :key="i"
                  :href="`tel:${phone.replace(/\s/g, '')}`"
                  class="flex items-center gap-2 px-3 py-2.5 text-sm text-ink font-medium rounded-lg hover:bg-surface transition-colors"
                >
                  <span class="size-4 shrink-0"><IconPhone /></span>
                  {{ phone }}
                </a>
              </div>
            </Transition>
          </div>

          <!-- Not yet revealed -->
          <button
            v-else-if="property.phones?.length"
            class="flex items-center gap-1.5 px-5 py-2.5 bg-ink text-cream text-sm font-medium rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
            @click="phoneRevealed = true"
          >
            <span class="size-4 shrink-0"><IconPhone /></span>
            Show number
          </button>
        </div>
      </div>
      <div class="lg:hidden h-20" />
    </template>
  </div>
</template>
