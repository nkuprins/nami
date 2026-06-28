<script setup lang="ts">
import type { PropertySummary } from '../../types/propertyItem';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { resolveTitle } from '../../types/propertyItem';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import StatusPill from './StatusPill.vue';
import SaveHeart from './SaveHeart.vue';
import SpecDots from './SpecDots.vue';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';

const props = defineProps<{ property: PropertySummary }>();
const { t } = useI18n();
const { locale, localePath } = useLocaleRoute();
const { typeLabel } = usePropertyLabels();

const title = computed(() => resolveTitle(props.property, locale.value));

const price = computed(() =>
  formatPrice(props.property.price, props.property.type, locale.value)
);
const pricePerM2 = computed(() =>
  formatPricePerM2(props.property.price / props.property.m2, locale.value)
);
const rentPrice = computed(() =>
  props.property.rentPrice != null
    ? formatPrice(props.property.rentPrice, 'rent', locale.value)
    : null
);

const specRow = computed(() => {
  const { rooms, m2, floor, totalFloors, landM2, propertyKind } =
    props.property;
  const parts: string[] = [];
  parts.push(`${rooms} ${t('property.rm')}`);
  parts.push(`${m2} m²`);
  if (propertyKind === 'house' && landM2) {
    parts.push(`${landM2.toLocaleString()} ${t('property.land')}`);
  } else if (floor) {
    parts.push(formatFloor(floor, totalFloors, locale.value));
  }
  return parts;
});
</script>

<template>
  <RouterLink
    :to="localePath(`/property/${property.id}`)"
    class="focus-ring block group h-full"
  >
    <article
      class="card-lift relative bg-bg border border-line rounded-xl overflow-hidden h-full flex flex-col"
    >
      <div class="relative aspect-4/3 overflow-hidden shrink-0">
        <img
          :src="property.photo"
          :alt="title"
          class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        />

        <div class="absolute top-3 left-3 z-10 flex items-center gap-1.5">
          <StatusPill :property="property" />
        </div>

        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id" />
        </div>
      </div>

      <div class="p-5 flex flex-col flex-1">
        <div class="micro-label">
          <p class="truncate">{{ property.city }} · {{ property.district }}</p>
          <p class="truncate">{{ property.address }}</p>
        </div>

        <div class="flex items-baseline justify-between gap-4 mt-2">
          <div>
            <p class="display-price text-2xl text-ink whitespace-nowrap">
              {{ price }}
            </p>
            <p
              v-if="rentPrice"
              class="text-sm text-ink-2 whitespace-nowrap mt-0.5"
            >
              {{ t('property.alsoForRent') }}: {{ rentPrice }}
            </p>
          </div>
          <div class="text-right shrink-0">
            <p class="text-sm text-ink-2 tabular whitespace-nowrap">
              {{ pricePerM2 }}
            </p>
            <p
              v-if="property.buyVatIncluded"
              class="text-xs text-ink-3 whitespace-nowrap mt-0.5"
            >
              {{ t('property.vatIncluded') }}
            </p>
          </div>
        </div>

        <h3
          class="text-[1.0625rem] leading-snug text-ink font-medium mt-1 line-clamp-2"
        >
          {{ title }}
        </h3>

        <SpecDots :parts="specRow" class="text-sm text-ink-2 mt-auto pt-2" />
      </div>
    </article>
  </RouterLink>
</template>
