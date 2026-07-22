<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { storeToRefs } from 'pinia';
import CategoryTabs from './CategoryTabs.vue';
import TransactionTabs from './TransactionTabs.vue';
import FilterPill from './FilterPill.vue';
import LocationPopover from '../../../components/listing/LocationPopover.vue';
import PricePopover from './popovers/PricePopover.vue';
import RoomsPopover from './popovers/RoomsPopover.vue';
import RangePopover from './popovers/RangePopover.vue';
import IconSearch from '../../../components/icons/IconSearch.vue';
import IconSliders from '../../../components/icons/IconSliders.vue';
import IconRefresh from '../../../components/icons/IconRefresh.vue';
import { useFiltersStore } from '../../../stores/filterStore';
import { useLocaleRoute } from '../../../composables/useLocaleRoute';
import { useKindCounts } from '../../../composables/useKindCounts';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { categoryProfile } from '../../../types/categoryRegistry';
import { groupFmt } from '../../../utils/format';
import { roomCountLabel } from '../../../types/filter';

const { t } = useI18n();
const { locale } = useLocaleRoute();
const { commercialTypeOptions, landUseOptions } = usePropertyLabels();

const emit = defineEmits<{ search: []; openMore: [] }>();

const locationPopoverRef = ref<InstanceType<typeof LocationPopover> | null>(
  null
);

const store = useFiltersStore();
const { locations } = storeToRefs(store);
const {
  state,
  setType,
  setKind,
  setLocations,
  setPriceRange,
  setVatIncluded,
  setRooms,
  setM2Range,
  setLandRange,
  setFloorRange,
  setNotGround,
  setNotTop,
  resetAll,
} = useFiltersStore();

const M2 = 'm²';

const areaPresets: Array<[number | undefined, number | undefined, string]> = [
  [undefined, 50, `≤ 50 ${M2}`],
  [50, 80, `50–80 ${M2}`],
  [80, 120, `80–120 ${M2}`],
  [120, undefined, `120+ ${M2}`],
];

const landPresets: Array<[number | undefined, number | undefined, string]> = [
  [undefined, 600, `≤ 600 ${M2}`],
  [600, 1200, `600–1200 ${M2}`],
  [1200, undefined, `1200+ ${M2}`],
];

const { counts: kindCounts } = useKindCounts(() => state.type);

// Which conditional pills/selects apply to the selected category.
const profile = computed(() =>
  state.kind ? categoryProfile(state.kind) : undefined
);

function setCommercialSubtype(v: string) {
  state.commercialSubtype = (v || undefined) as typeof state.commercialSubtype;
}
function setLandUse(v: string) {
  state.landUse = (v || undefined) as typeof state.landUse;
}

const locSummary = computed(() => {
  if (!locations.value.length) return '';

  const byCity = new Map<string, string[]>();
  for (const { city, district } of locations.value) {
    byCity.set(city, [...(byCity.get(city) ?? []), district]);
  }

  // One city: district names are unambiguous. Several cities (or too many
  // districts to list): show city names with counts instead.
  if (byCity.size === 1) {
    const [[city, ds]] = byCity;
    return ds.length <= 3 ? ds.join(', ') : `${city} (${ds.length})`;
  }
  return [...byCity].map(([city, ds]) => `${city} (${ds.length})`).join(', ');
});

const priceSummary = computed(() => {
  const { priceMin, priceMax, type } = state;
  const suffix = type === 'rent' ? t('filters.perMonth') : '';
  const fmt = (n: number) => groupFmt.format(n);
  let base = '';
  if (priceMin !== undefined && priceMax !== undefined)
    base =
      t('filters.priceRange', {
        min: `€${fmt(priceMin)}`,
        max: `€${fmt(priceMax)}`,
      }) + (suffix ? ' ' + suffix : '');
  else if (priceMin !== undefined)
    base =
      t('filters.priceFrom', { amount: `€${fmt(priceMin)}` }) +
      (suffix ? ' ' + suffix : '');
  else if (priceMax !== undefined)
    base =
      t('filters.priceUpTo', { amount: `€${fmt(priceMax)}` }) +
      (suffix ? ' ' + suffix : '');

  if (state.vatIncluded)
    return base ? `${base} · ${t('filters.vatShort')}` : t('filters.vatShort');
  return base;
});

const roomsSummary = computed(() => {
  if (!state.rooms.length) return '';
  const sorted = [...state.rooms].sort((a, b) => a - b);
  return (
    sorted.map(roomCountLabel).join(', ') +
    ' ' +
    t('filters.rm')
  );
});

function rangeSummary(
  min: number | undefined,
  max: number | undefined,
  unit?: string
) {
  const u = unit ? ` ${unit}` : '';
  const fmt = (n: number) => groupFmt.format(n);
  if (min !== undefined && max !== undefined)
    return t('filters.priceRange', { min: fmt(min), max: `${fmt(max)}${u}` });
  if (min !== undefined)
    return t('filters.rangeFrom', { value: `${fmt(min)}${u}` });
  if (max !== undefined)
    return t('filters.rangeUpTo', { value: `${fmt(max)}${u}` });
  return '';
}

function clearPrice() {
  setPriceRange(undefined, undefined);
  setVatIncluded(false);
}

const areaSummary = computed(() => rangeSummary(state.m2Min, state.m2Max, M2));
const landSummary = computed(() =>
  rangeSummary(state.landM2Min, state.landM2Max, M2)
);
const floorSummary = computed(() => {
  const parts: string[] = [];
  const range = rangeSummary(state.floorMin, state.floorMax);
  if (range) parts.push(range);
  if (state.notGround) parts.push(t('advFilters.notGround'));
  if (state.notTop) parts.push(t('advFilters.notTop'));
  return parts.join(' · ');
});

function clearFloor() {
  setFloorRange(undefined, undefined);
  setNotGround(false);
  setNotTop(false);
}

const advancedCount = computed(() => {
  let n = 0;
  if (state.notGround) n++;
  if (state.notTop) n++;
  if (state.yearMin !== undefined || state.yearMax !== undefined) n++;
  if (state.maintenanceCostMax !== undefined) n++;
  if (state.bathroomLayout) n++;
  n += state.features.length;
  n += state.roof.length;
  n += state.ventilationSystems.length;
  n += state.communications.length;
  n += state.stove.length;
  n += state.security.length;
  n += state.extras.length;
  n += state.parking.length;
  if (state.completion) n++;
  return n;
});
</script>

<template>
  <div
    class="bg-bg/97 backdrop-blur-md rounded-2xl shadow-lift border border-line/60 overflow-hidden"
  >
    <CategoryTabs
      :model-value="state.kind"
      :counts="kindCounts"
      @update:model-value="setKind"
    />

    <TransactionTabs :model-value="state.type" @update:model-value="setType" />

    <div class="p-4 sm:p-5 space-y-3">
      <!-- Category sub-type selectors -->
      <div
        v-if="profile && (profile.subtype === 'commercial' || profile.subtype === 'landUse')"
        class="flex flex-col sm:flex-row gap-2"
      >
        <select
          v-if="profile.subtype === 'commercial'"
          :value="state.commercialSubtype ?? ''"
          @change="setCommercialSubtype(($event.target as HTMLSelectElement).value)"
          class="focus-ring sm:flex-1 h-13 px-4 rounded-lg border border-line-2 bg-bg text-sm text-ink"
        >
          <option value="">{{ t('filters.allCommercialTypes') }}</option>
          <option v-for="o in commercialTypeOptions" :key="o.id" :value="o.id">
            {{ o.label }}
          </option>
        </select>

        <select
          v-if="profile.subtype === 'landUse'"
          :value="state.landUse ?? ''"
          @change="setLandUse(($event.target as HTMLSelectElement).value)"
          class="focus-ring sm:flex-1 h-13 px-4 rounded-lg border border-line-2 bg-bg text-sm text-ink"
        >
          <option value="">{{ t('filters.allLandUses') }}</option>
          <option v-for="o in landUseOptions" :key="o.id" :value="o.id">
            {{ o.label }}
          </option>
        </select>
      </div>

      <div class="flex flex-col sm:flex-row gap-2">
        <FilterPill
          class="sm:flex-1"
          :label="t('filters.location')"
          :summary="locSummary"
          :active="state.loc.length > 0"
          :width="560"
          @clear="locationPopoverRef?.clear()"
        >
          <LocationPopover
            ref="locationPopoverRef"
            :model-value="locations"
            @update:model-value="setLocations"
          />
        </FilterPill>

        <FilterPill
          class="sm:flex-1"
          :label="t('filters.price')"
          :summary="priceSummary"
          :active="
            state.priceMin !== undefined ||
            state.priceMax !== undefined ||
            !!state.vatIncluded
          "
          @clear="clearPrice"
        >
          <PricePopover
            :min="state.priceMin"
            :max="state.priceMax"
            :type="state.type"
            :vat-included="state.vatIncluded"
            @update:range="(mn, mx) => setPriceRange(mn, mx)"
            @update:vat-included="setVatIncluded"
          />
        </FilterPill>

        <FilterPill
          v-if="!profile || profile.rooms !== 'hidden'"
          class="sm:flex-1"
          :label="t('filters.rooms')"
          :summary="roomsSummary"
          :active="state.rooms.length > 0"
          @clear="setRooms([])"
        >
          <RoomsPopover
            :model-value="state.rooms"
            @update:model-value="setRooms"
          />
        </FilterPill>
      </div>

      <div class="flex flex-col sm:flex-row gap-2">
        <FilterPill
          v-if="!profile || profile.buildingArea !== 'hidden'"
          class="sm:flex-1"
          :label="t('filters.area')"
          :summary="areaSummary"
          :active="state.m2Min !== undefined || state.m2Max !== undefined"
          @clear="setM2Range(undefined, undefined)"
        >
          <RangePopover
            :min="state.m2Min"
            :max="state.m2Max"
            :unit="M2"
            :presets="areaPresets"
            @update:range="(mn, mx) => setM2Range(mn, mx)"
          />
        </FilterPill>

        <FilterPill
          v-if="profile && profile.plotArea !== 'hidden'"
          class="sm:flex-1"
          :label="t('filters.land')"
          :summary="landSummary"
          :active="state.landM2Min !== undefined || state.landM2Max !== undefined"
          @clear="setLandRange(undefined, undefined)"
        >
          <RangePopover
            :min="state.landM2Min"
            :max="state.landM2Max"
            :unit="M2"
            :presets="landPresets"
            @update:range="(mn, mx) => setLandRange(mn, mx)"
          />
        </FilterPill>

        <FilterPill
          v-if="!profile || profile.floors"
          class="sm:flex-1"
          :label="t('filters.floor')"
          :summary="floorSummary"
          :active="
            state.floorMin !== undefined ||
            state.floorMax !== undefined ||
            !!state.notGround ||
            !!state.notTop
          "
          @clear="clearFloor"
        >
          <RangePopover
            :min="state.floorMin"
            :max="state.floorMax"
            @update:range="(mn, mx) => setFloorRange(mn, mx)"
          >
            <template #footer>
              <div class="flex flex-col gap-2">
                <label
                  class="flex items-center gap-2 text-sm text-ink-2 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    :checked="!!state.notGround"
                    @change="
                      setNotGround(($event.target as HTMLInputElement).checked)
                    "
                    class="accent-ink size-4"
                  />
                  {{ t('advFilters.notGround') }}
                </label>
                <label
                  class="flex items-center gap-2 text-sm text-ink-2 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    :checked="!!state.notTop"
                    @change="
                      setNotTop(($event.target as HTMLInputElement).checked)
                    "
                    class="accent-ink size-4"
                  />
                  {{ t('advFilters.notTop') }}
                </label>
              </div>
            </template>
          </RangePopover>
        </FilterPill>
      </div>

      <div
        class="flex flex-col-reverse sm:flex-row sm:items-center gap-2 pt-3 border-t border-line/60"
      >
        <button
          type="button"
          @click="resetAll"
          class="focus-ring inline-flex items-center justify-center gap-2 h-13 px-4 rounded-lg border border-line-2 bg-bg text-sm text-ink-2 hover:text-ink hover:border-ink-3 transition-colors"
        >
          <span class="size-4 inline-block"><IconRefresh /></span>
          <span>{{ t('filters.reset') }}</span>
        </button>

        <button
          type="button"
          @click="emit('openMore')"
          class="focus-ring inline-flex items-center justify-center gap-2 h-13 px-5 sm:flex-1 rounded-lg border border-accent-2/40 bg-accent-2/10 text-sm font-medium text-accent-2 hover:bg-accent-2/16 hover:border-accent-2/60 transition-colors"
        >
          <span class="size-4 inline-block"><IconSliders /></span>
          <span>{{ t('filters.moreFilters') }}</span>
          <span
            v-if="advancedCount > 0"
            class="tabular text-[0.6875rem] px-1.5 h-5 inline-flex items-center rounded-full bg-accent-2 text-cream"
          >
            {{ advancedCount }}
          </span>
        </button>

        <button
          type="button"
          @click="emit('search')"
          class="focus-ring inline-flex items-center justify-center gap-2 h-13 px-8 sm:flex-[1.7] rounded-lg bg-ink text-bg text-sm font-medium tracking-wide hover:bg-accent-2 transition-colors"
        >
          <span class="size-4 inline-block"><IconSearch /></span>
          <span>{{ t('filters.search') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
