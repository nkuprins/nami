<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { numericInput } from '../../../utils/utils';
import { formatPriceInput, parsePriceInput } from '../../../utils/format';
import FormField from '../../../components/ui/FormField.vue';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import type { ListingFieldsForm } from '../composables/formTypes';

const { t } = useI18n();
const { completionOptions } = usePropertyLabels();

const form = defineModel<ListingFieldsForm>('form', { required: true });
const props = defineProps<{
  fieldError: (field: string) => string | undefined;
  isEdit: boolean;
}>();

const priceLabel = computed(() =>
  form.value.type === 'rent'
    ? t('addListing.rentPriceField')
    : t('addListing.priceField')
);

const pricePlaceholder = computed(() =>
  form.value.type === 'rent' ? 'e.g. 1,200' : 'e.g. 185,000'
);

const isDual = computed(
  () => !props.isEdit && form.value.type === 'buy' && form.value.alsoRent
);

const vatCheckboxClass = 'size-4 rounded border-line accent-ink cursor-pointer';
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.pricing') }}
    </h2>

    <!-- Dual mode: two labeled price cards, second becomes a separate listing on submit -->
    <template v-if="isDual">
      <div class="rounded-lg border border-line p-4 flex flex-col gap-3">
        <p class="text-xs font-semibold uppercase tracking-wide text-ink-3">
          {{ t('types.buy') }}
        </p>
        <FormField
          id="ap-price"
          :label="t('addListing.priceField')"
          v-model="form.price"
          :error="fieldError('price')"
          required
          inputmode="numeric"
          placeholder="e.g. 185,000"
          :format="formatPriceInput"
          :parse="parsePriceInput"
          @beforeinput="numericInput"
        />
        <label class="flex items-center gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="form.vatIncluded"
            :class="vatCheckboxClass"
          />
          <span class="text-sm text-ink">{{
            t('addListing.vatIncluded')
          }}</span>
        </label>
        <div class="flex flex-col gap-1.5">
          <label for="ap-duration" class="text-sm font-medium text-ink">
            {{ t('addListing.listingDuration') }}
            <span class="text-red-500">*</span>
          </label>
          <select
            id="ap-duration"
            v-model.number="form.durationMonths"
            class="h-10 rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
          >
            <option v-for="n in 6" :key="n" :value="n">
              {{ n }} {{ t('addListing.months') }}
            </option>
          </select>
        </div>
      </div>

      <div class="rounded-lg border border-line p-4 flex flex-col gap-3">
        <p class="text-xs font-semibold uppercase tracking-wide text-ink-3">
          {{ t('types.rent') }}
        </p>
        <FormField
          id="ap-rent-price"
          :label="t('addListing.rentPriceField')"
          v-model="form.rentPrice"
          :error="fieldError('rentPrice')"
          required
          inputmode="numeric"
          placeholder="e.g. 1,200"
          :format="formatPriceInput"
          :parse="parsePriceInput"
          @beforeinput="numericInput"
        />
        <label class="flex items-center gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="form.rentVatIncluded"
            :class="vatCheckboxClass"
          />
          <span class="text-sm text-ink">{{
            t('addListing.vatIncluded')
          }}</span>
        </label>
        <div class="flex flex-col gap-1.5">
          <label for="ap-rent-duration" class="text-sm font-medium text-ink">
            {{ t('addListing.listingDuration') }}
            <span class="text-red-500">*</span>
          </label>
          <select
            id="ap-rent-duration"
            v-model.number="form.rentDurationMonths"
            class="h-10 rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
          >
            <option v-for="n in 6" :key="n" :value="n">
              {{ n }} {{ t('addListing.months') }}
            </option>
          </select>
        </div>
      </div>
    </template>

    <!-- Single mode -->
    <template v-else>
      <FormField
        id="ap-single-price"
        :label="priceLabel"
        v-model="form.price"
        :error="fieldError('price')"
        required
        inputmode="numeric"
        :placeholder="pricePlaceholder"
        :format="formatPriceInput"
        :parse="parsePriceInput"
        @beforeinput="numericInput"
      />
      <label class="flex items-center gap-2.5 cursor-pointer select-none">
        <input
          type="checkbox"
          v-model="form.vatIncluded"
          :class="vatCheckboxClass"
        />
        <span class="text-sm text-ink">{{ t('addListing.vatIncluded') }}</span>
      </label>

      <div v-if="!isEdit" class="flex flex-col gap-1.5">
        <label for="ap-single-duration" class="text-sm font-medium text-ink">
          {{ t('addListing.listingDuration') }}
          <span class="text-red-500">*</span>
        </label>
        <select
          id="ap-single-duration"
          v-model.number="form.durationMonths"
          class="h-10 rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
        >
          <option v-for="n in 6" :key="n" :value="n">
            {{ n }} {{ t('addListing.months') }}
          </option>
        </select>
        <p class="text-xs text-ink-3">{{ t('addListing.durationHint') }}</p>
      </div>
    </template>

    <div v-if="form.type === 'new_project'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addListing.completionStatus') }}
        <span class="text-red-500">*</span>
      </p>
      <ToggleButtons
        :options="completionOptions"
        :model-value="form.completion ?? ''"
        @update:model-value="form.completion = $event as typeof form.completion"
      />
      <p v-if="fieldError('completion')" class="text-xs text-red-500">
        {{ fieldError('completion') }}
      </p>
    </div>
  </section>
</template>
