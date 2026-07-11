<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const props = defineProps<{ modelValue: number[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: number[]] }>();

const options: Array<{ value: number; label: string }> = [
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
  { value: 4, label: '4' },
  { value: 5, label: '5' },
  { value: 6, label: '6' },
  { value: 7, label: '7+' },
];

function toggle(v: number) {
  const set = new Set(props.modelValue);
  if (set.has(v)) set.delete(v);
  else set.add(v);
  emit(
    'update:modelValue',
    [...set].sort((a, b) => a - b)
  );
}

function clear() {
  emit('update:modelValue', []);
}
</script>

<template>
  <div class="space-y-3 min-w-60">
    <div class="flex flex-wrap gap-1.5">
      <button
        v-for="opt in options"
        :key="opt.value"
        type="button"
        @click="toggle(opt.value)"
        class="focus-ring inline-flex items-center justify-center min-w-11 h-10 px-3 rounded-md border text-sm transition-colors"
        :class="
          modelValue.includes(opt.value)
            ? 'border-ink bg-ink text-bg'
            : 'border-line-2 text-ink hover:border-ink-3'
        "
      >
        {{ opt.label }} {{ t('filters.rm') }}
      </button>
    </div>
    <div class="flex items-center justify-between pt-3 border-t border-line">
      <button
        type="button"
        class="focus-ring text-xs text-ink-2 underline underline-offset-4 hover:text-ink"
        @click="clear"
      >
        {{ t('filters.clear') }}
      </button>
      <p class="micro-label">
        {{ modelValue.length || t('filters.any') }} {{ t('filters.selected') }}
      </p>
    </div>
  </div>
</template>
