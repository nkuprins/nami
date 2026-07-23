<script setup lang="ts">
import { computed, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { numericInput, decimalInput } from '../../../utils/utils';
import FormField from '../../../components/ui/FormField.vue';
import {
  CADASTRE_OFFICIAL_KEY,
  type OfficialFigures,
} from '../composables/useCadastreAutofill';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { categoryProfile } from '../../../types/categoryRegistry';

const { t } = useI18n();
const {
  bathroomLayoutOptions,
  heatingOptions,
  energyClassOptions,
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
import type { PropertyFieldsForm } from '../composables/formTypes';

const form = defineModel<PropertyFieldsForm>('form', { required: true });

// Which physical fields the selected category carries (registry-driven).
const profile = computed(() => categoryProfile(form.value.propertyKind));

// Shows the explanation for the chosen heating type (e.g. central vs central gas
// vs gas) below the toggle — the desktop tooltip alone isn't visible on touch.
const selectedHeatingHint = computed(
  () => heatingOptions.value.find((o) => o.id === form.value.heating)?.hint
);
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();

// Official cadastre figures (from the listing-form view); absent → no hints.
const official = inject<import('vue').Ref<OfficialFigures>>(
  CADASTRE_OFFICIAL_KEY,
  ref({ yearBuilt: null, area: null, landM2: null, landUse: null })
);
const officialHint = (value: number | null, unit = '') =>
  value != null ? t('addListing.official', { value: `${value}${unit}` }) : undefined;
const areaHint = computed(() => officialHint(official.value.area, ' m²'));
const yearHint = computed(() => officialHint(official.value.yearBuilt));
const landAreaHint = computed(() => officialHint(official.value.landM2, ' m²'));
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.details') }}
    </h2>

    <div class="grid grid-cols-2 gap-4">
      <FormField
        v-if="profile.rooms !== 'hidden'"
        id="ap-rooms"
        :label="t('addListing.roomsLabel')"
        v-model="form.rooms"
        :error="fieldError('rooms')"
        :required="profile.rooms === 'required'"
        inputmode="numeric"
        placeholder="e.g. 3"
        @beforeinput="numericInput"
      />

      <FormField
        v-if="profile.buildingArea !== 'hidden'"
        id="ap-m2"
        :label="t('addListing.areaLabel')"
        v-model="form.m2"
        :error="fieldError('m2')"
        :hint="areaHint"
        :required="profile.buildingArea === 'required'"
        inputmode="decimal"
        placeholder="e.g. 72.5"
        @beforeinput="decimalInput"
      />
    </div>

    <div v-if="profile.rooms !== 'hidden'" class="grid grid-cols-2 gap-4">
      <FormField
        id="ap-bedrooms"
        :label="t('addListing.bedroomsLabel')"
        v-model="form.bedrooms"
        inputmode="numeric"
        placeholder="e.g. 2"
        @beforeinput="numericInput"
      />

      <FormField
        id="ap-bathrooms"
        :label="t('addListing.bathroomsLabel')"
        v-model="form.bathrooms"
        inputmode="numeric"
        placeholder="e.g. 1"
        @beforeinput="numericInput"
      />
    </div>

    <div v-if="profile.rooms !== 'hidden'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.bathroomLayoutLabel') }}
      </p>
      <ToggleButtons
        :options="bathroomLayoutOptions"
        :model-value="form.bathroomLayout ?? ''"
        @update:model-value="
          form.bathroomLayout = $event as typeof form.bathroomLayout
        "
      />
    </div>

    <FormField
      v-if="profile.plotArea !== 'hidden'"
      id="ap-land"
      :label="t('addListing.landAreaLabel')"
      v-model="form.landM2"
      :error="fieldError('landM2')"
      :hint="landAreaHint"
      :required="profile.plotArea === 'required'"
      inputmode="decimal"
      placeholder="e.g. 600"
      @beforeinput="decimalInput"
    />

    <div v-if="profile.floors" class="grid grid-cols-2 gap-4">
      <FormField
        id="ap-floor"
        :label="t('addListing.floorLabel')"
        v-model="form.floor"
        inputmode="numeric"
        placeholder="e.g. 4"
        @beforeinput="numericInput"
      />

      <FormField
        id="ap-total-floors"
        :label="t('addListing.totalFloorsLabel')"
        v-model="form.totalFloors"
        inputmode="numeric"
        placeholder="e.g. 9"
        @beforeinput="numericInput"
      />
    </div>

    <FormField
      v-if="profile.buildingArea !== 'hidden'"
      id="ap-year"
      :label="t('addListing.yearBuiltLabel')"
      v-model="form.yearBuilt"
      :hint="yearHint"
      inputmode="numeric"
      placeholder="e.g. 2018"
      @beforeinput="numericInput"
    />

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.heatingLabel') }}
      </p>
      <ToggleButtons
        :options="heatingOptions"
        :model-value="form.heating ?? ''"
        @update:model-value="form.heating = $event as typeof form.heating"
      />
      <p v-if="selectedHeatingHint" class="text-xs text-ink-3 leading-tight">
        {{ selectedHeatingHint }}
      </p>
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.energyClassLabel') }}
      </p>
      <div class="flex gap-2 flex-wrap">
        <button
          v-for="opt in energyClassOptions"
          :key="opt.id"
          type="button"
          :style="
            form.energyClass === opt.id
              ? {}
              : { backgroundColor: opt.color + '38', borderColor: opt.color }
          "
          class="focus-ring inline-flex items-center justify-center min-w-10 h-9 px-3 rounded-full border text-sm font-medium transition-colors"
          :class="
            form.energyClass === opt.id
              ? 'border-ink bg-ink text-bg'
              : 'text-ink'
          "
          @click="form.energyClass = form.energyClass === opt.id ? '' : opt.id"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.sewageLabel') }}
      </p>
      <ToggleButtons
        :options="sewageOptions"
        :model-value="form.sewage ?? ''"
        @update:model-value="form.sewage = $event as typeof form.sewage"
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.ventilationLabel') }}
      </p>
      <ToggleButtons
        :options="ventilationOptions"
        :model-value="form.ventilation ?? ''"
        @update:model-value="
          form.ventilation = $event as typeof form.ventilation
        "
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.roofLabel') }}
      </p>
      <ToggleButtons
        :options="roofOptions"
        :model-value="form.roof ?? ''"
        @update:model-value="form.roof = $event as typeof form.roof"
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.ventilationSystemsLabel') }}
      </p>
      <ToggleButtons
        :options="ventilationSystemOptions"
        :multiple="true"
        :model-value="form.ventilationSystems"
        @update:model-value="
          form.ventilationSystems = $event as typeof form.ventilationSystems
        "
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.communicationsLabel') }}
      </p>
      <ToggleButtons
        :options="communicationOptions"
        :multiple="true"
        :model-value="form.communications"
        @update:model-value="
          form.communications = $event as typeof form.communications
        "
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.stoveLabel') }}
      </p>
      <ToggleButtons
        :options="stoveOptions"
        :multiple="true"
        :model-value="form.stove"
        @update:model-value="form.stove = $event as typeof form.stove"
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.securityLabel') }}
      </p>
      <ToggleButtons
        :options="securityOptions"
        :multiple="true"
        :model-value="form.security"
        @update:model-value="form.security = $event as typeof form.security"
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.extrasLabel') }}
      </p>
      <ToggleButtons
        :options="extrasOptions"
        :multiple="true"
        :model-value="form.extras"
        @update:model-value="form.extras = $event as typeof form.extras"
      />
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.parkingLabel') }}
      </p>
      <ToggleButtons
        :options="parkingOptions"
        :multiple="true"
        :model-value="form.parking"
        @update:model-value="form.parking = $event as typeof form.parking"
      />
    </div>

    <FormField
      id="ap-maintenance"
      :label="t('addListing.maintenanceCostLabel')"
      v-model="form.maintenanceCost"
      inputmode="decimal"
      placeholder="e.g. 45"
      @beforeinput="decimalInput"
    />
  </section>
</template>
