<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import ToggleButtons from '../../components/ui/ToggleButtons.vue';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { useAddListingToProperty } from './composables/useListingFields';
import type { ListingType } from '../../types/listingItem';

import BasicInfoSection from './components/BasicInfoSection.vue';
import PricingSection from './components/PricingSection.vue';
import PhonesSection from './components/PhonesSection.vue';

const props = defineProps<{ id: string }>();

const { t } = useI18n();
const { localePath } = useLocaleRoute();

const {
  form,
  availableTypeOptions,
  loading,
  submitting,
  submitError,
  fieldError,
  addPhone,
  removePhone,
  submit,
} = useAddListingToProperty(props.id);
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-10 sm:py-14">
    <div v-if="loading" class="flex flex-col gap-4">
      <div class="h-8 w-48 rounded bg-surface animate-pulse" />
      <div class="h-4 w-64 rounded bg-surface animate-pulse" />
      <div class="h-64 rounded-xl bg-surface animate-pulse mt-4" />
    </div>

    <template v-else>
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-ink">
          {{ t('drawers.addListingTypeTitle') }}
        </h1>
      </div>

      <form class="flex flex-col gap-10" @submit.prevent="submit">
        <section class="flex flex-col gap-4">
          <h2
            class="text-base font-semibold text-ink border-b border-line pb-2"
          >
            {{ t('addListing.transactionType') }}
          </h2>
          <ToggleButtons
            :options="availableTypeOptions"
            :model-value="form.type"
            @update:model-value="form.type = $event as ListingType"
          />
          <p v-if="fieldError('type')" class="text-xs text-red-500">
            {{ fieldError('type') }}
          </p>
        </section>

        <PricingSection
          v-model:form="form"
          :field-error="fieldError"
          :is-edit="false"
        />

        <BasicInfoSection v-model:form="form" :field-error="fieldError" />

        <PhonesSection
          v-model:form="form"
          :field-error="fieldError"
          @add-phone="addPhone"
          @remove-phone="removePhone"
        />

        <div class="flex items-center gap-4 pt-2">
          <button
            type="submit"
            :disabled="submitting"
            class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{
              submitting
                ? t('addListing.submitting')
                : t('drawers.addListingTypeSubmit')
            }}
          </button>
          <RouterLink
            :to="localePath('/')"
            class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors"
          >
            {{ t('addListing.cancel') }}
          </RouterLink>
        </div>

        <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>
      </form>
    </template>
  </div>
</template>
