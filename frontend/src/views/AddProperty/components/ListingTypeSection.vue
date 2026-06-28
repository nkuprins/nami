<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import type { PropertyFormState } from '../composables/usePropertyForm';

const { t } = useI18n();
const { kindOptions } = usePropertyLabels();

const props = defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
}>();

const isBuyActive = computed(() => props.form.type === 'buy');
const isRentActive = computed(
  () => props.form.type === 'rent' || props.form.alsoRent
);

function toggleBuy() {
  if (props.form.type === 'new_project') {
    props.form.type = 'buy';
    return;
  }
  if (isBuyActive.value) {
    if (props.form.alsoRent) {
      props.form.type = 'rent';
      props.form.alsoRent = false;
    } else {
      props.form.type = '';
    }
  } else {
    if (props.form.type === 'rent') {
      props.form.type = 'buy';
      props.form.alsoRent = true;
    } else {
      props.form.type = 'buy';
    }
  }
}

function toggleRent() {
  if (props.form.type === 'new_project') {
    props.form.type = 'rent';
    return;
  }
  if (isRentActive.value) {
    if (props.form.alsoRent) {
      props.form.alsoRent = false;
    } else {
      props.form.type = '';
    }
  } else {
    if (props.form.type === 'buy') {
      props.form.alsoRent = true;
    } else {
      props.form.type = 'rent';
    }
  }
}

function selectNewProject() {
  props.form.type = props.form.type === 'new_project' ? '' : 'new_project';
  props.form.alsoRent = false;
}
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.listingType') }}
    </h2>
    <div class="flex flex-col gap-5">
      <!-- Transaction type -->
      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">
          {{ t('addProperty.transactionType') }}
          <span class="text-red-500">*</span>
        </p>

        <div class="flex flex-col gap-2">
          <!-- Pārdošanā -->
          <label
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
          </label>

          <!-- Īrei -->
          <label
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
          </label>

          <!-- Divider -->
          <div class="flex items-center gap-2 my-0.5">
            <div class="h-px flex-1 bg-line" />
            <span class="text-xs text-ink-3">{{ t('common.or') }}</span>
            <div class="h-px flex-1 bg-line" />
          </div>

          <!-- Jaunais projekts -->
          <label
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
          </label>
        </div>

        <p v-if="fieldError('type')" class="text-xs text-red-500">
          {{ fieldError('type') }}
        </p>
      </div>

      <!-- Property kind -->
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
