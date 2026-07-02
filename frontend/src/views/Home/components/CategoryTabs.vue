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
      @click="pick(tab.id)"
      class="focus-ring relative flex-1 px-4 sm:px-6 py-3 text-left transition-colors duration-200 border-b-2"
      :class="
        modelValue === tab.id
          ? 'border-ink text-ink'
          : 'border-transparent text-ink-2 hover:text-ink hover:border-line-2'
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
