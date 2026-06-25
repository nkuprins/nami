<script setup lang="ts">
import { type PropertySummary } from '../../types/propertyItem';
import PropertyItem from '../../components/listing/PropertyItem.vue';

defineProps<{
  items: PropertySummary[];
  loading: boolean;
}>();

const skeletons = Array.from({ length: 8 });
</script>

<template>
  <div>
    <div
      v-if="loading && items.length === 0"
      class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 lg:gap-8"
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

    <div
      v-else-if="items.length > 0"
      class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 lg:gap-8"
      :class="{ 'opacity-60 pointer-events-none': loading }"
    >
      <PropertyItem
        v-for="property in items"
        :key="property.id"
        :property="property"
      />
    </div>

    <slot v-else name="empty" />
  </div>
</template>
