<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import ToggleButtons from '../../components/ui/ToggleButtons.vue';
import WizardStepper from '../../components/ui/WizardStepper.vue';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useAddListingToProperty } from './composables/useListingFields';
import { setTransactionType } from './composables/formHelpers';
import { useWizardNavigation } from './composables/useWizardNavigation';
import {
  buildStepperSteps,
  LISTING_WIZARD_STEPS,
} from './composables/useWizardStepValidity';
import type { ListingType } from '../../types/listingItem';

import PropertyKindSection from './components/PropertyKindSection.vue';
import StepDescription from './components/steps/StepDescription.vue';
import StepPhotos from './components/steps/StepPhotos.vue';
import StepPublish from './components/steps/StepPublish.vue';
import StepConfirm from './components/steps/StepConfirm.vue';

const props = defineProps<{ id: string }>();

const { t } = useI18n();
const { localePath } = useLocaleRoute();

// The property's location is inherited, so this wizard skips the location step.
const STEPS = LISTING_WIZARD_STEPS.filter((s) => s !== 'location');

const stepperSteps = computed(() => buildStepperSteps(STEPS, t));

const photoUpload = usePhotoUpload();
const planUpload = usePhotoUpload(3);

const {
  form,
  availableTypeOptions,
  loading,
  submitting,
  submitError,
  errors,
  touched,
  fieldError,
  propertyLocation,
  addPhone,
  removePhone,
  submit,
} = useAddListingToProperty(props.id, photoUpload, planUpload);

const { wizard, handleContinue, handleBack, jumpToStep, handleJump } =
  useWizardNavigation(STEPS, errors, touched);

const addressLine = computed(() => {
  const loc = propertyLocation.value;
  return loc ? `${loc.address}, ${loc.district}, ${loc.city}` : '';
});

function handleTypeChange(type: ListingType | '') {
  setTransactionType(form, () => {
    form.type = type;
  });
  wizard.uncomplete(STEPS.indexOf('publish'));
}
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-6 py-10 sm:py-14">
    <div v-if="loading" class="flex flex-col gap-4">
      <div class="h-8 w-48 rounded bg-surface animate-pulse" />
      <div class="h-4 w-64 rounded bg-surface animate-pulse" />
      <div class="h-64 rounded-xl bg-surface animate-pulse mt-4" />
    </div>

    <template v-else>
      <div class="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-ink">
            {{ t('drawers.addListingTypeTitle') }}
          </h1>
          <p class="text-sm text-ink-3 mt-1">
            {{ t('drawers.addListingTypeSubtitle') }}
          </p>
        </div>
        <RouterLink
          :to="localePath('/')"
          class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors shrink-0 mt-1"
        >
          {{ t('addListing.cancel') }}
        </RouterLink>
      </div>

      <div
        v-if="addressLine"
        class="mb-8 rounded-xl border border-line bg-surface/40 px-4 py-3"
      >
        <p class="micro-label">{{ t('addListing.confirmAddress') }}</p>
        <p class="text-sm text-ink mt-1">{{ addressLine }}</p>
      </div>

      <div class="flex flex-col md:flex-row gap-6 md:gap-10 items-start">
        <WizardStepper
          :steps="stepperSteps"
          :current-index="wizard.currentIndex.value"
          :completed="wizard.completed.value"
          @jump="handleJump"
        />

        <div class="w-full min-w-0 max-w-2xl">
          <div class="flex flex-col gap-10">
            <div
              v-show="wizard.currentStep.value === 'category'"
              data-step="category"
              class="flex flex-col gap-10"
            >
              <section class="flex flex-col gap-4">
                <h2
                  class="text-base font-semibold text-ink border-b border-line pb-2"
                >
                  {{ t('addListing.listingType') }}
                </h2>
                <ToggleButtons
                  :options="availableTypeOptions"
                  :model-value="form.type"
                  @update:model-value="
                    handleTypeChange($event as ListingType | '')
                  "
                />
                <p v-if="fieldError('type')" class="text-xs text-red-500">
                  {{ fieldError('type') }}
                </p>
              </section>

              <PropertyKindSection
                :property-kind="form.propertyKind"
                @update:property-kind="form.propertyKind = $event"
              />
            </div>

            <div
              v-show="wizard.currentStep.value === 'description'"
              data-step="description"
            >
              <StepDescription v-model:form="form" :field-error="fieldError" />
            </div>

            <div
              v-show="wizard.currentStep.value === 'photos'"
              data-step="photos"
            >
              <StepPhotos
                v-model:form="form"
                :photos="photoUpload.photos.value"
                :plans="planUpload.photos.value"
                :field-error="fieldError"
                @add-photo-files="photoUpload.addFiles"
                @remove-photo="photoUpload.remove"
                @move-photo="photoUpload.move"
                @add-plan-files="planUpload.addFiles"
                @remove-plan="planUpload.remove"
                @move-plan="planUpload.move"
              />
            </div>

            <div
              v-show="wizard.currentStep.value === 'publish'"
              data-step="publish"
            >
              <StepPublish
                v-model:form="form"
                :field-error="fieldError"
                :turnstile-enabled="false"
                @add-phone="addPhone"
                @remove-phone="removePhone"
              />
            </div>

            <div
              v-show="wizard.currentStep.value === 'confirm'"
              data-step="confirm"
            >
              <StepConfirm
                :form="form"
                :photos="photoUpload.photos.value"
                :address-line="addressLine"
                :show-address-edit="false"
                :submitting="submitting"
                :submit-error="submitError"
                :rent-listing-warning="false"
                @edit-step="jumpToStep"
                @submit="submit"
              />
            </div>
          </div>

          <div
            v-if="wizard.currentStep.value !== 'confirm'"
            class="mt-10 pt-6 border-t border-line flex items-center justify-between"
          >
            <button
              v-if="!wizard.isFirst.value"
              type="button"
              class="text-sm text-ink-2 hover:text-ink transition-colors"
              @click="handleBack"
            >
              {{ t('addListing.back') }}
            </button>
            <span v-else />
            <button
              type="button"
              class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              @click="handleContinue"
            >
              {{ t('addListing.continue') }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
