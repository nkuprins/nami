<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import IconChevron from '../../../components/icons/IconChevron.vue';

const { t } = useI18n();
import LocationPopover from '../../../components/listing/LocationPopover.vue';
import LocationMap from '../../../components/listing/LocationMap.vue';
import FormField from '../../../components/ui/FormField.vue';
import type { PropertyFieldsForm } from '../composables/formTypes';
import type { Location } from '../../../data/rawLocations';

defineProps<{
  form: PropertyFieldsForm;
  fieldError: (field: string) => string | undefined;
  districtName: string;
  selectedLocation: Location | null;
  isOpen: boolean;
}>();

defineEmits<{
  (e: 'update:isOpen', value: boolean): void;
  (e: 'select', value: Location[]): void;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.locationSection') }}
    </h2>

    <div class="flex flex-col gap-1.5 relative">
      <label class="text-sm font-medium text-ink" for="ap-district-toggle"
        >{{ t('addListing.districtLabel') }}
        <span class="text-red-500">*</span></label
      >
      <button
        id="ap-district-toggle"
        type="button"
        class="h-10 px-3 rounded-lg border text-sm text-ink bg-bg flex items-center justify-between focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-all text-left"
        :class="
          fieldError('district') ? 'border-red-400 bg-red-50' : 'border-line'
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
        class="absolute top-[calc(100%+4px)] left-0 z-50 w-full bg-bg border border-line rounded-lg shadow-xl p-3"
      >
        <LocationPopover
          :model-value="selectedLocation ? [selectedLocation] : []"
          :multiple="false"
          @update:model-value="$emit('select', $event)"
        />
      </div>

      <p v-if="fieldError('district')" class="text-xs text-red-500 mt-1">
        {{ fieldError('district') }}
      </p>
    </div>

    <FormField
      id="ap-address"
      :label="t('addListing.streetAddress')"
      v-model="form.address"
      :error="fieldError('address')"
      required
      placeholder="e.g. Brīvības iela 12, apt. 4"
    />

    <LocationMap
      v-model="form.coords"
      :address="form.address"
      :district="districtName"
      :city="selectedLocation?.city ?? ''"
    />
  </section>
</template>
