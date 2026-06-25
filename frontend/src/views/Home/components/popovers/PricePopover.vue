<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import type { PropertyType } from '../../../../types/propertyItem';

const DEBOUNCE_MS = 280;

const { t } = useI18n();

const fmt = new Intl.NumberFormat('en-IE', { maximumFractionDigits: 0 });

const props = defineProps<{
  min: number | undefined;
  max: number | undefined;
  type: PropertyType;
}>();
const emit = defineEmits<{
  'update:range': [min: number | undefined, max: number | undefined];
}>();

const rawMin = ref(props.min);
const rawMax = ref(props.max);

const displayMin = computed(() =>
  rawMin.value !== undefined ? fmt.format(rawMin.value) : ''
);
const displayMax = computed(() =>
  rawMax.value !== undefined ? fmt.format(rawMax.value) : ''
);

watch(
  () => [props.min, props.max],
  ([mn, mx]) => {
    rawMin.value = mn as number | undefined;
    rawMax.value = mx as number | undefined;
  }
);

const suggestions = computed<
  Array<[number | undefined, number | undefined, string]>
>(() => {
  if (props.type === 'rent') {
    return [
      [undefined, 500, 'Under €500'],
      [500, 900, '€500 – €900'],
      [900, 1500, '€900 – €1,500'],
      [1500, undefined, '€1,500+'],
    ];
  }
  return [
    [undefined, 150_000, 'Under €150k'],
    [150_000, 300_000, '€150k – €300k'],
    [300_000, 600_000, '€300k – €600k'],
    [600_000, undefined, '€600k+'],
  ];
});

let timer: number | null = null;

function onInput(field: 'min' | 'max', e: Event) {
  const raw = (e.target as HTMLInputElement).value.replace(/\D/g, '');
  const num = raw === '' ? undefined : Math.max(0, parseInt(raw, 10));
  const val = num !== undefined && Number.isFinite(num) ? num : undefined;
  if (field === 'min') rawMin.value = val;
  else rawMax.value = val;
  scheduleCommit();
}

function scheduleCommit() {
  if (timer !== null) window.clearTimeout(timer);
  timer = window.setTimeout(() => {
    emit('update:range', rawMin.value, rawMax.value);
  }, DEBOUNCE_MS);
}

function pickPreset(mn: number | undefined, mx: number | undefined) {
  rawMin.value = mn;
  rawMax.value = mx;
  emit('update:range', mn, mx);
}

function clear() {
  rawMin.value = undefined;
  rawMax.value = undefined;
  emit('update:range', undefined, undefined);
}
</script>

<template>
  <div class="space-y-4 min-w-70">
    <div class="grid grid-cols-2 gap-2">
      <label class="flex flex-col gap-1">
        <span class="micro-label">{{
          t('filters.minPrice', { unit: type === 'rent' ? '€ / mo' : '€' })
        }}</span>
        <input
          :value="displayMin"
          @input="onInput('min', $event)"
          inputmode="numeric"
          :placeholder="t('filters.anyAmount')"
          class="focus-ring h-10 px-3 rounded-md border border-line bg-bg text-sm tabular placeholder:text-ink-3"
        />
      </label>
      <label class="flex flex-col gap-1">
        <span class="micro-label">{{
          t('filters.maxPrice', { unit: type === 'rent' ? '€ / mo' : '€' })
        }}</span>
        <input
          :value="displayMax"
          @input="onInput('max', $event)"
          inputmode="numeric"
          :placeholder="t('filters.anyAmount')"
          class="focus-ring h-10 px-3 rounded-md border border-line bg-bg text-sm tabular placeholder:text-ink-3"
        />
      </label>
    </div>
    <div>
      <p class="micro-label mb-2">{{ t('filters.quickPicks') }}</p>
      <div class="flex flex-wrap gap-1.5">
        <button
          v-for="[mn, mx, label] in suggestions"
          :key="label"
          type="button"
          class="focus-ring px-3 h-8 rounded-full border border-line text-xs text-ink-2 hover:text-ink hover:border-line-2 transition-colors"
          @click="pickPreset(mn, mx)"
        >
          {{ label }}
        </button>
      </div>
    </div>
    <div class="flex items-center justify-end pt-3 border-t border-line">
      <button
        type="button"
        class="focus-ring text-xs text-ink-2 underline underline-offset-4 hover:text-ink"
        @click="clear"
      >
        {{ t('filters.clear') }}
      </button>
    </div>
  </div>
</template>
