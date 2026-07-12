<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ROOM_COUNT_OPTIONS, roomCountLabel } from '../../../../types/filter';

const { t } = useI18n();
const props = defineProps<{ modelValue: number[] }>();
const emit = defineEmits<{ 'update:modelValue': [value: number[]] }>();

const options = ROOM_COUNT_OPTIONS.map((value) => ({
  value,
  label: roomCountLabel(value),
}));

function toggle(v: number) {
  const set = new Set(props.modelValue);
  if (set.has(v)) set.delete(v);
  else set.add(v);
  emit(
    'update:modelValue',
    [...set].sort((a, b) => a - b)
  );
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
    <div class="pt-3 border-t border-line text-right">
      <p class="micro-label">
        {{ modelValue.length || t('filters.any') }} {{ t('filters.selected') }}
      </p>
    </div>
  </div>
</template>
