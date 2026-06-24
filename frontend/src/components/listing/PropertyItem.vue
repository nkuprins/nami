<script setup lang="ts">
import type { PropertyItem } from '../../types/propertyItem';
import { TYPES_LABELS } from '../../types/propertyLabels';
import { computed } from 'vue';
import CardCarousel from './CardCarousel.vue';
import StatusPill from './StatusPill.vue';
import SaveHeart from './SaveHeart.vue';
import SpecDots from './SpecDots.vue';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';

const props = defineProps<{ property: PropertyItem }>();

const price = computed(() =>
  formatPrice(props.property.price, props.property.type)
);
const pricePerM2 = computed(() =>
  formatPricePerM2(props.property.price / props.property.m2)
);

const specRow = computed(() => {
  const { rooms, m2, floor, totalFloors, landM2, propertyKind } =
    props.property;
  const parts: string[] = [];
  parts.push(`${rooms} rm`);
  parts.push(`${m2} m²`);

  if (propertyKind === 'house' && landM2) {
    parts.push(`${landM2.toLocaleString()} m² land`);
  } else if (floor) {
    parts.push(formatFloor(floor, totalFloors));
  }

  return parts;
});
</script>

<template>
  <RouterLink :to="`/property/${property.id}`" class="focus-ring block group">
    <article
      class="card-lift relative bg-bg border border-line rounded-xl overflow-hidden"
    >
      <div class="relative aspect-4/3 overflow-hidden">
        <CardCarousel :photos="property.photos" :alt="property.title" />

        <div class="absolute top-3 left-3 z-10 flex items-center gap-1.5">
          <StatusPill :property="property" />
        </div>

        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id" />
        </div>
      </div>

      <div class="p-5">
        <div class="flex items-baseline justify-between gap-4">
          <p class="micro-label truncate min-w-0">
            {{ property.district }} · {{ property.city }}
          </p>
          <p
            class="display-price text-2xl text-ink whitespace-nowrap shrink-0"
          >
            {{ price }}
          </p>
        </div>

        <div class="flex items-baseline justify-between gap-4 mt-1">
          <h3
            class="text-[1.0625rem] leading-snug text-ink font-medium truncate min-w-0"
          >
            {{ property.title }}
          </h3>
          <p class="text-sm text-ink-2 tabular whitespace-nowrap shrink-0">
            {{ pricePerM2 }}
          </p>
        </div>

        <SpecDots :parts="specRow" class="text-sm text-ink-2 mt-3" />

        <div class="text-sm text-ink-2 mt-2">
          {{ property.address }}
        </div>
      </div>
    </article>
  </RouterLink>
</template>
