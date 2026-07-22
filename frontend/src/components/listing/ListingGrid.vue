<script setup lang="ts">
import { computed } from 'vue';
import { type ListingSummary } from '../../types/listingItem';
import type { ViewMode } from '../../composables/useViewMode';
import ListingCard from '../../components/listing/ListingCard.vue';
import ListingRow from '../../components/listing/ListingRow.vue';

const props = withDefaults(
  defineProps<{
    items: ListingSummary[];
    loading: boolean;
    view?: ViewMode;
    // Cap columns so cards keep their size in the narrow map-split panel.
    maxCols?: 2 | 4;
  }>(),
  { view: 'grid', maxCols: 4 }
);

// Two columns max keeps cards full-size beside the map; four is the roomy
// full-width default.
const gridCols = computed(() =>
  props.maxCols === 2
    ? 'grid-cols-1 sm:grid-cols-2'
    : 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'
);

const skeletons = Array.from({ length: 8 });
const rowSkeletons = Array.from({ length: 6 });
</script>

<template>
  <div class="mb-6">
    <!-- Skeletons -->
    <template v-if="loading && items.length === 0">
      <div
        v-if="view === 'grid'"
        class="grid gap-6 lg:gap-8"
        :class="gridCols"
      >
        <div
          v-for="(_, i) in skeletons"
          :key="i"
          class="bg-bg border border-line rounded-xl overflow-hidden animate-pulse"
        >
          <div class="aspect-4/3 bg-surface" />
          <div class="p-5 space-y-3">
            <div class="h-3 w-24 bg-surface rounded-sm" />
            <div class="h-5 w-3/4 bg-surface rounded-sm" />
            <div class="h-4 w-1/2 bg-surface rounded-sm" />
            <div class="pt-3 border-t border-line h-7 bg-surface rounded-sm" />
          </div>
        </div>
      </div>

      <div v-else class="flex flex-col gap-3 sm:gap-4">
        <div
          v-for="(_, i) in rowSkeletons"
          :key="i"
          class="bg-bg border border-line rounded-xl overflow-hidden animate-pulse flex gap-4 sm:gap-5 p-3 sm:p-4"
        >
          <div class="shrink-0 w-32 sm:w-44 aspect-4/3 bg-surface rounded-lg" />
          <div class="flex-1 py-1 space-y-3">
            <div class="h-3 w-24 bg-surface rounded-sm" />
            <div class="h-5 w-2/3 bg-surface rounded-sm" />
            <div class="h-4 w-1/3 bg-surface rounded-sm" />
          </div>
        </div>
      </div>
    </template>

    <!-- Results -->
    <template v-else-if="items.length > 0">
      <div
        v-if="view === 'grid'"
        class="grid gap-6 lg:gap-8"
        :class="[gridCols, { 'opacity-60 pointer-events-none': loading }]"
      >
        <ListingCard
          v-for="property in items"
          :key="property.id"
          :property="property"
        />
      </div>

      <div
        v-else
        class="flex flex-col gap-3 sm:gap-4"
        :class="{ 'opacity-60 pointer-events-none': loading }"
      >
        <ListingRow
          v-for="property in items"
          :key="property.id"
          :property="property"
        />
      </div>
    </template>

    <slot v-else name="empty" />
  </div>
</template>
