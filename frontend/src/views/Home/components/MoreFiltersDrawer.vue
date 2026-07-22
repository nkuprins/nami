<script setup lang="ts">
import { reactive, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../../../components/ui/Drawer.vue';
import { useFiltersStore } from '../../../stores/filterStore';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import {
  ROOM_COUNT_OPTIONS,
  roomCountLabel,
  type FilterState,
} from '../../../types/filter';
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
  bathroomLayoutOptions,
  sewageOptions,
  ventilationOptions,
  roofOptions,
  ventilationSystemOptions,
  communicationOptions,
  stoveOptions,
  securityOptions,
  extrasOptions,
  parkingOptions,
} = usePropertyLabels();

type Draft = Pick<
  FilterState,
  | 'yearMin'
  | 'yearMax'
  | 'maintenanceCostMax'
  | 'bathroomLayout'
  | 'bedrooms'
  | 'bathrooms'
  | 'heating'
  | 'energyClass'
  | 'sewage'
  | 'ventilation'
  | 'roof'
  | 'features'
  | 'ventilationSystems'
  | 'communications'
  | 'stove'
  | 'security'
  | 'extras'
  | 'parking'
  | 'completion'
>;

const draft = reactive<Draft>(makeDraftFromState());

function makeDraftFromState(): Draft {
  return {
    yearMin: state.yearMin,
    yearMax: state.yearMax,
    maintenanceCostMax: state.maintenanceCostMax,
    bathroomLayout: state.bathroomLayout,
    bedrooms: [...state.bedrooms],
    bathrooms: [...state.bathrooms],
    heating: [...state.heating],
    energyClass: [...state.energyClass],
    sewage: [...state.sewage],
    ventilation: [...state.ventilation],
    roof: [...state.roof],
    features: [...state.features],
    ventilationSystems: [...state.ventilationSystems],
    communications: [...state.communications],
    stove: [...state.stove],
    security: [...state.security],
    extras: [...state.extras],
    parking: [...state.parking],
    completion: state.completion,
  };
}

// Generic multi-select toggle for the new attribute-set drafts; returns a new
// array so Vue reactivity fires on assignment (draft.<key> = toggleEnum(...)).
function toggleEnum<T>(list: T[], id: T): T[] {
  const set = new Set(list);
  if (set.has(id)) set.delete(id);
  else set.add(id);
  return [...set];
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

function toggleSewage(id: (typeof draft.sewage)[number]) {
  const set = new Set(draft.sewage);
  if (set.has(id)) set.delete(id);
  else set.add(id);
  draft.sewage = [...set];
}

function toggleVentilation(id: (typeof draft.ventilation)[number]) {
  const set = new Set(draft.ventilation);
  if (set.has(id)) set.delete(id);
  else set.add(id);
  draft.ventilation = [...set];
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

function bindNumber(key: 'yearMin' | 'yearMax' | 'maintenanceCostMax') {
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

const yearMin = bindNumber('yearMin');
const yearMax = bindNumber('yearMax');
const maintenanceCostMax = bindNumber('maintenanceCostMax');

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
    width="w-[56rem] max-w-[94vw]"
    @update:open="(v) => emit('update:open', v)"
  >
    <div
      class="space-y-6 md:space-y-0 md:grid md:grid-cols-3 md:gap-x-8 md:items-start"
    >
      <div class="space-y-6">
      <section>
        <p class="micro-label mb-2">{{ t('advFilters.bedrooms') }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="n in ROOM_COUNT_OPTIONS"
            :key="n"
            type="button"
            @click="toggleNumeric('bedrooms', n)"
            class="focus-ring inline-flex items-center justify-center min-w-11 h-10 px-3 rounded-md border text-sm transition-colors"
            :class="
              draft.bedrooms.includes(n)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ roomCountLabel(n) }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.bathrooms') }}</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="n in ROOM_COUNT_OPTIONS"
            :key="n"
            type="button"
            @click="toggleNumeric('bathrooms', n)"
            class="focus-ring inline-flex items-center justify-center min-w-11 h-10 px-3 rounded-md border text-sm transition-colors"
            :class="
              draft.bathrooms.includes(n)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ roomCountLabel(n) }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.bathroomLayout') }}</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in bathroomLayoutOptions"
            :key="opt.id"
            type="button"
            @click="
              draft.bathroomLayout =
                draft.bathroomLayout === opt.id ? undefined : opt.id
            "
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.bathroomLayout === opt.id
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
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
            class="focus-ring h-11 px-3 rounded-md border border-line-2 text-sm tabular"
          />
          <input
            v-model="yearMax.value"
            type="number"
            min="1800"
            max="2030"
            :placeholder="t('advFilters.to')"
            :aria-label="`${t('advFilters.yearBuilt')} ${t('advFilters.to')}`"
            class="focus-ring h-11 px-3 rounded-md border border-line-2 text-sm tabular"
          />
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">{{ t('advFilters.maintenanceCost') }}</p>
        <input
          v-model="maintenanceCostMax.value"
          type="number"
          min="0"
          :placeholder="t('advFilters.maintenanceCostMax')"
          :aria-label="t('advFilters.maintenanceCostMax')"
          class="focus-ring w-full h-11 px-3 rounded-md border border-line-2 text-sm tabular"
        />
      </section>
      </div>

      <div class="space-y-6">
      <section>
        <p class="micro-label mb-3">
          {{ t('advFilters.energyClass') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="opt in energyClassOptions"
            :key="opt.id"
            type="button"
            @click="toggleEnergyClass(opt.id)"
            :style="
              draft.energyClass.includes(opt.id)
                ? {}
                : { backgroundColor: opt.color + '38', borderColor: opt.color }
            "
            class="focus-ring inline-flex items-center justify-center min-w-10 h-10 px-3 rounded-md border text-sm font-medium transition-colors"
            :class="
              draft.energyClass.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'text-ink'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-3">
          {{ t('advFilters.heating') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="grid grid-cols-1 gap-1.5">
          <label
            v-for="opt in heatingOptions"
            :key="opt.id"
            class="focus-ring flex items-center gap-3 px-3 py-2.5 rounded-md border border-line-2 hover:border-ink-3 cursor-pointer transition-colors"
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
            <div>
              <p class="text-sm text-ink leading-tight">{{ opt.label }}</p>
              <p class="text-xs text-ink-3 leading-tight">{{ opt.hint }}</p>
            </div>
          </label>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.sewage') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in sewageOptions"
            :key="opt.id"
            type="button"
            @click="toggleSewage(opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.sewage.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.ventilation') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in ventilationOptions"
            :key="opt.id"
            type="button"
            @click="toggleVentilation(opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.ventilation.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section v-if="state.kind === 'new_project'">
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
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>
      </div>

      <div class="space-y-6">
      <section>
        <p class="micro-label mb-3">
          {{ t('advFilters.features') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAll') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in featureOptions"
            :key="opt.id"
            type="button"
            :title="opt.hint"
            @click="toggleFeature(opt.id)"
            class="focus-ring inline-flex items-center gap-2 h-9 pl-3 pr-4 rounded-full text-sm font-medium border transition-colors"
            :class="
              draft.features.includes(opt.id)
                ? 'bg-ink border-ink text-bg'
                : opt.category === 'comfort'
                  ? 'bg-accent/10 border-accent/25 text-accent-2 hover:border-accent/50'
                  : 'bg-surface border-line-2 text-ink-2 hover:border-ink-3'
            "
          >
            <span class="size-4.5 shrink-0"><component :is="opt.icon" /></span>
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.roof') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in roofOptions"
            :key="opt.id"
            type="button"
            @click="draft.roof = toggleEnum(draft.roof, opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.roof.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.ventilationSystems') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in ventilationSystemOptions"
            :key="opt.id"
            type="button"
            @click="
              draft.ventilationSystems = toggleEnum(
                draft.ventilationSystems,
                opt.id
              )
            "
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.ventilationSystems.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.communications') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in communicationOptions"
            :key="opt.id"
            type="button"
            @click="
              draft.communications = toggleEnum(draft.communications, opt.id)
            "
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.communications.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.stove') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in stoveOptions"
            :key="opt.id"
            type="button"
            @click="draft.stove = toggleEnum(draft.stove, opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.stove.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.security') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in securityOptions"
            :key="opt.id"
            type="button"
            @click="draft.security = toggleEnum(draft.security, opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.security.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.extras') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in extrasOptions"
            :key="opt.id"
            type="button"
            @click="draft.extras = toggleEnum(draft.extras, opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.extras.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>

      <section>
        <p class="micro-label mb-2">
          {{ t('advFilters.parking') }}
          <span class="ml-1 normal-case tracking-normal font-normal text-ink-3">· {{ t('advFilters.matchAny') }}</span>
        </p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="opt in parkingOptions"
            :key="opt.id"
            type="button"
            @click="draft.parking = toggleEnum(draft.parking, opt.id)"
            class="focus-ring px-4 h-10 rounded-md border text-sm transition-colors"
            :class="
              draft.parking.includes(opt.id)
                ? 'border-ink bg-ink text-bg'
                : 'border-line-2 text-ink hover:border-ink-3'
            "
          >
            {{ opt.label }}
          </button>
        </div>
      </section>
      </div>
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
          class="focus-ring inline-flex items-center justify-center gap-2 h-11 px-5 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors min-w-44"
        >
          <span>{{ t('advFilters.apply') }}</span>
        </button>
      </div>
    </template>
  </Drawer>
</template>
