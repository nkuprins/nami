<script setup lang="ts">
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { PropertyFormState } from '../composables/usePropertyForm';

const { t } = useI18n();
const { featureOptions } = usePropertyLabels();

defineProps<{
  form: PropertyFormState;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.featuresSection') }}
    </h2>
    <ToggleButtons
      :options="featureOptions"
      :model-value="form.features"
      multiple
      @update:model-value="form.features = $event as typeof form.features"
    />
  </section>
</template>
