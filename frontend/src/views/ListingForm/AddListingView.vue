<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useLocationDropdown } from './composables/useLocationDropdown';
import { useListingForm } from './composables/useListingForm';
import { useListingDraft } from './composables/useListingDraft';
import { useDuplicatePropertyNudge } from './composables/useDuplicatePropertyNudge';
import { useWizardNavigation } from './composables/useWizardNavigation';
import {
  buildStepperSteps,
  LISTING_WIZARD_STEPS,
  type ListingWizardStep,
} from './composables/useWizardStepValidity';
import { selectedBuildingCode } from './composables/formHelpers';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import WizardStepper from '../../components/ui/WizardStepper.vue';

import StepLocation from './components/steps/StepLocation.vue';
import StepCategory from './components/steps/StepCategory.vue';
import StepDescription from './components/steps/StepDescription.vue';
import StepPhotos from './components/steps/StepPhotos.vue';
import StepPublish from './components/steps/StepPublish.vue';
import StepConfirm from './components/steps/StepConfirm.vue';

const { t } = useI18n();
const { localePath, localePush } = useLocaleRoute();

const STEPS = [...LISTING_WIZARD_STEPS];

const stepperSteps = computed(() => buildStepperSteps(STEPS, t));

const photoUpload = usePhotoUpload();
const planUpload = usePhotoUpload(3);
const { selectedLocation, isOpen, districtName, onSelect } =
  useLocationDropdown();
const nudge = useDuplicatePropertyNudge();

const turnstileEnabled = !!import.meta.env.VITE_TURNSTILE_SITE_KEY;
const turnstileToken = ref('');
const stepPublishRef = ref<InstanceType<typeof StepPublish> | null>(null);

const {
  form,
  touched,
  submitting,
  submitError,
  rentListingWarning,
  errors,
  fieldError,
  addPhone,
  removePhone,
  submit,
} = useListingForm(
  () => selectedLocation.value,
  photoUpload,
  planUpload,
  {
    blocked: () => nudge.blockSubmit.value,
    confirmed: () => nudge.acknowledged.value,
  },
  {
    token: () => turnstileToken.value,
    reset: () => stepPublishRef.value?.resetTurnstile(),
  }
);

const { wizard, handleContinue, handleBack, jumpToStep, handleJump } =
  useWizardNavigation(STEPS, errors, touched);

// Restore any saved draft (form + location + step) and keep it in sync. Cleared
// only on a successful submit — `submitting` stays true through the redirect,
// so a route-leave while it's set means we published; Cancel leaves it false
// and the draft survives for next time.
const draft = useListingDraft(form, selectedLocation, wizard);
onBeforeRouteLeave(() => {
  if (submitting.value) draft.clear();
});

// The only hard block on Continue itself (nothing more to reveal by clicking —
// the duplicate card already explains it). Ordinary field validation is instead
// checked on click, revealing that step's red errors rather than pre-disabling.
const continueBlocked = computed(
  () => wizard.currentStep.value === 'location' && nudge.blockSubmit.value
);

const locationStepIndex = STEPS.indexOf('location');

let nudgeTimer: ReturnType<typeof setTimeout> | undefined;

function goAddToExisting() {
  if (!nudge.match.value) return;
  localePush(`/property/${nudge.match.value.propertyId}/add-listing`);
}

// Steps already completed on an earlier pass (before the address became a
// duplicate) stay clickable in the stepper rail and reachable via Confirm's
// "edit" links — without this guard either would jump straight past the
// blocked Location step, leaving the user stuck on a Confirm screen whose
// Submit silently no-ops (see useListingForm's `duplicate?.blocked()` check).
function guardedJump(index: number) {
  if (nudge.blockSubmit.value && index > locationStepIndex) {
    jumpToStep('location');
    return;
  }
  handleJump(index);
}

function guardedEditStep(step: ListingWizardStep) {
  if (nudge.blockSubmit.value && STEPS.indexOf(step) > locationStepIndex) {
    jumpToStep('location');
    return;
  }
  jumpToStep(step);
}

watch(
  () =>
    [
      form.address,
      selectedBuildingCode(form),
      form.apartment,
      selectedLocation.value?.district,
      selectedLocation.value?.city,
    ] as const,
  ([address, arBuildingCode, apartment, district, city]) => {
    clearTimeout(nudgeTimer);
    nudgeTimer = setTimeout(
      () =>
        nudge.check({ arBuildingCode, apartment, address, district, city }),
      400
    );
  },
  // immediate: a restored draft (see useListingDraft) fills these fields
  // before this watcher exists, so without it a duplicate address that was
  // already there on mount would never get (re-)checked.
  { immediate: true }
);

// Changing the transaction type after Publish was already filled in can
// invalidate it (e.g. adding a second type with no price of its own yet), so
// drop its completed mark rather than leave a stale checkmark in the rail.
watch(
  () => [form.type, form.alsoRent] as const,
  () => wizard.uncomplete(STEPS.indexOf('publish'))
);
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-6 py-10 sm:py-14">
    <div class="mb-8 flex items-start justify-between gap-4">
      <div>
        <h1 class="display-headline text-3xl text-ink">{{ t('addListing.title') }}</h1>
        <p class="text-sm text-ink-3 mt-1">{{ t('addListing.subtitle') }}</p>
      </div>
      <RouterLink
        :to="localePath('/')"
        class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors shrink-0 mt-1"
      >
        {{ t('addListing.cancel') }}
      </RouterLink>
    </div>

    <div class="flex flex-col md:flex-row gap-6 md:gap-10 items-start">
      <WizardStepper
        :steps="stepperSteps"
        :current-index="wizard.currentIndex.value"
        :completed="wizard.completed.value"
        @jump="guardedJump"
      />

      <div class="w-full min-w-0 max-w-2xl">
        <div class="flex flex-col gap-10">
          <div
            v-show="wizard.currentStep.value === 'location'"
            data-step="location"
          >
            <StepLocation
              v-model:form="form"
              :field-error="fieldError"
              :district-name="districtName"
              :selected-location="selectedLocation"
              :is-open="isOpen"
              :match-kind="nudge.matchKind.value"
              :match="nudge.match.value"
              :acknowledged="nudge.acknowledged.value"
              @update:is-open="isOpen = $event"
              @select="onSelect"
              @add-listing="goAddToExisting"
              @acknowledge-fuzzy="nudge.acknowledgeFuzzy"
            />
          </div>

          <div
            v-show="wizard.currentStep.value === 'category'"
            data-step="category"
          >
            <StepCategory v-model:form="form" :field-error="fieldError" />
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
              ref="stepPublishRef"
              v-model:form="form"
              v-model:turnstile-token="turnstileToken"
              :field-error="fieldError"
              :turnstile-enabled="turnstileEnabled"
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
              :address-line="
                districtName ? `${form.address}, ${districtName}` : form.address
              "
              :submitting="submitting"
              :submit-error="submitError"
              :rent-listing-warning="rentListingWarning"
              @edit-step="guardedEditStep"
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
            :disabled="continueBlocked"
            class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            @click="handleContinue"
          >
            {{ t('addListing.continue') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
