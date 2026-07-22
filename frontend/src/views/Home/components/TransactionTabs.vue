<script setup lang="ts">
import type { ListingType } from '../../../types/listingItem';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';

defineProps<{ modelValue: ListingType }>();
const emit = defineEmits<{ 'update:modelValue': [value: ListingType] }>();

const { typeOptions } = usePropertyLabels();

function pick(id: ListingType) {
  emit('update:modelValue', id);
}
</script>

<template>
  <div
    class="flex items-stretch border-b border-line"
    role="tablist"
    :aria-label="'transaction'"
  >
    <button
      v-for="tab in typeOptions"
      :key="tab.id"
      type="button"
      role="tab"
      :aria-selected="modelValue === tab.id"
      @click="pick(tab.id)"
      class="focus-ring relative flex-1 px-4 sm:px-6 py-2.5 text-center transition-colors duration-200"
      :class="
        modelValue === tab.id
          ? 'bg-surface text-ink'
          : 'text-ink-2 hover:text-ink hover:bg-surface/50'
      "
    >
      <span
        class="inline-block border-b-2 pb-0.5 text-sm font-semibold leading-tight"
        :class="modelValue === tab.id ? 'border-accent-2' : 'border-transparent'"
      >
        {{ tab.label }}
      </span>
    </button>
  </div>
</template>
