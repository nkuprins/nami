<script setup lang="ts">
import type { ListingSummary } from '../../types/listingItem';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { resolveTitle } from '../../types/listingItem';
import { mediaVariant, onVariantError } from '../../utils/mediaVariant';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import StatusPill from './StatusPill.vue';
import VerifiedBadge from './VerifiedBadge.vue';
import SaveHeart from './SaveHeart.vue';
import {
  formatFloor,
  formatPrice,
  formatPricePerM2,
  joinPlace,
} from '../../utils/format';

const props = defineProps<{ property: ListingSummary }>();
const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();

const title = computed(() => resolveTitle(props.property, locale.value));

const price = computed(() =>
  formatPrice(props.property.price.amount, props.property.type, locale.value)
);
const pricePerM2 = computed(() => {
  const m2 = props.property.details.m2;
  return m2
    ? formatPricePerM2(props.property.price.amount / m2, locale.value)
    : '';
});

const specRow = computed(() => {
  const { rooms, m2, floor, totalFloors, landM2 } = props.property.details;
  const { propertyKind } = props.property;
  const parts: { value: string; unit: string }[] = [
    { value: `${rooms}`, unit: t('listing.rm') },
    { value: `${m2}`, unit: 'm²' },
  ];
  if (propertyKind === 'house' && landM2) {
    const landUnit = t('listing.land');
    const firstSpace = landUnit.indexOf(' ');
    parts.push({
      value: `${landM2.toLocaleString()} ${landUnit.slice(0, firstSpace)}`,
      unit: landUnit.slice(firstSpace + 1),
    });
  } else if (floor) {
    const full = formatFloor(floor, totalFloors, locale.value);
    const lastSpace = full.lastIndexOf(' ');
    parts.push({
      value: full.slice(0, lastSpace),
      unit: full.slice(lastSpace + 1),
    });
  }
  return parts;
});
</script>

<template>
  <RouterLink
    :to="localePath(`/listing/${property.id}`)"
    class="focus-ring block group"
  >
    <article
      class="card-lift relative bg-bg shadow-soft rounded-xl overflow-hidden flex gap-4 sm:gap-5 p-3 sm:p-4"
    >
      <div
        class="relative shrink-0 w-32 sm:w-44 aspect-4/3 rounded-lg overflow-hidden bg-surface"
      >
        <img
          :src="
            property.photo ? mediaVariant(property.photo, 'card') : undefined
          "
          :alt="title"
          class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
          @error="(e) => property.photo && onVariantError(e, property.photo)"
        />

        <div class="absolute top-2 left-2 z-10 flex items-center gap-1.5">
          <StatusPill :property="property" />
        </div>

        <div class="absolute top-2 right-2 z-10">
          <SaveHeart :listing-id="property.id" />
        </div>
      </div>

      <div class="flex-1 min-w-0 flex items-start justify-between gap-3 sm:gap-4">
        <div class="min-w-0 flex flex-col self-stretch">
          <div class="micro-label">
            <p class="truncate">
              {{ joinPlace(property.location.city, property.location.district) }}
            </p>
            <p class="truncate">{{ property.location.address }}</p>
          </div>

          <h3
            class="text-[1.0625rem] leading-snug text-ink font-medium mt-1.5 line-clamp-2"
          >
            {{ title }}
          </h3>

          <div class="mt-auto pt-2 flex items-center gap-2 flex-wrap">
            <template v-for="(part, i) in specRow" :key="i">
              <span
                v-if="i > 0"
                class="size-1 rounded-full bg-line-2 shrink-0"
                aria-hidden="true"
              />
              <span class="text-sm font-semibold text-ink-2 whitespace-nowrap">
                <span class="tabular">{{ part.value }}</span> {{ part.unit }}
              </span>
            </template>
          </div>
        </div>

        <div class="shrink-0 text-right self-stretch flex flex-col">
          <div class="flex items-center justify-end gap-2">
            <p class="display-price text-xl sm:text-2xl text-ink whitespace-nowrap">
              {{ price }}
            </p>
            <span
              v-if="property.price.vatIncluded"
              :title="t('listing.vatIncluded')"
              class="text-base font-semibold text-ink whitespace-nowrap shrink-0"
            >
              {{ t('listing.vatBadge') }}
            </span>
          </div>
          <p
            class="text-sm font-semibold text-ink-2 tabular whitespace-nowrap mt-0.5"
          >
            {{ pricePerM2 }}
          </p>
          <div
            v-if="property.cadastreVerified"
            class="mt-auto pt-2 flex justify-end"
          >
            <VerifiedBadge />
          </div>
        </div>
      </div>
    </article>
  </RouterLink>
</template>
