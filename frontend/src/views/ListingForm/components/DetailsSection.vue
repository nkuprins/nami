<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { numericInput, decimalInput } from '../../../utils/utils';
import FormField from '../../../components/ui/FormField.vue';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';

const { t } = useI18n();
const { bathroomLayoutOptions, heatingOptions, energyClassOptions } =
  usePropertyLabels();
import type { PropertyFieldsForm } from '../composables/formTypes';

const form = defineModel<PropertyFieldsForm>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.details') }}
    </h2>

    <div class="grid grid-cols-2 gap-4">
      <FormField
        id="ap-rooms"
        :label="t('addListing.roomsLabel')"
        v-model="form.rooms"
        :error="fieldError('rooms')"
        required
        inputmode="numeric"
        placeholder="e.g. 3"
        @beforeinput="numericInput"
      />

      <FormField
        id="ap-m2"
        :label="t('addListing.areaLabel')"
        v-model="form.m2"
        :error="fieldError('m2')"
        required
        inputmode="decimal"
        placeholder="e.g. 72.5"
        @beforeinput="decimalInput"
      />
    </div>

    <div class="grid grid-cols-2 gap-4">
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

    <div class="flex flex-col gap-1.5">
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
      v-if="form.propertyKind === 'house'"
      id="ap-land"
      :label="t('addListing.landAreaLabel')"
      v-model="form.landM2"
      inputmode="decimal"
      placeholder="e.g. 600"
      @beforeinput="decimalInput"
    />

    <div class="grid grid-cols-2 gap-4">
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
      id="ap-year"
      :label="t('addListing.yearBuiltLabel')"
      v-model="form.yearBuilt"
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
    </div>

    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.energyClassLabel') }}
      </p>
      <ToggleButtons
        :options="energyClassOptions"
        :model-value="form.energyClass ?? ''"
        @update:model-value="
          form.energyClass = $event as typeof form.energyClass
        "
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
