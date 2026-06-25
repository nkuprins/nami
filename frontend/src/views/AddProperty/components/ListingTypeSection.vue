<script setup lang="ts">
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { PropertyFormState } from '../composables/usePropertyForm';

const { t } = useI18n();
const { typeOptions, kindOptions } = usePropertyLabels();

defineProps<{ form: PropertyFormState }>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.listingType') }}
    </h2>
    <div class="flex flex-col gap-3">
      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">
          {{ t('addProperty.transactionType') }}
        </p>
        <ToggleButtons
          :options="typeOptions"
          :model-value="form.type"
          @update:model-value="form.type = $event as typeof form.type"
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">
          {{ t('addProperty.propertyKind') }}
        </p>
        <ToggleButtons
          :options="kindOptions"
          :model-value="form.propertyKind"
          @update:model-value="
            form.propertyKind = $event as typeof form.propertyKind
          "
        />
      </div>
    </div>
  </section>
</template>
