<script setup lang="ts">

import {PropertyItem} from "../../types/propertyItem";
import {computed} from "vue";
import CardCarousel from "./CardCarousel.vue";
import StatusPill from "./StatusPill.vue";
import SaveHeart from "./SaveHeart.vue";
import {formatFloor, formatPrice, formatPricePerM2} from "../../utils/format";

const props = defineProps<{ property: PropertyItem }>();

const price = computed(() => formatPrice(props.property.price, props.property.type));
const pricePerM2 = computed(() => formatPricePerM2(props.property.price / props.property.m2));

const specRow = computed(() => {
  const {rooms, m2, floor, totalFloors, landM2, propertyKind} = props.property;
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

const typeLabel = computed(() => {
  switch (props.property.type) {
    case 'rent':
      return 'For rent';
    case 'new-project':
      return 'New project';
    default:
      return 'For sale';
  }
});

</script>

<template>
  <RouterLink
      :to="`/listing/${property.id}`"
      class="focus-ring block group"
  >
    <article class="card-lift relative bg-bg border border-line rounded-xl overflow-hidden">
      <div class="relative aspect-4/3 overflow-hidden">
        <CardCarousel :photos="property.photos" :alt="property.title"/>

        <div class="absolute top-3 left-3 z-10 flex items-center gap-1.5">
          <StatusPill :property="property"/>
        </div>

        <div class="absolute top-3 right-3 z-10">
          <SaveHeart :property-id="property.id"/>
        </div>

        <div
            class="absolute left-3 bottom-3 z-10 micro-label text-cream!
                 bg-ink/55 backdrop-blur px-2 h-6 inline-flex items-center rounded-sm"
        >
          {{ typeLabel }}
        </div>
      </div>

      <div class="p-5 space-y-3">
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0">
          <p class="micro-label">{{ property.district }} · {{ property.city }}</p>
          <h3 class="mt-1 text-[1.0625rem] leading-snug text-ink font-medium line-clamp-2 h-[2.6em]">
            {{ property.title }}
          </h3>
        </div>
        <div class="text-right shrink-0">
          <p class="display-price text-2xl text-ink whitespace-nowrap">{{ price }}</p>
          <p class="text-xs text-ink-2 tabular">{{ pricePerM2 }}</p>
        </div>
      </div>

      <div class="flex items-center flex-wrap gap-x-3 gap-y-1 text-sm text-ink-2 tabular">
          <span v-for="(part, i) in specRow" :key="i" class="inline-flex items-center gap-3">
            {{ part }}
            <span v-if="i < specRow.length - 1" class="text-ink-3" aria-hidden="true">·</span>
          </span>
      </div>

      <div class="text-sm text-ink-2">
        {{ property.address }}
      </div>
    </div>
  </article>
  </RouterLink>
</template>