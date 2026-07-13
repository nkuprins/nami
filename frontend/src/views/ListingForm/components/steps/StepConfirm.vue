<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../../composables/usePropertyLabels';
import { formatPrice } from '../../../../utils/format';
import type { ListingFormState } from '../../composables/formTypes';
import type { ListingType } from '../../../../types/listingItem';
import type { ListingWizardStep } from '../../composables/useWizardStepValidity';
import IconEdit from '../../../../components/icons/IconEdit.vue';

const props = withDefaults(
  defineProps<{
    form: ListingFormState;
    photos: Array<{ preview: string } | { url: string }>;
    addressLine: string;
    submitting: boolean;
    submitError: string;
    rentListingWarning: boolean;
    // False when the wizard has no Location step to jump back to (e.g. adding
    // a listing to an existing property, where the address is fixed).
    showAddressEdit?: boolean;
  }>(),
  { showAddressEdit: true }
);

const emit = defineEmits<{
  (e: 'edit-step', step: ListingWizardStep): void;
  (e: 'submit'): void;
}>();

const { t } = useI18n();
const { typeLabel, kindLabel } = usePropertyLabels();

const title = computed(
  () => props.form.titleLv || props.form.titleEn || props.form.titleRu || ''
);

const phones = computed(() =>
  props.form.phones.filter((p) => p.phone.trim()).map((p) => p.phone)
);

function thumbSrc(entry: { preview: string } | { url: string }): string {
  return 'preview' in entry ? entry.preview : entry.url;
}

function priceLine(type: ListingType, amount: string): string {
  return amount ? formatPrice(Number(amount), type) : '';
}
</script>

<template>
  <div class="flex flex-col gap-6">
    <div>
      <h2 class="text-lg font-display text-ink">
        {{ t('addListing.confirmTitle') }}
      </h2>
      <p class="text-sm text-ink-3 mt-1">
        {{ t('addListing.confirmSubtitle') }}
      </p>
    </div>

    <div class="flex flex-col gap-3">
      <!-- Address -->
      <div
        class="rounded-xl border border-line bg-surface/40 p-4 flex items-start justify-between gap-3"
      >
        <div class="min-w-0">
          <p class="micro-label">{{ t('addListing.confirmAddress') }}</p>
          <p class="text-sm text-ink mt-1">{{ addressLine }}</p>
        </div>
        <button
          v-if="showAddressEdit"
          type="button"
          class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
          :aria-label="t('addListing.confirmEdit')"
          @click="emit('edit-step', 'location')"
        >
          <IconEdit class="size-3.5" />
        </button>
      </div>

      <!-- Category -->
      <div
        class="rounded-xl border border-line bg-surface/40 p-4 flex items-start justify-between gap-3"
      >
        <div class="min-w-0">
          <p class="micro-label">{{ t('addListing.confirmCategory') }}</p>
          <p class="text-sm text-ink mt-1">
            <template v-if="form.type">{{
              typeLabel(form.type as ListingType)
            }}</template>
            <span v-if="form.alsoRent"> + {{ typeLabel('rent') }}</span>
            · {{ kindLabel(form.propertyKind) }}
          </p>
        </div>
        <button
          type="button"
          class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
          :aria-label="t('addListing.confirmEdit')"
          @click="emit('edit-step', 'category')"
        >
          <IconEdit class="size-3.5" />
        </button>
      </div>

      <!-- Title + price -->
      <div
        class="rounded-xl border border-line bg-surface/40 p-4 flex items-start justify-between gap-3"
      >
        <div class="min-w-0 flex-1">
          <p class="micro-label">{{ t('addListing.confirmPrice') }}</p>
          <p v-if="title" class="text-sm text-ink mt-1 truncate">{{ title }}</p>
          <p class="display-price text-lg text-ink mt-0.5">
            {{ priceLine(form.type as ListingType, form.price) }}
            <span v-if="form.alsoRent" class="text-sm text-ink-2 font-sans">
              · {{ priceLine('rent', form.rentPrice) }}
            </span>
          </p>
        </div>
        <button
          type="button"
          class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
          :aria-label="t('addListing.confirmEdit')"
          @click="emit('edit-step', 'publish')"
        >
          <IconEdit class="size-3.5" />
        </button>
      </div>

      <!-- Photos -->
      <div
        class="rounded-xl border border-line bg-surface/40 p-4 flex flex-col gap-3"
      >
        <div class="flex items-start justify-between gap-3">
          <p class="micro-label">
            {{ t('addListing.confirmPhotos') }} ({{ photos.length }})
          </p>
          <button
            type="button"
            class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
            :aria-label="t('addListing.confirmEdit')"
            @click="emit('edit-step', 'photos')"
          >
            <IconEdit class="size-3.5" />
          </button>
        </div>
        <div v-if="photos.length" class="flex gap-2 overflow-x-auto">
          <img
            v-for="(entry, i) in photos.slice(0, 8)"
            :key="i"
            :src="thumbSrc(entry)"
            class="size-16 shrink-0 rounded-lg object-cover"
            alt=""
          />
        </div>
      </div>

      <!-- Phones -->
      <div
        class="rounded-xl border border-line bg-surface/40 p-4 flex items-start justify-between gap-3"
      >
        <div class="min-w-0">
          <p class="micro-label">{{ t('addListing.confirmPhones') }}</p>
          <p class="text-sm text-ink mt-1">{{ phones.join(' · ') }}</p>
        </div>
        <button
          type="button"
          class="focus-ring shrink-0 size-8 grid place-items-center rounded-full text-ink-3 hover:text-ink hover:bg-line/60 transition-colors"
          :aria-label="t('addListing.confirmEdit')"
          @click="emit('edit-step', 'publish')"
        >
          <IconEdit class="size-3.5" />
        </button>
      </div>
    </div>

    <p v-if="rentListingWarning" class="text-sm text-ink-2">
      {{ t('addListing.rentListingFailed') }}
    </p>

    <div class="flex items-center gap-4 pt-2">
      <button
        type="button"
        :disabled="submitting"
        class="h-11 px-8 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        @click="emit('submit')"
      >
        {{
          submitting
            ? t('addListing.submitting')
            : t('addListing.publishListing')
        }}
      </button>
    </div>

    <p v-if="submitError" class="text-sm text-warn">{{ submitError }}</p>
  </div>
</template>
