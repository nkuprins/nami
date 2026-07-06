<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ref, watch } from 'vue';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useLocationDropdown } from './composables/useLocationDropdown';
import { useListingForm } from './composables/useListingForm';
import { useDuplicatePropertyNudge } from './composables/useDuplicatePropertyNudge';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import TurnstileWidget from '../../components/ui/TurnstileWidget.vue';

import ListingTypeSection from './components/ListingTypeSection.vue';
import BasicInfoSection from './components/BasicInfoSection.vue';
import PricingSection from './components/PricingSection.vue';
import LocationSection from './components/LocationSection.vue';
import DetailsSection from './components/DetailsSection.vue';
import FeaturesSection from './components/FeaturesSection.vue';
import PhonesSection from './components/PhonesSection.vue';
import PhotosSection from './components/PhotosSection.vue';
import PlansSection from './components/PlansSection.vue';

const { t } = useI18n();
const { localePath, localePush } = useLocaleRoute();

const photoUpload = usePhotoUpload();
const planUpload = usePhotoUpload(3);
const { selectedLocation, isOpen, districtName, onSelect } =
  useLocationDropdown();
const nudge = useDuplicatePropertyNudge();

// Cloudflare Turnstile human-check on submission. Inactive locally when no site
// key is configured, so dev/mock flows aren't blocked.
const turnstileEnabled = !!import.meta.env.VITE_TURNSTILE_SITE_KEY;
const turnstileToken = ref('');
const turnstileWidget = ref<InstanceType<typeof TurnstileWidget> | null>(null);

const {
  form,
  submitting,
  submitError,
  rentListingWarning,
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
    reset: () => turnstileWidget.value?.reset(),
  }
);

let nudgeTimer: ReturnType<typeof setTimeout> | undefined;

function goAddToExisting() {
  if (!nudge.match.value) return;
  localePush(`/property/${nudge.match.value.propertyId}/add-listing`);
}

watch(
  () =>
    [
      form.address,
      selectedLocation.value?.district,
      selectedLocation.value?.city,
    ] as const,
  ([address, district, city]) => {
    clearTimeout(nudgeTimer);
    nudgeTimer = setTimeout(() => nudge.check(address, district, city), 400);
  }
);
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-10 sm:py-14">
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-ink">{{ t('addListing.title') }}</h1>
      <p class="text-sm text-ink-3 mt-1">{{ t('addListing.subtitle') }}</p>
    </div>

    <Teleport to="body">
      <Transition name="scrim">
        <div
          v-if="nudge.blockSubmit.value && nudge.match.value"
          class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4"
        >
          <div class="absolute inset-0 bg-ink/40 backdrop-blur-sm" />
          <dialog
            open
            aria-modal="true"
            class="relative z-10 w-full max-w-sm bg-bg rounded-2xl shadow-lift border border-line p-6 flex flex-col gap-5"
          >
            <div class="flex flex-col gap-1.5">
              <h2 class="text-base font-semibold text-ink">
                {{
                  nudge.matchKind.value === 'fuzzy'
                    ? t('addListing.duplicateNudgeFuzzyTitle')
                    : t('addListing.duplicateNudgeTitle')
                }}
              </h2>
              <p class="text-sm text-ink-2 leading-relaxed">
                {{
                  nudge.matchKind.value === 'fuzzy'
                    ? t('addListing.duplicateNudgeFuzzy', {
                        address: nudge.match.value.location.address,
                      })
                    : t('addListing.duplicateNudge', {
                        address: nudge.match.value.location.address,
                      })
                }}
              </p>
            </div>
            <div class="flex flex-col gap-2">
              <RouterLink
                :to="localePath(`/listing/${nudge.match.value.id}`)"
                class="focus-ring h-9 px-4 rounded-lg bg-ink text-bg text-sm font-medium flex items-center justify-center hover:bg-accent-2 transition-colors"
              >
                {{ t('addListing.duplicateNudgeView') }}
              </RouterLink>
              <button
                type="button"
                class="focus-ring h-9 px-4 rounded-lg border border-line text-sm text-ink-2 hover:bg-surface transition-colors"
                @click="goAddToExisting()"
              >
                {{ t('addListing.duplicateNudgeAddListing') }}
              </button>
              <button
                v-if="nudge.matchKind.value === 'fuzzy'"
                type="button"
                class="text-sm text-ink-3 hover:text-ink transition-colors self-center"
                @click="nudge.acknowledgeFuzzy()"
              >
                {{ t('addListing.duplicateNudgeFuzzyContinue') }}
              </button>
            </div>
          </dialog>
        </div>
      </Transition>
    </Teleport>

    <form class="flex flex-col gap-10" @submit.prevent="submit">
      <ListingTypeSection v-model:form="form" :field-error="fieldError" />

      <PricingSection
        v-model:form="form"
        :field-error="fieldError"
        :is-edit="false"
      />

      <BasicInfoSection v-model:form="form" :field-error="fieldError" />

      <LocationSection
        v-model:form="form"
        :field-error="fieldError"
        :district-name="districtName"
        :selected-location="selectedLocation"
        v-model:is-open="isOpen"
        @select="onSelect"
      />

      <DetailsSection v-model:form="form" :field-error="fieldError" />

      <FeaturesSection v-model:form="form" />

      <PhonesSection
        v-model:form="form"
        :field-error="fieldError"
        @add-phone="addPhone"
        @remove-phone="removePhone"
      />

      <PhotosSection
        v-model:form="form"
        :photos="photoUpload.photos.value"
        :field-error="fieldError"
        @add-files="photoUpload.addFiles"
        @remove-photo="photoUpload.remove"
        @move="photoUpload.move"
      />
      <PlansSection
        :plans="planUpload.photos.value"
        @add-files="planUpload.addFiles"
        @remove-plan="planUpload.remove"
        @move="planUpload.move"
      />

      <p v-if="rentListingWarning" class="text-sm text-amber-600">
        {{ t('addListing.rentListingFailed') }}
      </p>

      <TurnstileWidget
        v-if="turnstileEnabled"
        ref="turnstileWidget"
        v-model="turnstileToken"
      />

      <div class="flex items-center gap-4 pt-2">
        <button
          type="submit"
          :disabled="
            submitting ||
            nudge.blockSubmit.value ||
            (turnstileEnabled && !turnstileToken)
          "
          class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{
            submitting
              ? t('addListing.submitting')
              : t('addListing.publishListing')
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
  </div>
</template>
