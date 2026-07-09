<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import type { ListingDetail } from '../../types/listingItem';
import {
  resolveTitle,
  resolveDescription,
  hasLanguage,
} from '../../types/listingItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';
import { renderMarkdown } from '../../utils/renderMarkdown';
import { mediaVariant, onVariantError } from '../../utils/mediaVariant';
import { getListing, deleteListing } from '../../api/listingsApi';
import { useSavedStore } from '../../stores/savedStore';
import { useAuthStore } from '../../stores/authStore';
import { useShare } from '../../composables/useShare';
import type { Locale } from '../../i18n';
import SaveHeart from '../../components/listing/SaveHeart.vue';
import CardCarousel from '../../components/listing/CardCarousel.vue';
import LocationMap from '../../components/listing/LocationMap.vue';
import PhotoGrid from './components/PhotoGrid.vue';
import ContactCard from './components/ContactCard.vue';
import VideoPlayer from './components/VideoPlayer.vue';
import BentoPhotoGrid from './components/BentoPhotoGrid.vue';
import MobileStickyBar from './components/MobileStickyBar.vue';
import PhotoLightBox from '../../components/listing/PhotoLightBox.vue';
import SpecDots from '../../components/listing/SpecDots.vue';
import ConfirmDialog from '../../components/ui/ConfirmDialog.vue';
import IconShare from '../../components/icons/IconShare.vue';
import IconArrowLeft from '../../components/icons/IconArrowLeft.vue';
import IconHeart from '../../components/icons/IconHeart.vue';
import IconEdit from '../../components/icons/IconEdit.vue';
import IconTrash from '../../components/icons/IconTrash.vue';

const props = defineProps<{ id: string }>();
const router = useRouter();
const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();
const { featureLabel, featureIcon, featureCategory } = usePropertyLabels();
const savedStore = useSavedStore();
const authStore = useAuthStore();

const listing = ref<ListingDetail | null>(null);
onMounted(async () => {
  listing.value = (await getListing(props.id)) ?? null;
});

const saved = computed(() => savedStore.isSaved(props.id));
const isOwner = computed(
  () =>
    authStore.user?.id != null && authStore.user.id === listing.value?.ownerId
);

const confirmingDelete = ref(false);
const deleting = ref(false);

async function confirmDelete() {
  deleting.value = true;
  try {
    await deleteListing(props.id);
    await router.replace(localePath('/'));
  } catch {
    deleting.value = false;
  }
}

const contentLocale = ref<Locale>(locale.value);
watch(locale, (l) => {
  contentLocale.value = l;
});

const displayTitle = computed(() =>
  listing.value ? resolveTitle(listing.value, contentLocale.value) : ''
);
const displayDescription = computed(() =>
  listing.value ? resolveDescription(listing.value, contentLocale.value) : ''
);
const renderedDescription = computed(() =>
  displayDescription.value ? renderMarkdown(displayDescription.value) : ''
);
const availableLanguages = computed(() =>
  (['lv', 'en', 'ru'] as const).filter(
    (l) => listing.value && hasLanguage(listing.value, l)
  )
);
const hasMultipleLanguages = computed(
  () => availableLanguages.value.length > 1
);

const price = computed(() =>
  listing.value
    ? formatPrice(listing.value.price.amount, listing.value.type, locale.value)
    : ''
);
const pricePerM2 = computed(() =>
  listing.value
    ? formatPricePerM2(
        listing.value.price.amount / listing.value.details.m2,
        locale.value
      )
    : ''
);

const specRow = computed(() => {
  if (!listing.value) return [];
  const { rooms, m2, floor, totalFloors, landM2, yearBuilt } =
    listing.value.details;
  const { propertyKind } = listing.value;
  const parts: string[] = [`${rooms} ${t('listing.rm')}`, `${m2} m²`];
  if (propertyKind === 'house' && landM2) {
    parts.push(`${landM2.toLocaleString()} ${t('listing.land')}`);
  } else if (floor) {
    parts.push(formatFloor(floor, totalFloors, locale.value));
  }
  if (yearBuilt) parts.push(`${t('listing.built')} ${yearBuilt}`);
  return parts;
});

const phoneRevealed = ref(false);
const mediaTab = ref<'photos' | 'plans' | 'video'>('photos');

function switchToVideo() {
  mediaTab.value = 'video';
}

const videoTourUrl = computed(() => {
  const raw = listing.value?.media.videoUrl;
  return typeof raw === 'string' ? raw.trim() : '';
});

const { share, justCopied } = useShare();

function shareListing() {
  if (!listing.value) return;
  share({ title: displayTitle.value, url: window.location.href });
}

const bentoLightboxOpen = ref(false);
const bentoLightboxIndex = ref(0);

function openBento(i: number) {
  bentoLightboxIndex.value = i;
  bentoLightboxOpen.value = true;
}

const planLightboxOpen = ref(false);
const planLightboxIndex = ref(0);

function openPlanLightbox(i: number) {
  planLightboxIndex.value = i;
  planLightboxOpen.value = true;
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-4 py-6 lg:px-6">
    <button
      v-if="!listing"
      class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-2 hover:text-ink transition-colors bg-transparent border-none p-0 cursor-pointer mb-6"
      @click="router.back()"
    >
      <span class="size-4 shrink-0"><IconArrowLeft /></span>
      {{ t('listing.back') }}
    </button>
    <p v-if="!listing" class="text-sm text-ink-2">Loading&hellip;</p>

    <template v-if="listing">
      <button
        class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-2 hover:text-ink transition-colors mb-6 bg-transparent border-none p-0 cursor-pointer"
        @click="router.back()"
      >
        <span class="size-4 shrink-0"><IconArrowLeft /></span>
        {{ t('listing.back') }}
      </button>

      <!-- Language toggle -->
      <div v-if="hasMultipleLanguages" class="flex gap-2 mb-4">
        <button
          v-for="l in availableLanguages"
          :key="l"
          type="button"
          class="h-7 px-3 rounded-full text-xs font-medium border transition-colors"
          :class="
            contentLocale === l
              ? 'bg-ink text-bg border-ink'
              : 'border-line text-ink-2 hover:text-ink hover:border-ink/40'
          "
          @click="contentLocale = l"
        >
          {{ l.toUpperCase() }}
        </button>
      </div>

      <BentoPhotoGrid
        :photos="listing.media.photos ?? []"
        :alt="displayTitle"
        @open-lightbox="openBento"
      />

      <!-- Mobile carousel -->
      <div
        class="lg:hidden relative aspect-4/3 rounded-lg overflow-hidden mb-6"
      >
        <CardCarousel
          :photos="listing.media.photos ?? []"
          :alt="displayTitle"
          zoomable
        />
        <div class="absolute top-3 right-3 z-10 flex items-center gap-2">
          <RouterLink
            v-if="isOwner"
            :to="localePath(`/listing/${listing.id}/edit`)"
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
            @click.stop="shareListing"
          >
            <span class="size-4"><IconShare /></span>
          </button>
          <SaveHeart :listing-id="listing.id" />
        </div>
      </div>

      <PhotoLightBox
        v-model:open="bentoLightboxOpen"
        :photos="listing.media.photos ?? []"
        :alt="displayTitle"
        :initial-index="bentoLightboxIndex"
      />

      <!-- Two-column layout on desktop -->
      <div class="lg:grid lg:grid-cols-[1fr_320px] lg:gap-10">
        <!-- Main content -->
        <div>
          <div class="mb-1">
            <p class="micro-label">
              {{ listing.location.district }} · {{ listing.location.city }}
            </p>
            <h1 class="mt-1 text-xl leading-snug text-ink font-medium">
              {{ displayTitle }}
            </h1>
            <p class="mt-1 text-sm text-ink-2">
              {{ listing.location.address }}
            </p>
          </div>

          <SpecDots
            :parts="specRow"
            class="lg:hidden text-sm text-ink-2 mt-3"
          />

          <div
            v-if="listing.features?.length"
            class="flex flex-wrap gap-2 mt-4"
          >
            <span
              v-for="f in listing.features"
              :key="f"
              class="inline-flex items-center gap-2 h-9 pl-3 pr-4 rounded-full text-sm font-medium"
              :class="
                featureCategory(f) === 'comfort'
                  ? 'bg-accent/10 text-accent-2'
                  : 'bg-feature-building/10 text-feature-building-2'
              "
            >
              <span class="size-4.5 shrink-0"
                ><component :is="featureIcon(f)"
              /></span>
              {{ featureLabel(f) }}
            </span>
          </div>

          <hr class="border-none border-t border-line my-5" />

          <div
            class="text-sm text-ink-2 leading-relaxed prose-description"
            v-html="renderedDescription"
          />

          <hr class="border-none border-t border-line my-5" />

          <!-- Media toggle + content -->
          <div>
            <div
              v-if="videoTourUrl || listing.media.plans?.length"
              class="flex items-center gap-1.5 mb-4"
            >
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
                {{ t('listing.photos') }}
              </button>
              <button
                v-if="listing.media.plans?.length"
                type="button"
                class="focus-ring px-4 py-1.5 text-sm font-medium rounded-full border transition-colors cursor-pointer"
                :class="
                  mediaTab === 'plans'
                    ? 'bg-ink text-cream border-ink'
                    : 'bg-transparent text-ink-2 border-line hover:border-line-2 hover:text-ink'
                "
                @click="mediaTab = 'plans'"
              >
                {{ t('listing.plans') }}
              </button>
              <button
                v-if="videoTourUrl"
                type="button"
                class="focus-ring px-4 py-1.5 text-sm font-medium rounded-full border transition-colors cursor-pointer"
                :class="
                  mediaTab === 'video'
                    ? 'bg-ink text-cream border-ink'
                    : 'bg-transparent text-ink-2 border-line hover:border-line-2 hover:text-ink'
                "
                @click="switchToVideo()"
              >
                {{ t('listing.video') }}
              </button>
            </div>

            <PhotoGrid
              v-if="mediaTab === 'photos'"
              :photos="listing.media.photos ?? []"
              :alt="displayTitle"
              :video-url="videoTourUrl"
              @play-video="switchToVideo()"
            />

            <div
              v-else-if="mediaTab === 'plans' && listing.media.plans?.length"
              class="grid grid-cols-2 sm:grid-cols-3 gap-2"
            >
              <img
                v-for="(url, i) in listing.media.plans"
                :key="i"
                :src="mediaVariant(url, 'card')"
                class="w-full rounded-lg object-contain bg-surface cursor-pointer"
                :alt="`${t('listing.plans')} ${i + 1}`"
                @click="openPlanLightbox(i)"
                @error="(e) => onVariantError(e, url)"
              />
            </div>

            <VideoPlayer
              v-else-if="mediaTab === 'video' && videoTourUrl"
              :video-url="videoTourUrl"
              :alt="displayTitle"
            />
          </div>

          <PhotoLightBox
            v-model:open="planLightboxOpen"
            :photos="listing.media.plans ?? []"
            :alt="t('listing.plans')"
            :initial-index="planLightboxIndex"
          />

          <div v-if="listing.location.coords" class="my-5">
            <p class="micro-label mb-3">{{ t('listing.location') }}</p>
            <LocationMap
              :model-value="listing.location.coords"
              :address="listing.location.address"
              :district="listing.location.district"
              :city="listing.location.city"
              readonly
            />
          </div>

          <!-- Contact section: mobile only -->
          <div class="lg:hidden">
            <hr class="border-none border-t border-line my-5" />
            <div>
              <p class="micro-label mb-4">{{ t('listing.contact') }}</p>
              <ContactCard
                :phones="listing.phones ?? undefined"
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
              <p class="display-price text-2xl text-ink">{{ price }}</p>
              <p class="text-xs text-ink-2 tabular mt-1">{{ pricePerM2 }}</p>
              <p
                v-if="listing.price.vatIncluded"
                class="text-xs text-ink-3 mt-0.5"
              >
                {{ t('listing.vatIncluded') }}
              </p>

              <SpecDots
                :parts="specRow"
                class="text-xs text-ink-2 mt-3 pb-5 border-b border-line"
              />

              <button
                type="button"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-4 text-sm font-medium bg-transparent border border-line rounded-lg cursor-pointer hover:bg-surface transition-colors"
                :class="saved ? 'text-accent-2' : 'text-ink-2 hover:text-ink'"
                @click="savedStore.toggle(listing.id)"
              >
                <span class="size-4 shrink-0"
                  ><IconHeart :filled="saved"
                /></span>
                {{ saved ? t('listing.saved') : t('listing.save') }}
              </button>

              <button
                type="button"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg cursor-pointer hover:bg-surface hover:text-ink transition-colors"
                @click="shareListing"
              >
                <span class="size-4 shrink-0"><IconShare /></span>
                {{ justCopied ? t('listing.linkCopied') : t('listing.share') }}
              </button>

              <RouterLink
                v-if="isOwner"
                :to="localePath(`/listing/${listing.id}/edit`)"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg hover:bg-surface hover:text-ink transition-colors"
              >
                <span class="size-4 shrink-0"><IconEdit /></span>
                {{ t('listing.edit') }}
              </RouterLink>

              <button
                v-if="isOwner"
                type="button"
                :disabled="deleting"
                class="w-full flex items-center justify-center gap-1.5 py-2.5 mt-2 text-sm font-medium text-ink-2 bg-transparent border border-line rounded-lg cursor-pointer hover:bg-warn/5 hover:text-warn hover:border-warn/30 transition-colors disabled:opacity-50"
                @click="confirmingDelete = true"
              >
                <span class="size-4 shrink-0"><IconTrash /></span>
                {{ t('listing.delete') }}
              </button>

              <ContactCard
                class="mt-5"
                :phones="listing.phones ?? undefined"
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
        :phones="listing.phones ?? undefined"
        :phone-revealed="phoneRevealed"
        @reveal-phone="phoneRevealed = true"
      />
    </template>

    <ConfirmDialog
      :open="confirmingDelete"
      :title="t('listing.deleteConfirmTitle')"
      :description="t('listing.deleteConfirmDesc')"
      :confirm-label="t('listing.deleteConfirmLabel')"
      danger
      @update:open="confirmingDelete = false"
      @confirm="confirmDelete"
    />
  </div>
</template>
