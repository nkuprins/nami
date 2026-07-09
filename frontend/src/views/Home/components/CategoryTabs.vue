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
      type="button"
      role="tab"
      :aria-selected="modelValue === tab.id"
      @click="pick(tab.id)"
      class="focus-ring relative flex-1 px-4 sm:px-6 py-3 text-left transition-colors duration-200"
      :class="
        modelValue === tab.id
          ? 'bg-surface text-ink'
          : 'text-ink-2 hover:text-ink hover:bg-surface/50'
      "
    >
      <span
        class="inline-block border-b-2 pb-0.5"
        :class="
          modelValue === tab.id ? 'border-accent-2' : 'border-transparent'
        "
      >
        <span class="text-base font-semibold leading-tight">{{
          tab.label
        }}</span>
      </span>
    </button>
  </div>
</template>
