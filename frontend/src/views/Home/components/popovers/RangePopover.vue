<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { groupFmt } from '../../../../utils/format';
import { numericInput } from '../../../../utils/utils';

const DEBOUNCE_MS = 280;

const { t } = useI18n();

const props = defineProps<{
  min: number | undefined;
  max: number | undefined;
  unit?: string;
  // [min, max, label] — either bound may be undefined for an open-ended pick.
  presets?: Array<[number | undefined, number | undefined, string]>;
}>();
const emit = defineEmits<{
  'update:range': [min: number | undefined, max: number | undefined];
}>();

const rawMin = ref(props.min);
const rawMax = ref(props.max);

const displayMin = computed(() =>
  rawMin.value !== undefined ? groupFmt.format(rawMin.value) : ''
);
const displayMax = computed(() =>
  rawMax.value !== undefined ? groupFmt.format(rawMax.value) : ''
);

watch(
  () => [props.min, props.max],
  ([mn, mx]) => {
    rawMin.value = mn as number | undefined;
    rawMax.value = mx as number | undefined;
  }
);

let timer: number | null = null;

function onInput(field: 'min' | 'max', e: Event) {
  const raw = (e.target as HTMLInputElement).value.replace(/\D/g, '');
  const num = raw === '' ? undefined : Math.max(0, Number.parseInt(raw, 10));
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
</script>

<template>
  <div class="space-y-4 min-w-64">
    <div class="grid grid-cols-2 gap-2">
      <label class="flex flex-col gap-1">
        <span class="micro-label">{{ t('advFilters.min') }}</span>
        <input
          :value="displayMin"
          @input="onInput('min', $event)"
          @beforeinput="numericInput"
          inputmode="numeric"
          :placeholder="unit || t('filters.any')"
          class="focus-ring h-10 px-3 rounded-md border border-line-2 bg-bg text-sm tabular placeholder:text-ink-3"
        />
      </label>
      <label class="flex flex-col gap-1">
        <span class="micro-label">{{ t('advFilters.max') }}</span>
        <input
          :value="displayMax"
          @input="onInput('max', $event)"
          @beforeinput="numericInput"
          inputmode="numeric"
          :placeholder="unit || t('filters.any')"
          class="focus-ring h-10 px-3 rounded-md border border-line-2 bg-bg text-sm tabular placeholder:text-ink-3"
        />
      </label>
    </div>
    <div v-if="presets?.length">
      <p class="micro-label mb-2">{{ t('filters.quickPicks') }}</p>
      <div class="flex flex-wrap gap-1.5">
        <button
          v-for="[mn, mx, label] in presets"
          :key="label"
          type="button"
          class="focus-ring px-3 h-8 rounded-full border border-line-2 text-xs text-ink-2 hover:text-ink hover:border-ink-3 transition-colors"
          @click="pickPreset(mn, mx)"
        >
          {{ label }}
        </button>
      </div>
    </div>

    <div v-if="$slots.footer" class="pt-3 border-t border-line">
      <slot name="footer" />
    </div>
  </div>
</template>
