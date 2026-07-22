<script setup lang="ts">
import type { Category, CategoryCounts } from '../../../types/listingItem';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';

defineProps<{
  modelValue: Category | undefined;
  counts?: CategoryCounts;
}>();
const emit = defineEmits<{ 'update:modelValue': [value: Category] }>();

const { categoryOptions } = usePropertyLabels();

function pick(id: Category) {
  emit('update:modelValue', id);
}
</script>

<template>
  <div class="flex items-stretch overflow-x-auto">
    <button
      v-for="tab in categoryOptions"
      :key="tab.id"
      type="button"
      role="tab"
      :aria-selected="modelValue === tab.id"
      @click="pick(tab.id)"
      class="focus-ring relative flex-1 whitespace-nowrap px-4 sm:px-5 py-3 text-left transition-colors duration-200"
      :class="
        modelValue === tab.id
          ? 'bg-surface text-ink'
          : 'text-ink-2 hover:text-ink hover:bg-surface/50'
      "
    >
      <span
        class="inline-flex items-baseline gap-1.5 border-b-2 pb-0.5"
        :class="
          modelValue === tab.id ? 'border-accent-2' : 'border-transparent'
        "
      >
        <span class="text-base font-semibold leading-tight">{{
          tab.label
        }}</span>
        <span
          v-if="counts"
          class="text-sm font-semibold text-ink-2 tabular"
          >{{ counts[tab.id] }}</span
        >
      </span>
    </button>
  </div>
</template>
