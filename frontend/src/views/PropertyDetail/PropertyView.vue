<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { PropertyDetail } from '../../types/propertyItem';
import { FEATURE_LABELS } from '../../types/propertyLabels';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';
import SaveHeart from '../../components/listing/SaveHeart.vue';
import CardCarousel from '../../components/listing/CardCarousel.vue';
import { getProperty, deleteProperty } from '../../api/propertiesApi';
import LocationMap from '../../components/listing/LocationMap.vue';
import PhotoGrid from './components/PhotoGrid.vue';
import ContactCard from './components/ContactCard.vue';
import VideoPlayer from './components/VideoPlayer.vue';
import BentoPhotoGrid from './components/BentoPhotoGrid.vue';
import MobileStickyBar from './components/MobileStickyBar.vue';
import IconShare from '../../components/icons/IconShare.vue';
import IconArrowLeft from '../../components/icons/IconArrowLeft.vue';
import IconHeart from '../../components/icons/IconHeart.vue';
import PhotoLightBox from '../../components/listing/PhotoLightBox.vue';
import SpecDots from '../../components/listing/SpecDots.vue';
import { useShare } from '../../composables/useShare';
import { useSavedStore } from '../../stores/savedStore';
import { useAuthStore } from '../../stores/authStore';
import IconEdit from '../../components/icons/IconEdit.vue';
import IconTrash from '../../components/icons/IconTrash.vue';
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue';

const props = defineProps<{ id: string }>();
const router = useRouter();
const savedStore = useSavedStore();
const authStore = useAuthStore();
const saved = computed(() => savedStore.isSaved(props.id));
const isOwner = computed(
  () =>
    authStore.user?.id != null && authStore.user.id === property.value?.ownerId
);

const property = ref<PropertyDetail | null>(null);
onMounted(async () => {
  property.value = (await getProperty(props.id)) ?? null;
});

const confirmingDelete = ref(false);
const deleting = ref(false);

async function confirmDelete() {
  deleting.value = true;
  try {
    await deleteProperty(props.id);
    await router.replace('/');
  } catch {
    deleting.value = false;
  }
}

const price = computed(() =>
  property.value ? formatPrice(property.value.price, property.value.type) : ''
);
const pricePerM2 = computed(() =>
  property.value
    ? formatPricePerM2(property.value.price / property.value.m2)
    : ''
);

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

const mediaTab = ref<'photos' | 'video'>('photos');

function switchToVideo() {
  mediaTab.value = 'video';
}

const videoTourUrl = computed(() => {
  const raw = property.value?.videoUrl;
  return typeof raw === 'string' ? raw.trim() : '';
});

const { share, justCopied } = useShare();

function shareProperty() {
  if (!property.value) return;
  share({ title: property.value.title, url: window.location.href });
}

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
      class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-2 hover:text-ink transition-colors bg-transparent border-none p-0 cursor-pointer mb-6"
      @click="router.back()"
    >
      <span class="size-4 shrink-0"><IconArrowLeft /></span>
      Back
    </button>
    <p v-if="!property" class="text-sm text-ink-2">Loading&hellip;</p>

    <template v-if="property">
      <button
        class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-2 hover:text-ink transition-colors mb-6 bg-transparent border-none p-0 cursor-pointer"
        @click="router.back()"
      >
        <span class="size-4 shrink-0"><IconArrowLeft /></span>
        Back
      </button>

      <BentoPhotoGrid
        :photos="property.photos"
        :alt="property.title"
        @open-lightbox="openBento"
      />

      <!-- Mobile carousel -->
      <div
        class="lg:hidden relative aspect-4/3 rounded-lg overflow-hidden mb-6"
      >
        <CardCarousel
          :photos="property.photos"
          :alt="property.title"
          zoomable
        />
        <div class="absolute top-3 right-3 z-10 flex items-center gap-2">
          <RouterLink
            v-if="isOwner"
            :to="`/property/${property.id}/edit`"
            class="size-9 grid place-items-center rounded-full bg-bg/90 backdrop-blur text-ink-2 hover:bg-bg hover:scale-105 active:scale-95 transition-all duration-200"
          >
            <span class="size-4"><IconEdit /></span>
          </RouterLink>
          <button
            v-if="isOwner"
            type="button"
            class="size-9 grid place-items-center rounded-full bg-bg/90 backdrop-blur text-ink-2 cursor-pointer hover:bg-bg hover:scale-105 active:scale-95 transition-all duration-200"
            @click.stop="confirmingDelete = true"
          >
            <span class="size-4"><IconTrash /></span>
          </button>
          <button
            type="button"
            class="size-9 grid place-items-center rounded-full bg-bg/90 backdrop-blur text-ink-2 cursor-pointer hover:bg-bg hover:scale-105 active:scale-95 transition-all duration-200"
            @click.stop="shareProperty"
          >
            <span class="size-4"><IconShare /></span>
          </button>
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
          <div class="mb-1">
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

          <SpecDots
            :parts="specRow"
            class="lg:hidden text-sm text-ink-2 mt-3"
          />

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

          <!-- Media toggle + content -->
          <div>
            <div v-if="videoTourUrl" class="flex items-center gap-1.5 mb-4">
              <button
                type="button"
                class="focus-ring px-4 py-1.5 text-sm font-medium rounded-full border transition-colors cursor-pointer"
                :class="
                  mediaTab === 'photos'
                    ? 'bg-ink text-cream border-ink'
                    : 'bg-transparent text-ink-2 border-line hover:border-line-2 hover:text-ink'
                "
                @click="mediaTab = 'photos'"
              >
                Photos
              </button>
              <button
                type="button"
                class="focus-ring px-4 py-1.5 text-sm font-medium rounded-full border transition-colors cursor-pointer"
                :class="
                  mediaTab === 'video'
                    ? 'bg-ink text-cream border-ink'
                    : 'bg-transparent text-ink-2 border-line hover:border-line-2 hover:text-ink'
                "
                @click="switchToVideo()"
              >
                Video
              </button>
            </div>

            <PhotoGrid
              v-if="mediaTab === 'photos'"
              :photos="property.photos"
              :alt="property.title"
              :video-url="videoTourUrl"
              @play-video="switchToVideo()"
            />

            <VideoPlayer
              v-else-if="mediaTab === 'video' && videoTourUrl"
              :video-url="videoTourUrl"
              :alt="property.title"
            />
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

              <button
                type="button"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-4 text-sm font-medium bg-transparent border border-line rounded-lg cursor-pointer hover:bg-surface transition-colors"
                :class="saved ? 'text-accent-2' : 'text-ink-2 hover:text-ink'"
                @click="savedStore.toggle(property.id)"
              >
                <span class="size-4 shrink-0"
                  ><IconHeart :filled="saved"
                /></span>
                {{ saved ? 'Saved' : 'Save listing' }}
              </button>

              <button
                type="button"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg cursor-pointer hover:bg-surface hover:text-ink transition-colors"
                @click="shareProperty"
              >
                <span class="size-4 shrink-0"><IconShare /></span>
                {{ justCopied ? 'Link copied!' : 'Share listing' }}
              </button>

              <RouterLink
                v-if="isOwner"
                :to="`/property/${property.id}/edit`"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg hover:bg-surface hover:text-ink transition-colors"
              >
                <span class="size-4 shrink-0"><IconEdit /></span>
                Edit listing
              </RouterLink>

              <button
                v-if="isOwner"
                type="button"
                :disabled="deleting"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg cursor-pointer hover:bg-warn/5 hover:text-warn hover:border-warn/30 transition-colors disabled:opacity-50"
                @click="confirmingDelete = true"
              >
                <span class="size-4 shrink-0"><IconTrash /></span>
                Delete listing
              </button>

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

      <MobileStickyBar
        :price="price"
        :price-per-m2="pricePerM2"
        :phones="property.phones"
        :phone-revealed="phoneRevealed"
        @reveal-phone="phoneRevealed = true"
      />
    </template>

    <ConfirmDialog
      :open="confirmingDelete"
      title="Delete listing?"
      description="This listing will be permanently removed and cannot be recovered."
      confirm-label="Delete"
      danger
      @update:open="confirmingDelete = false"
      @confirm="confirmDelete"
    />
  </div>
</template>
