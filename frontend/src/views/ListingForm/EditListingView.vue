<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import ToggleButtons from '../../components/ui/ToggleButtons.vue';
import FormField from '../../components/ui/FormField.vue';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { usePhotoUpload } from './composables/usePhotoUpload';
import { useListingEditForm } from './composables/useListingEditForm';
import type { ListingType } from '../../types/listingItem';

import EditContentsRail from './components/edit/EditContentsRail.vue';
import LocationLockedSection from './components/edit/LocationLockedSection.vue';
import BasicInfoSection from './components/BasicInfoSection.vue';
import PricingSection from './components/PricingSection.vue';
import PropertyKindSection from './components/PropertyKindSection.vue';
import DetailsSection from './components/DetailsSection.vue';
import FeaturesSection from './components/FeaturesSection.vue';
import PhonesSection from './components/PhonesSection.vue';
import PhotosSection from './components/PhotosSection.vue';
import PlansSection from './components/PlansSection.vue';

const props = defineProps<{ id: string }>();

const { t } = useI18n();
const route = useRoute();
const { localePath } = useLocaleRoute();
const { typeOptions } = usePropertyLabels();

const photoUpload = usePhotoUpload();
const planUpload = usePhotoUpload(3);

const {
  form,
  submitting,
  submitError,
  fieldError,
  addPhone,
  removePhone,
  submit,
  loading,
  loadedType,
  districtDisplay,
  cityDisplay,
  savingPin,
  pinSaveError,
  siblingListingsCount,
  savePin,
} = useListingEditForm(props.id, photoUpload, planUpload);

// Editing keeps the transaction type fixed: show only the listing's own type
// as the single pill (based on the loaded type, so deselecting can't hide it).
const typeOption = computed(() =>
  typeOptions.value.filter((o) => o.id === loadedType.value)
);

const districtName = computed(() =>
  districtDisplay.value && cityDisplay.value
    ? `${districtDisplay.value}, ${cityDisplay.value}`
    : ''
);

const sections = computed(() => [
  { id: 'location', label: t('addListing.locationSection') },
  { id: 'category', label: t('addListing.listingType') },
  { id: 'description', label: t('addListing.basicInfo') },
  { id: 'photos', label: t('addListing.photosSection') },
  { id: 'pricing', label: t('addListing.pricing') },
]);

const highlightedSection = ref('');

onMounted(async () => {
  const target = route.query.section;
  if (typeof target !== 'string') return;
  await nextTick();
  // Loading finishes asynchronously (getListing), so wait a tick past that too.
  await nextTick();
  document
    .getElementById(target)
    ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  highlightedSection.value = target;
  setTimeout(() => (highlightedSection.value = ''), 1800);
});
</script>

<template>
  <div class="mx-auto max-w-4xl px-4 sm:px-6 py-10 sm:py-14">
    <div v-if="loading" class="flex flex-col gap-4">
      <div class="h-8 w-48 rounded bg-surface animate-pulse" />
      <div class="h-4 w-64 rounded bg-surface animate-pulse" />
      <div class="h-64 rounded-xl bg-surface animate-pulse mt-4" />
    </div>

    <template v-else>
      <div class="mb-8 flex items-start justify-between gap-4">
        <div>
          <h1 class="display-headline text-3xl text-ink">
            {{ t('editListing.title') }}
          </h1>
          <p class="text-sm text-ink-3 mt-1">{{ t('editListing.subtitle') }}</p>
        </div>
        <RouterLink
          :to="localePath('/')"
          class="text-sm text-ink-2 hover:text-ink underline underline-offset-2 transition-colors shrink-0 mt-1"
        >
          {{ t('addListing.cancel') }}
        </RouterLink>
      </div>

      <div class="flex gap-8 items-start">
        <EditContentsRail :sections="sections" />

        <form
          class="flex flex-col gap-6 min-w-0 flex-1"
          @submit.prevent="submit"
        >
          <div
            id="location"
            class="rounded-xl border p-6 transition-shadow"
            :class="
              highlightedSection === 'location'
                ? 'border-accent-2 ring-2 ring-accent-2/25'
                : 'border-line'
            "
          >
            <LocationLockedSection
              :address="form.address"
              :district-name="districtName"
              :district="districtDisplay"
              :city="cityDisplay"
              :coords="form.coords"
              :affected-listings-count="siblingListingsCount"
              :saving="savingPin"
              :save-error="pinSaveError"
              @update:coords="form.coords = $event"
              @save-pin="savePin(form.coords!)"
            />
          </div>

          <div
            id="category"
            class="rounded-xl border border-line p-6 flex flex-col gap-10"
          >
            <section class="flex flex-col gap-4">
              <h2
                class="text-base font-semibold text-ink border-b border-line pb-2"
              >
                {{ t('addListing.transactionType') }}
              </h2>
              <ToggleButtons
                :options="typeOption"
                :model-value="form.type"
                @update:model-value="form.type = $event as ListingType"
              />
              <p v-if="fieldError('type')" class="text-xs text-warn">
                {{ fieldError('type') }}
              </p>
            </section>
            <PropertyKindSection
              :property-kind="form.propertyKind"
              @update:property-kind="form.propertyKind = $event"
            />
          </div>

          <div
            id="description"
            class="rounded-xl border border-line p-6 flex flex-col gap-10"
          >
            <BasicInfoSection v-model:form="form" :field-error="fieldError" />
            <DetailsSection v-model:form="form" :field-error="fieldError" />
            <FeaturesSection v-model:form="form" />
          </div>

          <div
            id="photos"
            class="rounded-xl border border-line p-6 flex flex-col gap-10"
          >
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
          </div>

          <div
            id="pricing"
            class="rounded-xl border border-line p-6 flex flex-col gap-10"
          >
            <PricingSection
              v-model:form="form"
              :field-error="fieldError"
              is-edit
            />
            <PhonesSection
              v-model:form="form"
              :field-error="fieldError"
              @add-phone="addPhone"
              @remove-phone="removePhone"
            />
            <div>
              <FormField
                id="ap-website-url"
                :label="t('addListing.websiteUrl')"
                v-model="form.websiteUrl"
                type="url"
                placeholder="https://..."
              />
              <p class="text-xs text-ink-3 mt-1.5">
                {{ t('addListing.websiteUrlHint') }}
              </p>
            </div>
          </div>

          <div class="mt-2 pt-6 border-t border-line flex items-center gap-4">
            <button
              type="submit"
              :disabled="submitting"
              class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{
                submitting
                  ? t('addListing.submitting')
                  : t('editListing.publishListing')
              }}
            </button>
            <p v-if="submitError" class="text-sm text-warn">
              {{ submitError }}
            </p>
          </div>
        </form>
      </div>
    </template>
  </div>
</template>
