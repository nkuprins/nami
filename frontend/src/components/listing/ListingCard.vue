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
    // t('listing.land') is "m² land" / "m² zeme" / "м² участок" — the m²
    // symbol is always the first token, so fold it into the value to avoid
    // showing "m²" twice in a row right under the m² spec above it.
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
    class="focus-ring block group h-full"
  >
    <article
      class="card-lift relative bg-bg shadow-soft rounded-xl overflow-hidden h-full flex flex-col"
    >
      <div class="relative aspect-4/3 overflow-hidden shrink-0">
        <img
          :src="
            property.photo ? mediaVariant(property.photo, 'card') : undefined
          "
          :alt="title"
          class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
          @error="(e) => property.photo && onVariantError(e, property.photo)"
        />

        <div class="absolute top-3 left-3 z-10 flex items-center gap-1.5">
          <StatusPill :property="property" />
          <VerifiedBadge v-if="property.cadastreVerified" />
        </div>

        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :listing-id="property.id" />
        </div>
      </div>

      <div class="p-5 flex flex-col flex-1">
        <div class="micro-label">
          <p class="truncate">
            {{ joinPlace(property.location.city, property.location.district) }}
          </p>
          <p class="truncate">{{ property.location.address }}</p>
        </div>

        <div class="mt-2 flex items-center justify-between gap-3">
          <div>
            <div class="flex items-center gap-2.5">
              <p class="display-price text-2xl text-ink whitespace-nowrap">
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
          </div>

          <div class="grid grid-cols-[auto_auto] gap-x-1.5 gap-y-0.5 shrink-0">
            <template v-for="(part, i) in specRow" :key="i">
              <span
                class="text-sm font-semibold text-ink-2 tabular text-right whitespace-nowrap"
              >
                {{ part.value }}
              </span>
              <span
                class="text-sm font-semibold text-ink-2 text-left whitespace-nowrap"
              >
                {{ part.unit }}
              </span>
            </template>
          </div>
        </div>

        <h3
          class="text-[1.0625rem] leading-snug text-ink font-medium mt-2 line-clamp-2"
        >
          {{ title }}
        </h3>
      </div>
    </article>
  </RouterLink>
</template>
