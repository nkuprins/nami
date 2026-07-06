<script setup lang="ts">
import { reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../../../components/ui/Drawer.vue';
import { useFiltersStore } from '../../../stores/filterStore';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { FilterState } from '../../../types/filter';
import { Feature } from '../../../types/listingItem';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const { t } = useI18n();
const { state, applyAdvanced, resetAdvanced } = useFiltersStore();
const {
  featureOptions,
  completionOptions,
  heatingOptions,
  energyClassOptions,
} = usePropertyLabels();

const BEDROOM_BATHROOM_OPTIONS = [1, 2, 3, 4, 5];

type Draft = Pick<
  FilterState,
  | 'm2Min'
  | 'm2Max'
  | 'floorMin'
  | 'floorMax'
  | 'notGround'
  | 'notTop'
  | 'yearMin'
  | 'yearMax'
  | 'bedrooms'
  | 'bathrooms'
  | 'heating'
  | 'energyClass'
  | 'features'
  | 'completion'
>;

const draft = reactive<Draft>(makeDraftFromState());

function makeDraftFromState(): Draft {
  return {
    m2Min: state.m2Min,
    m2Max: state.m2Max,
    floorMin: state.floorMin,
    floorMax: state.floorMax,
    notGround: state.notGround,
    notTop: state.notTop,
    yearMin: state.yearMin,
    yearMax: state.yearMax,
    bedrooms: [...state.bedrooms],
    bathrooms: [...state.bathrooms],
    heating: [...state.heating],
    energyClass: [...state.energyClass],
    features: [...state.features],
    completion: state.completion,
  };
}

function toggleNumeric(key: 'bedrooms' | 'bathrooms', v: number) {
  const set = new Set(draft[key]);
  if (set.has(v)) set.delete(v);
  else set.add(v);
  draft[key] = [...set].sort((a, b) => a - b);
}

function toggleHeating(id: (typeof draft.heating)[number]) {
  const set = new Set(draft.heating);
  if (set.has(id)) set.delete(id);
  else set.add(id);
  draft.heating = [...set];
}

function toggleEnergyClass(id: (typeof draft.energyClass)[number]) {
  const set = new Set(draft.energyClass);
  if (set.has(id)) set.delete(id);
  else set.add(id);
  draft.energyClass = [...set];
}

watch(
  () => props.open,
  (val) => {
    if (val) Object.assign(draft, makeDraftFromState());
  }
);

function toggleFeature(f: Feature) {
  const set = new Set(draft.features);
  if (set.has(f)) set.delete(f);
  else set.add(f);
  draft.features = [...set];
}

function bindNumber(
  key: 'm2Min' | 'm2Max' | 'floorMin' | 'floorMax' | 'yearMin' | 'yearMax'
) {
  return {
    get value() {
      const v = draft[key];
      return v === undefined ? '' : String(v);
    },
    set value(v: string) {
      const n = v === '' ? undefined : Number.parseInt(v, 10);
      draft[key] = Number.isFinite(n as number) ? n : undefined;
    },
  };
}

const m2Min = bindNumber('m2Min');
const m2Max = bindNumber('m2Max');
const floorMin = bindNumber('floorMin');
const floorMax = bindNumber('floorMax');
const yearMin = bindNumber('yearMin');
const yearMax = bindNumber('yearMax');

function apply() {
  applyAdvanced({ ...draft });
  emit('update:open', false);
}

function reset() {
  resetAdvanced();
  Object.assign(draft, makeDraftFromState());
}
</script>

<template>
  <Drawer
    :open="open"
    :title="t('advFilters.title')"
    @update:open="(v) => emit('update:open', v)"
  >
    <div class="space-y-8">
      <section>
        <p class="micro-label mb-2">{{ t('advFilters.area') }}</p>
        <div class="grid grid-cols-2 gap-2">
          <input
            v-model="m2Min.value"
            type="number"
            min="0"
            :placeholder="t('advFilters.min')"
            :aria-label="`${t('advFilters.area')} ${t('advFilters.min')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
            v-model="m2Max.value"
            type="number"
            min="0"
            :placeholder="t('advFilters.max')"
            :aria-label="`${t('advFilters.area')} ${t('advFilters.max')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.floor') }}</p>
        <div class="grid grid-cols-2 gap-2 mb-2">
          <input
            v-model="floorMin.value"
            type="number"
            min="0"
            :placeholder="t('advFilters.min')"
            :aria-label="`${t('advFilters.floor')} ${t('advFilters.min')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
            v-model="floorMax.value"
            type="number"
            min="0"
            :placeholder="t('advFilters.max')"
            :aria-label="`${t('advFilters.floor')} ${t('advFilters.max')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
        </div>
        <div class="flex flex-wrap gap-2">
          <label
            class="inline-flex items-center gap-2 text-sm text-ink-2 cursor-pointer"
          >
            <input
              type="checkbox"
              :checked="!!draft.notGround"
              @change="draft.notGround = !draft.notGround || undefined"
              class="accent-ink size-4"
            />
            {{ t('advFilters.notGround') }}
          </label>
          <label
            class="inline-flex items-center gap-2 text-sm text-ink-2 cursor-pointer"
          >
            <input
              type="checkbox"
              :checked="!!draft.notTop"
              @change="draft.notTop = !draft.notTop || undefined"
              class="accent-ink size-4"
            />
            {{ t('advFilters.notTop') }}
          </label>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.bedrooms') }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="n in BEDROOM_BATHROOM_OPTIONS"
            :key="n"
            type="button"
            @click="toggleNumeric('bedrooms', n)"
            class="focus-ring inline-flex items-center justify-center min-w-11 h-10 px-3 rounded-md border text-sm transition-colors"
            :class="
              draft.bedrooms.includes(n)
                ? 'border-ink bg-ink text-bg'
                : 'border-line text-ink hover:border-line-2'
            "
          >
            {{ n }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.bathrooms') }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="n in BEDROOM_BATHROOM_OPTIONS"
            :key="n"
            type="button"
            @click="toggleNumeric('bathrooms', n)"
            class="focus-ring inline-flex items-center justify-center min-w-11 h-10 px-3 rounded-md border text-sm transition-colors"
            :class="
              draft.bathrooms.includes(n)
                ? 'border-ink bg-ink text-bg'
                : 'border-line text-ink hover:border-line-2'
            "
          >
            {{ n }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.yearBuilt') }}</p>
        <div class="grid grid-cols-2 gap-2">
          <input
            v-model="yearMin.value"
            type="number"
            min="1800"
            max="2030"
            :placeholder="t('advFilters.from')"
            :aria-label="`${t('advFilters.yearBuilt')} ${t('advFilters.from')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
            v-model="yearMax.value"
            type="number"
            min="1800"
            max="2030"
            :placeholder="t('advFilters.to')"
            :aria-label="`${t('advFilters.yearBuilt')} ${t('advFilters.to')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
        </div>
      </section>

      <section>
        <p class="micro-label mb-3">{{ t('advFilters.features') }}</p>
        <div class="grid grid-cols-1 gap-1.5">
          <label
            v-for="opt in featureOptions"
            :key="opt.id"
            class="focus-ring flex items-center justify-between gap-3 px-3 py-2.5 rounded-md border border-line hover:border-line-2 cursor-pointer transition-colors"
            :class="
              draft.features.includes(opt.id) ? 'bg-cream/70 border-line-2' : ''
            "
          >
            <div class="flex items-center gap-3">
              <input
                type="checkbox"
                :checked="draft.features.includes(opt.id)"
                @change="toggleFeature(opt.id)"
                class="accent-ink size-4"
              />
              <div>
                <p class="text-sm text-ink leading-tight">{{ opt.label }}</p>
                <p class="micro-label text-[0.625rem]!">{{ opt.hint }}</p>
              </div>
            </div>
          </label>
        </div>
      </section>

      <section>
        <p class="micro-label mb-3">{{ t('advFilters.heating') }}</p>
        <div class="grid grid-cols-1 gap-1.5">
          <label
            v-for="opt in heatingOptions"
            :key="opt.id"
            class="focus-ring flex items-center gap-3 px-3 py-2.5 rounded-md border border-line hover:border-line-2 cursor-pointer transition-colors"
            :class="
              draft.heating.includes(opt.id) ? 'bg-cream/70 border-line-2' : ''
            "
          >
            <input
              type="checkbox"
              :checked="draft.heating.includes(opt.id)"
              @change="toggleHeating(opt.id)"
              class="accent-ink size-4"
            />
            <p class="text-sm text-ink leading-tight">{{ opt.label }}</p>
          </label>
        </div>
      </section>

      <section>
        <p class="micro-label mb-3">{{ t('advFilters.energyClass') }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="opt in energyClassOptions"
            :key="opt.id"
            type="button"
            @click="toggleEnergyClass(opt.id)"
            class="focus-ring inline-flex items-center justify-center min-w-10 h-10 px-3 rounded-md border text-sm transition-colors"
            :class="
              draft.energyClass.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line text-ink hover:border-line-2'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section v-if="state.type === 'new_project'">
        <p class="micro-label mb-3">{{ t('advFilters.completion') }}</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in completionOptions"
            :key="opt.id"
            type="button"
            @click="
              draft.completion =
                draft.completion === opt.id ? undefined : opt.id
            "
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.completion === opt.id
                ? 'border-ink bg-ink text-bg'
                : 'border-line text-ink hover:border-line-2'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="flex items-center justify-between gap-4">
        <button
          type="button"
          class="focus-ring text-sm text-ink-2 hover:text-ink underline underline-offset-4"
          @click="reset"
        >
          {{ t('advFilters.resetAdvanced') }}
        </button>
        <button
          type="button"
          @click="apply"
          class="focus-ring inline-flex items-center justify-center gap-2 h-11 px-5 rounded-md bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors min-w-44"
        >
          <span>{{ t('advFilters.apply') }}</span>
        </button>
      </div>
    </template>
  </Drawer>
</template>
