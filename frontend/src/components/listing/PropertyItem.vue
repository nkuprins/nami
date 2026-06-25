<script setup lang="ts">
import type { PropertySummary } from '../../types/propertyItem';
import { computed } from 'vue';
import StatusPill from './StatusPill.vue';
import SaveHeart from './SaveHeart.vue';
import SpecDots from './SpecDots.vue';
import { formatFloor, formatPrice, formatPricePerM2 } from '../../utils/format';

const props = defineProps<{ property: PropertySummary }>();

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
  parts.push(`${rooms} rooms`);
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
  <RouterLink
    :to="`/property/${property.id}`"
    class="focus-ring block group h-full"
  >
    <article
      class="card-lift relative bg-bg border border-line rounded-xl overflow-hidden h-full flex flex-col"
    >
      <div class="relative aspect-4/3 overflow-hidden shrink-0">
        <img
          :src="property.photo"
          :alt="property.title"
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
          <p class="display-price text-2xl text-ink whitespace-nowrap">
            {{ price }}
          </p>
          <p class="text-sm text-ink-2 tabular whitespace-nowrap shrink-0">
            {{ pricePerM2 }}
          </p>
        </div>

        <h3
          class="text-[1.0625rem] leading-snug text-ink font-medium mt-1 line-clamp-2"
        >
          {{ property.title }}
        </h3>

        <SpecDots :parts="specRow" class="text-sm text-ink-2 mt-auto pt-2" />
      </div>
    </article>
  </RouterLink>
</template>
