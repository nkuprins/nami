<script setup lang="ts">
import type { ListingType } from '../../../types/listingItem';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';

defineProps<{ modelValue: ListingType }>();
const emit = defineEmits<{ 'update:modelValue': [value: ListingType] }>();

const { categoryOptions } = usePropertyLabels();

function pick(id: ListingType) {
  emit('update:modelValue', id);
}
</script>

<template>
  <div class="flex items-stretch">
    <button
      v-for="tab in categoryOptions"
      :key="tab.id"
      role="tab"
      :aria-selected="modelValue === tab.id"
      @click="pick(tab.id)"
      class="focus-ring relative flex-1 px-4 sm:px-6 py-3 text-left transition-colors duration-200 border-b-2"
      :class="
        modelValue === tab.id
          ? 'bg-surface text-ink border-accent'
          : 'text-ink-2 border-transparent hover:text-ink hover:bg-surface/50'
      "
    >
      <span class="block text-sm font-medium leading-tight">{{
        tab.label
      }}</span>
      <span class="block micro-label text-[0.625rem]! mt-0.5">{{
        tab.hint
      }}</span>
    </button>
  </div>
</template>
