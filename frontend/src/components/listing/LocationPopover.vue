<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import IconChevron from '../icons/IconChevron.vue';
import AutocompleteCombobox from '../ui/AutocompleteCombobox.vue';
import { districtSlugByName, slugify } from '../../data/locations';
import { Location, LOCATION_MAP } from '../../data/rawLocations';
import { searchStreets, type StreetOption } from '../../api/addressApi';
import type { StreetFilter } from '../../types/filter';
import { logger } from '../../utils/logger';

const { t } = useI18n();

const props = withDefaults(
  defineProps<{
    modelValue: Location[];
    multiple?: boolean;
    // Search-only: adds a street typeahead, enabled once exactly one district is
    // picked (the register street search needs a single resolved city+district).
    enableStreet?: boolean;
    streets?: StreetFilter[];
  }>(),
  {
    multiple: true,
    enableStreet: false,
    streets: () => [],
  }
);
const emit = defineEmits<{
  'update:modelValue': [value: Location[]];
  'update:streets': [value: StreetFilter[]];
}>();

const query = ref('');

// Local, synchronously-updated copy of the selection. Toggling reads/writes
// this instead of props.modelValue, whose value only catches up once the
// parent re-renders — reading the prop directly let rapid clicks across
// different checkboxes each compute from the same stale snapshot and
// clobber one another's toggle.
const selected = ref<Location[]>([...props.modelValue]);

watch(
  () => props.modelValue,
  (v) => {
    selected.value = [...v];
  }
);

// Cities with existing selections start unwrapped so they're visible.
const expanded = ref(new Set(props.modelValue.map((l) => l.city)));

function toggleCity(cityName: string) {
  if (expanded.value.has(cityName)) {
    expanded.value.delete(cityName);
  } else {
    expanded.value.add(cityName);
  }
}

function isExpanded(cityName: string): boolean {
  return expanded.value.has(cityName);
}

// Searching auto-unwraps the matching cities (collapsed rows would hide
// results), but they stay individually collapsible.
watch(query, () => {
  expanded.value = query.value.trim()
    ? new Set(grouped.value.keys())
    : new Set(selected.value.map((l) => l.city));
});

function selectedCount(cityName: string): number {
  return selected.value.filter((l) => l.city === cityName).length;
}

const grouped = computed(() => {
  const normalizedQuery = slugify(query.value);
  if (!normalizedQuery) return LOCATION_MAP;

  const filteredMap = new Map<string, readonly string[]>();
  for (const [cityName, districts] of LOCATION_MAP) {
    if (slugify(cityName).includes(normalizedQuery)) {
      filteredMap.set(cityName, districts);
      continue;
    }

    const matchingDistricts = districts.filter((districtName) =>
      districtSlugByName.get(districtName)?.includes(normalizedQuery)
    );

    if (matchingDistricts.length > 0) {
      filteredMap.set(cityName, matchingDistricts);
    }
  }

  return filteredMap;
});

function isSelected(cityName: string, districtName: string): boolean {
  return selected.value.some(
    (l) => l.city === cityName && l.district === districtName
  );
}

function handleSelect(cityName: string, districtName: string) {
  const index = selected.value.findIndex(
    (l) => l.city === cityName && l.district === districtName
  );

  if (props.multiple) {
    if (index > -1) {
      selected.value.splice(index, 1);
    } else {
      selected.value.push({ city: cityName, district: districtName });
    }
  } else {
    selected.value = [{ city: cityName, district: districtName }];
  }
  emit('update:modelValue', [...selected.value]);
}

// ── street typeahead (search only) ──
// The register street search needs a single resolved city+district, so the
// picker is live only when exactly one district is selected.
const singleLocation = computed<Location | null>(() =>
  selected.value.length === 1 ? selected.value[0] : null
);

const streetOptions = ref<StreetOption[]>([]);
const streetLoading = ref(false);
let streetAbort: AbortController | undefined;

// The current street pick shaped for the combobox (it only reads `name`).
const streetModel = computed<StreetOption | null>(() => {
  const s = props.streets[0];
  return s ? { kind: 'street', code: s.code, name: s.name, territory: '' } : null;
});

async function onStreetSearch(q: string) {
  const loc = singleLocation.value;
  if (!loc) return;
  streetAbort?.abort();
  streetAbort = new AbortController();
  streetLoading.value = true;
  try {
    const results = await searchStreets(loc.city, loc.district, q, {
      signal: streetAbort.signal,
    });
    // Rural houses (kind 'house') have no street — drop them from a street filter.
    streetOptions.value = results.filter((o) => o.kind === 'street');
    streetLoading.value = false;
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return;
    logger.error('street search failed', e);
    streetOptions.value = [];
    streetLoading.value = false;
  }
}

function onStreetSelect(option: StreetOption | null) {
  emit(
    'update:streets',
    option ? [{ code: option.code, name: option.name }] : []
  );
}

// Show the source territory only when it disambiguates (grouped cities).
function streetSecondary(option: StreetOption): string {
  const territories = new Set(streetOptions.value.map((o) => o.territory));
  return territories.size > 1 ? option.territory : '';
}

// Changing (or losing) the single district invalidates the street pick.
watch(singleLocation, (next, prev) => {
  if (next?.city === prev?.city && next?.district === prev?.district) return;
  streetOptions.value = [];
  if (props.streets.length) emit('update:streets', []);
});

function clear() {
  selected.value = [];
  emit('update:modelValue', []);
  emit('update:streets', []);
  streetOptions.value = [];
  query.value = '';
}

defineExpose({ clear });
</script>

<template>
  <div class="space-y-4 min-w-70">
    <input
      id="location-popover-search"
      v-model="query"
      type="search"
      :placeholder="t('filters.findDistrict')"
      :aria-label="t('filters.findDistrict')"
      class="focus-ring w-full h-11 px-3.5 rounded-md border border-line-2 bg-bg text-sm placeholder:text-ink-3"
    />
    <div class="space-y-0.5 max-h-[min(38vh,17rem)] overflow-y-auto pr-2.5">
      <div v-for="[cityName, districts] in grouped" :key="cityName">
        <button
          type="button"
          class="focus-ring w-full flex items-center gap-2 px-2 py-2 rounded-md select-none hover:bg-surface transition-colors"
          @click="toggleCity(cityName)"
        >
          <span class="text-sm font-medium text-ink">{{ cityName }}</span>
          <span
            v-if="selectedCount(cityName) > 0"
            class="tabular text-[0.6875rem] px-1.5 h-4.5 inline-flex items-center rounded-full bg-ink text-cream"
          >
            {{ selectedCount(cityName) }}
          </span>
          <span
            class="size-3.5 ml-auto shrink-0"
            :class="isExpanded(cityName) ? 'text-ink' : 'text-ink-3'"
          >
            <IconChevron :dir="isExpanded(cityName) ? 'up' : 'down'" />
          </span>
        </button>
        <div
          v-if="isExpanded(cityName)"
          class="grid grid-cols-2 gap-1 ml-3.5 pl-2.5 border-l border-line mt-0.5 mb-2"
        >
          <template v-if="multiple">
            <label
              v-for="d in districts"
              :key="d"
              class="focus-ring flex items-center gap-2 px-2 py-1.5 rounded-md text-sm text-ink cursor-pointer hover:bg-surface select-none"
            >
              <input
                type="checkbox"
                class="accent-ink size-4"
                :checked="isSelected(cityName, d)"
                @change="handleSelect(cityName, d)"
              />
              <span>{{ d }}</span>
            </label>
          </template>

          <template v-else>
            <button
              v-for="d in districts"
              :key="d"
              type="button"
              class="focus-ring flex items-center justify-between px-2.5 py-1.5 rounded-md text-sm text-left transition-colors group select-none"
              :class="
                isSelected(cityName, d)
                  ? 'bg-surface text-ink font-semibold'
                  : 'text-ink hover:bg-surface/70'
              "
              @click="handleSelect(cityName, d)"
            >
              <span>{{ d }}</span>
              <span
                v-if="isSelected(cityName, d)"
                class="text-ink text-xs font-semibold"
              >
                ✓
              </span>
            </button>
          </template>
        </div>
      </div>
      <p v-if="grouped.size === 0" class="text-sm text-ink-3 py-2">
        {{ t('filters.nothingMatches', { query }) }}
      </p>
    </div>

    <div v-if="enableStreet" class="pt-3 border-t border-line">
      <AutocompleteCombobox
        id="search-street"
        :model-value="streetModel"
        :options="streetOptions"
        :label="t('filters.street')"
        :placeholder="
          singleLocation
            ? t('filters.streetPlaceholder')
            : t('filters.streetNeedsDistrict')
        "
        :no-results-text="t('filters.streetNoResults')"
        :disabled="!singleLocation"
        :loading="streetLoading"
        :secondary="streetSecondary"
        @update:model-value="onStreetSelect"
        @search="onStreetSearch"
      />
    </div>

    <div v-if="multiple" class="pt-3 border-t border-line text-right">
      <p class="micro-label">
        {{ selected.length || t('filters.any') }} {{ t('filters.selected') }}
      </p>
    </div>
  </div>
</template>
