<script setup lang="ts">
import { computed, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import FormField from '../../../components/ui/FormField.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { categoryProfile } from '../../../types/categoryRegistry';
import type { PropertyFieldsForm } from '../composables/formTypes';
import {
  CADASTRE_OFFICIAL_KEY,
  type OfficialFigures,
} from '../composables/useCadastreAutofill';

const { t } = useI18n();
const { landUseOptions } = usePropertyLabels();

const form = defineModel<PropertyFieldsForm>('form', { required: true });

// Which physical fields the selected category carries (registry-driven).
// `parcel` = land/commercial, where a manual parcel number is the only way to
// identify the plot; building categories are resolved from the address.
const profile = computed(() => categoryProfile(form.value.propertyKind));

// Official cadastre figures (from the listing-form view); absent → panel is empty.
const official = inject<import('vue').Ref<OfficialFigures>>(
  CADASTRE_OFFICIAL_KEY,
  ref({ yearBuilt: null, area: null, landM2: null, landUse: null })
);

// Registry figures resolved so far, as label/value rows for the readout.
const figures = computed(() => {
  const o = official.value;
  const rows: { label: string; value: string }[] = [];
  if (o.yearBuilt != null)
    rows.push({ label: t('addListing.yearBuiltLabel'), value: String(o.yearBuilt) });
  if (o.area != null)
    rows.push({ label: t('addListing.areaLabel'), value: `${o.area} m²` });
  if (o.landM2 != null)
    rows.push({ label: t('addListing.landAreaLabel'), value: `${o.landM2} m²` });
  if (o.landUse != null) {
    const label =
      landUseOptions.value.find((op) => op.id === o.landUse)?.label ?? o.landUse;
    rows.push({ label: t('addListing.landUse'), value: label });
  }
  return rows;
});
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.cadastreSection') }}
    </h2>

    <!-- Land / commercial: no address-based building, so the parcel number is
         the only way to identify the plot in the register. -->
    <FormField
      v-if="profile.parcel"
      id="ap-cadastre-parcel"
      :label="t('addListing.cadastreParcelLabel')"
      v-model="form.cadastreParcelNr"
      :hint="t('addListing.cadastreParcelHelp')"
      inputmode="numeric"
      maxlength="32"
      placeholder="e.g. 21000030512"
    />

    <!-- Unified registry readout: whatever the cadastre resolved so far, from
         the address (building categories) and/or the parcel (land/commercial). -->
    <div
      v-if="figures.length"
      class="flex flex-col gap-1.5 rounded-lg bg-surface p-3"
    >
      <p class="text-sm font-medium text-ink flex items-center gap-1.5">
        <span class="text-accent-2" aria-hidden="true">✓</span>
        {{ t('addListing.cadastreMatched') }}
      </p>
      <dl class="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-ink-3">
        <div v-for="row in figures" :key="row.label" class="flex gap-1">
          <dt>{{ row.label }}:</dt>
          <dd class="text-ink-2">{{ row.value }}</dd>
        </div>
      </dl>
    </div>
    <p v-else class="text-xs text-ink-3 leading-tight">
      {{ t('addListing.cadastreEmpty') }}
    </p>
  </section>
</template>
