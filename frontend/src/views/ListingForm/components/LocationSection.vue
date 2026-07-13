<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import IconChevron from '../../../components/icons/IconChevron.vue';
import LocationPopover from '../../../components/listing/LocationPopover.vue';
import LocationMap from '../../../components/listing/LocationMap.vue';
import FormField from '../../../components/ui/FormField.vue';
import AutocompleteCombobox from '../../../components/ui/AutocompleteCombobox.vue';
import {
  searchStreets,
  searchBuildings,
  type StreetOption,
  type BuildingOption,
} from '../../../api/addressApi';
import { composeAddress, registerCoords } from '../composables/formHelpers';
import type { PropertyFieldsForm } from '../composables/formTypes';
import type { Location } from '../../../data/rawLocations';
import { logger } from '../../../utils/logger';

const { t } = useI18n();

const form = defineModel<PropertyFieldsForm>('form', { required: true });
const props = defineProps<{
  fieldError: (field: string) => string | undefined;
  districtName: string;
  selectedLocation: Location | null;
  isOpen: boolean;
}>();

defineEmits<{
  (e: 'update:isOpen', value: boolean): void;
  (e: 'select', value: Location[]): void;
}>();

// ── strict street/house pickers over the State Address Register ──

const streetOptions = ref<StreetOption[]>([]);
const streetLoading = ref(false);
const buildingOptions = ref<BuildingOption[]>([]);
const buildingLoading = ref(false);
let streetAbort: AbortController | undefined;
let buildingAbort: AbortController | undefined;

async function onStreetSearch(q: string) {
  const loc = props.selectedLocation;
  if (!loc) return;
  streetAbort?.abort();
  streetAbort = new AbortController();
  streetLoading.value = true;
  try {
    streetOptions.value = await searchStreets(loc.city, loc.district, q, {
      signal: streetAbort.signal,
    });
    streetLoading.value = false;
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return;
    logger.error('street search failed', e);
    streetOptions.value = [];
    streetLoading.value = false;
  }
}

async function onBuildingSearch(q: string) {
  const street = form.value.street;
  if (!street || street.kind !== 'street') return;
  buildingAbort?.abort();
  buildingAbort = new AbortController();
  buildingLoading.value = true;
  try {
    buildingOptions.value = await searchBuildings(street.code, q, {
      signal: buildingAbort.signal,
    });
    buildingLoading.value = false;
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') return;
    logger.error('building search failed', e);
    buildingOptions.value = [];
    buildingLoading.value = false;
  }
}

// Show the source territory only when it disambiguates (grouped cities).
function streetSecondary(option: StreetOption): string {
  const territories = new Set(streetOptions.value.map((o) => o.territory));
  return territories.size > 1 ? option.territory : '';
}

// District change invalidates the picks; street change invalidates the house.
watch(
  () => props.selectedLocation,
  (next, prev) => {
    if (next?.city === prev?.city && next?.district === prev?.district) return;
    form.value.street = null;
    form.value.building = null;
    streetOptions.value = [];
    buildingOptions.value = [];
  }
);

watch(
  () => form.value.street,
  (next, prev) => {
    if (next?.code === prev?.code && next?.kind === prev?.kind) return;
    form.value.building = null;
    buildingOptions.value = [];
  }
);

// Keep the composed display address and the register-pinned coords in sync
// with the picks. The pin stays user-draggable for fine placement.
watch(
  () => [form.value.street, form.value.building, form.value.apartment] as const,
  ([street, building], prev) => {
    form.value.address = composeAddress(form.value);
    // No prev means the initial run (draft restore) — keep any restored pin.
    if (!prev) return;
    if (street?.code !== prev[0]?.code || building?.code !== prev[1]?.code)
      form.value.coords = registerCoords(form.value);
  },
  { immediate: true }
);
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.locationSection') }}
    </h2>

    <div class="flex flex-col gap-1.5 relative">
      <label class="text-sm font-medium text-ink" for="ap-district-toggle"
        >{{ t('addListing.districtLabel') }}
        <span class="text-warn">*</span></label
      >
      <button
        id="ap-district-toggle"
        type="button"
        class="h-10 px-3 rounded-lg border text-sm text-ink bg-bg flex items-center justify-between focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-all text-left"
        :class="
          fieldError('district') ? 'border-warn/40 bg-warn/5' : 'border-line-2'
        "
        @click="$emit('update:isOpen', !isOpen)"
      >
        <span v-if="selectedLocation" class="text-ink font-medium">{{
          districtName
        }}</span>
        <span v-else class="text-ink-3">{{
          t('addListing.selectDistrict')
        }}</span>
        <span class="size-4 text-ink-2">
          <IconChevron :dir="isOpen ? 'up' : 'down'" />
        </span>
      </button>

      <div
        v-if="isOpen"
        class="fixed inset-0 z-40"
        @click="$emit('update:isOpen', false)"
      />

      <div
        v-if="isOpen"
        class="absolute top-[calc(100%+4px)] left-0 z-50 w-full bg-bg border border-line rounded-lg shadow-lift p-3"
      >
        <LocationPopover
          :model-value="selectedLocation ? [selectedLocation] : []"
          :multiple="false"
          @update:model-value="$emit('select', $event)"
        />
      </div>

      <p v-if="fieldError('district')" class="text-xs text-warn mt-1">
        {{ fieldError('district') }}
      </p>
    </div>

    <AutocompleteCombobox
      id="ap-street"
      v-model="form.street"
      :options="streetOptions"
      :label="t('addListing.streetLabel')"
      :placeholder="
        selectedLocation
          ? t('addListing.streetPlaceholder')
          : t('addListing.streetNeedsDistrict')
      "
      :error="fieldError('street')"
      :no-results-text="t('addListing.addressNoResults')"
      :disabled="!selectedLocation"
      :loading="streetLoading"
      :secondary="streetSecondary"
      required
      @search="onStreetSearch"
    />

    <div
      v-if="form.street"
      class="grid gap-4"
      :class="form.street.kind === 'street' ? 'grid-cols-2' : 'grid-cols-1'"
    >
      <AutocompleteCombobox
        v-if="form.street.kind === 'street'"
        id="ap-building"
        v-model="form.building"
        :options="buildingOptions"
        :label="t('addListing.houseLabel')"
        :placeholder="t('addListing.housePlaceholder')"
        :error="fieldError('building')"
        :no-results-text="t('addListing.addressNoResults')"
        :loading="buildingLoading"
        required
        @search="onBuildingSearch"
      />
      <FormField
        id="ap-apartment"
        v-model="form.apartment"
        :label="t('addListing.apartmentLabel')"
        :placeholder="t('addListing.apartmentPlaceholder')"
      />
    </div>

    <LocationMap
      v-model="form.coords"
      :geocode="false"
      :address="form.address"
      :district="districtName"
      :city="selectedLocation?.city ?? ''"
    />
  </section>
</template>
