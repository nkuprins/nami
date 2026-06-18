<script setup lang="ts">
import type {PropertyType} from '../../types/propertyItem';

defineProps<{ modelValue: PropertyType }>();
const emit = defineEmits<{ 'update:modelValue': [value: PropertyType] }>();

const tabs: Array<{ id: PropertyType; label: string; hint: string }> = [
  {id: 'buy', label: 'Buy', hint: 'For sale'},
  {id: 'rent', label: 'Rent', hint: 'Monthly'},
  {id: 'new_project', label: 'New projects', hint: ''},
];

function pick(id: PropertyType) {
  emit('update:modelValue', id);
}
</script>

<template>
  <div class="flex items-stretch">
    <button
        v-for="tab in tabs"
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
