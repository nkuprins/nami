<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { categoryProfile } from '../../../types/categoryRegistry';
import type { Category } from '../../../types/listingItem';
import type { ListingFormState } from '../composables/formTypes';

const { t } = useI18n();
const {
  categoryOptions,
  kindOptions,
  commercialTypeOptions,
  landUseOptions,
} = usePropertyLabels();

const form = defineModel<ListingFormState>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();

const profile = computed(() => categoryProfile(form.value.propertyKind));

function setCategory(value: Category) {
  form.value.propertyKind = value;
  // Drop sub-type values that don't apply to the newly chosen category.
  const p = categoryProfile(value);
  if (p.subtype !== 'newProjectKind') form.value.newProjectKind = '';
  if (p.subtype !== 'commercial') form.value.commercialSubtype = '';
  if (p.subtype !== 'landUse') form.value.landUse = '';
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.propertyKind') }}
    </h2>
    <ToggleButtons
      :options="categoryOptions"
      :model-value="form.propertyKind"
      @update:model-value="setCategory($event as Category)"
    />

    <!-- new_project: apartment | house -->
    <div v-if="profile.subtype === 'newProjectKind'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.newProjectKind') }} <span class="text-warn">*</span>
      </p>
      <ToggleButtons
        :options="kindOptions"
        :model-value="form.newProjectKind"
        @update:model-value="form.newProjectKind = $event as typeof form.newProjectKind"
      />
      <p v-if="fieldError('newProjectKind')" class="text-xs text-warn">
        {{ fieldError('newProjectKind') }}
      </p>
    </div>

    <!-- commercial subtype -->
    <div v-if="profile.subtype === 'commercial'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.commercialType') }} <span class="text-warn">*</span>
      </p>
      <ToggleButtons
        :options="commercialTypeOptions"
        :model-value="form.commercialSubtype"
        @update:model-value="form.commercialSubtype = $event as typeof form.commercialSubtype"
      />
      <p v-if="fieldError('commercialSubtype')" class="text-xs text-warn">
        {{ fieldError('commercialSubtype') }}
      </p>
    </div>

    <!-- land use -->
    <div v-if="profile.subtype === 'landUse'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.landUse') }} <span class="text-warn">*</span>
      </p>
      <ToggleButtons
        :options="landUseOptions"
        :model-value="form.landUse"
        @update:model-value="form.landUse = $event as typeof form.landUse"
      />
      <p v-if="fieldError('landUse')" class="text-xs text-warn">
        {{ fieldError('landUse') }}
      </p>
    </div>
  </section>
</template>
