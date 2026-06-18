<script setup lang="ts">
import {reactive, watch} from 'vue';
import Drawer from '../ui/Drawer.vue';
import {useFiltersStore} from '../../stores/filterStore';
import type {FilterState} from '../../types/filter';
import {Feature} from '../../types/propertyItem';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const {state, applyAdvanced, resetAdvanced} = useFiltersStore();

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
    features: [...state.features],
    completion: state.completion,
  };
}

watch(
    () => props.open,
    (val) => {
      if (val) Object.assign(draft, makeDraftFromState());
    }
);

const FEATURE_OPTIONS: Array<{ id: Feature; label: string; hint: string }> = [
  {id: 'balcony', label: 'Balcony', hint: 'Open-air space'},
  {id: 'parking', label: 'Parking', hint: 'Garage or assigned spot'},
  {id: 'elevator', label: 'Elevator', hint: 'Accessible upper floors'},
  {id: 'furnished', label: 'Furnished', hint: 'Move-in ready'},
  {id: 'pets', label: 'Pets allowed', hint: 'Animals welcome'},
  {id: 'new_building', label: 'New building', hint: 'Built since 2010'},
];

const COMPLETION_OPTIONS: Array<{ id: 'ready' | 'not_ready'; label: string }> =
    [
      {id: 'ready', label: 'Ready to move in'},
      {id: 'not_ready', label: 'Not yet completed'},
    ];

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
      const n = v === '' ? undefined : parseInt(v, 10);
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
  applyAdvanced({...draft});
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
      title="Refine your search"
      @update:open="(v) => emit('update:open', v)"
  >
    <div class="space-y-8">
      <section>
        <p class="micro-label mb-2">Area (m²)</p>
        <div class="grid grid-cols-2 gap-2">
          <input
              v-model="m2Min.value"
              type="number"
              min="0"
              placeholder="Min"
              class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
              v-model="m2Max.value"
              type="number"
              min="0"
              placeholder="Max"
              class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">Floor</p>
        <div class="grid grid-cols-2 gap-2 mb-2">
          <input
              v-model="floorMin.value"
              type="number"
              min="0"
              placeholder="Min"
              class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
              v-model="floorMax.value"
              type="number"
              min="0"
              placeholder="Max"
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
            Not ground floor
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
            Not top floor
          </label>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">Year built</p>
        <div class="grid grid-cols-2 gap-2">
          <input
              v-model="yearMin.value"
              type="number"
              min="1800"
              max="2030"
              placeholder="From"
              class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
          <input
              v-model="yearMax.value"
              type="number"
              min="1800"
              max="2030"
              placeholder="To"
              class="focus-ring h-11 px-3 rounded-md border border-line text-sm tabular"
          />
        </div>
      </section>

      <section>
        <p class="micro-label mb-3">Features</p>
        <div class="grid grid-cols-1 gap-1.5">
          <label
              v-for="opt in FEATURE_OPTIONS"
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

      <section v-if="state.type === 'new_project'">
        <p class="micro-label mb-3">Completion</p>
        <div class="flex flex-wrap gap-2">
          <button
              v-for="opt in COMPLETION_OPTIONS"
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
          Reset advanced
        </button>
        <button
            type="button"
            @click="apply"
            class="focus-ring inline-flex items-center justify-center gap-2 h-11 px-5 rounded-md bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors min-w-44"
        >
          <span>Apply</span>
        </button>
      </div>
    </template>
  </Drawer>
</template>
