<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import PropertyKindSection from './PropertyKindSection.vue';
import type { ListingFormState } from '../composables/formTypes';

const { t } = useI18n();

const form = defineModel<ListingFormState>('form', { required: true });
defineProps<{
  fieldError: (field: string) => string | undefined;
}>();

const isBuyActive = computed(() => form.value.type === 'buy');
const isRentActive = computed(
  () => form.value.type === 'rent' || form.value.alsoRent
);

function toggleBuy() {
  if (form.value.type === 'new_project') {
    form.value.type = 'buy';
    return;
  }
  if (isBuyActive.value) {
    if (form.value.alsoRent) {
      form.value.type = 'rent';
      form.value.alsoRent = false;
    } else {
      form.value.type = '';
    }
  } else if (form.value.type === 'rent') {
    form.value.type = 'buy';
    form.value.alsoRent = true;
  } else {
    form.value.type = 'buy';
  }
}

function toggleRent() {
  if (form.value.type === 'new_project') {
    form.value.type = 'rent';
    return;
  }
  if (isRentActive.value) {
    if (form.value.alsoRent) {
      form.value.alsoRent = false;
    } else {
      form.value.type = '';
    }
  } else if (form.value.type === 'buy') {
    form.value.alsoRent = true;
  } else {
    form.value.type = 'rent';
  }
}

function selectNewProject() {
  form.value.type = form.value.type === 'new_project' ? '' : 'new_project';
  form.value.alsoRent = false;
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.listingType') }}
    </h2>
    <div class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.transactionType') }}
        <span class="text-red-500">*</span>
      </p>

      <div class="flex flex-col gap-2">
        <!-- Pārdošanā -->
        <div
          class="flex items-center gap-3 cursor-pointer select-none group w-fit"
        >
          <span
            class="w-5 h-5 rounded border flex items-center justify-center shrink-0 transition-colors"
            :class="
              isBuyActive
                ? 'bg-ink border-ink'
                : 'bg-bg border-line group-hover:border-ink/50'
            "
            @click.prevent="toggleBuy"
          >
            <svg
              v-if="isBuyActive"
              class="w-3 h-3 text-bg"
              viewBox="0 0 12 12"
              fill="none"
            >
              <path
                d="M2 6l3 3 5-5"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="text-sm text-ink" @click="toggleBuy">{{
            t('types.buy')
          }}</span>
        </div>

        <!-- Īrei -->
        <div
          class="flex items-center gap-3 cursor-pointer select-none group w-fit"
        >
          <span
            class="w-5 h-5 rounded border flex items-center justify-center shrink-0 transition-colors"
            :class="
              isRentActive
                ? 'bg-ink border-ink'
                : 'bg-bg border-line group-hover:border-ink/50'
            "
            @click.prevent="toggleRent"
          >
            <svg
              v-if="isRentActive"
              class="w-3 h-3 text-bg"
              viewBox="0 0 12 12"
              fill="none"
            >
              <path
                d="M2 6l3 3 5-5"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="text-sm text-ink" @click="toggleRent">{{
            t('types.rent')
          }}</span>
        </div>

        <!-- Divider -->
        <div class="flex items-center gap-2 my-0.5">
          <div class="h-px flex-1 bg-line" />
          <span class="text-xs text-ink-3">{{ t('common.or') }}</span>
          <div class="h-px flex-1 bg-line" />
        </div>

        <!-- Jaunais projekts -->
        <div
          class="flex items-center gap-3 cursor-pointer select-none group w-fit"
        >
          <span
            class="w-5 h-5 rounded border flex items-center justify-center shrink-0 transition-colors"
            :class="
              form.type === 'new_project'
                ? 'bg-ink border-ink'
                : 'bg-bg border-line group-hover:border-ink/50'
            "
            @click.prevent="selectNewProject"
          >
            <svg
              v-if="form.type === 'new_project'"
              class="w-3 h-3 text-bg"
              viewBox="0 0 12 12"
              fill="none"
            >
              <path
                d="M2 6l3 3 5-5"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </span>
          <span class="text-sm text-ink" @click="selectNewProject">{{
            t('types.new_project')
          }}</span>
        </div>
      </div>

      <p v-if="fieldError('type')" class="text-xs text-red-500">
        {{ fieldError('type') }}
      </p>
    </div>
  </section>

  <PropertyKindSection
    :property-kind="form.propertyKind"
    @update:property-kind="form.propertyKind = $event"
  />
</template>
