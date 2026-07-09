<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import FormField from '../../../../components/ui/FormField.vue';
import LocationMap from '../../../../components/listing/LocationMap.vue';

const { t } = useI18n();

const props = defineProps<{
  address: string;
  districtName: string;
  city: string;
  district: string;
  coords: { lat: number; lng: number } | null;
  affectedListingsCount: number;
  saving: boolean;
  saveError: string;
}>();

const emit = defineEmits<{
  (e: 'update:coords', value: { lat: number; lng: number } | null): void;
  (e: 'save-pin'): void;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.locationSection') }}
    </h2>

    <FormField
      id="edit-district"
      :label="t('addListing.districtLabel')"
      :model-value="districtName"
      disabled
    />
    <FormField
      id="edit-address"
      :label="t('addListing.streetAddress')"
      :model-value="address"
      disabled
    />
    <p class="text-xs text-ink-3 -mt-2">
      {{ t('addListing.locationLockedNote') }}
    </p>

    <LocationMap
      :model-value="coords"
      :address="address"
      :district="district"
      :city="city"
      @update:model-value="emit('update:coords', $event)"
    />

    <div class="flex flex-col gap-2">
      <p v-if="affectedListingsCount > 1" class="text-xs text-ink-3">
        {{
          t('addListing.pinAffectsListings', { count: affectedListingsCount })
        }}
      </p>
      <p v-else class="text-xs text-ink-3">
        {{ t('addListing.pinAffectsListingsOne') }}
      </p>
      <button
        type="button"
        :disabled="saving"
        class="self-start h-9 px-4 rounded-full text-sm font-medium border border-line text-ink-2 hover:border-ink/40 hover:text-ink transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        @click="emit('save-pin')"
      >
        {{ saving ? t('addListing.pinSaving') : t('addListing.pinSaveAction') }}
      </button>
      <p v-if="saveError" class="text-xs text-red-500">{{ saveError }}</p>
    </div>
  </section>
</template>
