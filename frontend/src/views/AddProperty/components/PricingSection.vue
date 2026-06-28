<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePropertyLabels } from '../../../composables/usePropertyLabels';
import { decimalInput } from '../../../utils/utils';
import FormField from '../../../components/ui/FormField.vue';
import ToggleButtons from '../../../components/ui/ToggleButtons.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

const { t } = useI18n();
const { completionOptions } = usePropertyLabels();

const props = defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
  isEdit: boolean;
}>();

const priceLabel = computed(() =>
  props.form.type === 'rent'
    ? t('addProperty.rentPriceField')
    : t('addProperty.priceField')
);

const pricePlaceholder = computed(() =>
  props.form.type === 'rent' ? 'e.g. 1200' : 'e.g. 185000'
);

const isDual = computed(() => props.form.type === 'buy' && props.form.alsoRent);

const vatCheckboxClass = 'size-4 rounded border-line accent-ink cursor-pointer';
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addProperty.pricing') }}
    </h2>

    <!-- Dual mode: two labeled price cards -->
    <template v-if="isDual">
      <div class="rounded-lg border border-line p-4 flex flex-col gap-3">
        <p class="text-xs font-semibold uppercase tracking-wide text-ink-3">
          {{ t('types.buy') }}
        </p>
        <FormField
          id="ap-price"
          :label="t('addProperty.priceField')"
          v-model="form.price"
          :error="fieldError('price')"
          required
          inputmode="decimal"
          placeholder="e.g. 185000"
          @beforeinput="decimalInput"
        />
        <label class="flex items-center gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="form.buyVatIncluded"
            :class="vatCheckboxClass"
          />
          <span class="text-sm text-ink">{{
            t('addProperty.buyVatIncluded')
          }}</span>
        </label>
      </div>

      <div class="rounded-lg border border-line p-4 flex flex-col gap-3">
        <p class="text-xs font-semibold uppercase tracking-wide text-ink-3">
          {{ t('types.rent') }} / {{ t('addProperty.months') }}
        </p>
        <FormField
          id="ap-rent-price"
          :label="t('addProperty.rentPriceField')"
          v-model="form.rentPrice"
          :error="fieldError('rentPrice')"
          required
          inputmode="decimal"
          placeholder="e.g. 1200"
          @beforeinput="decimalInput"
        />
        <label class="flex items-center gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="form.rentVatIncluded"
            :class="vatCheckboxClass"
          />
          <span class="text-sm text-ink">{{
            t('addProperty.rentVatIncluded')
          }}</span>
        </label>
      </div>
    </template>

    <!-- Single mode -->
    <template v-else>
      <FormField
        id="ap-price"
        :label="priceLabel"
        v-model="form.price"
        :error="fieldError('price')"
        required
        inputmode="decimal"
        :placeholder="pricePlaceholder"
        @beforeinput="decimalInput"
      />
      <label class="flex items-center gap-2.5 cursor-pointer select-none">
        <input
          type="checkbox"
          v-model="form.buyVatIncluded"
          :class="vatCheckboxClass"
        />
        <span class="text-sm text-ink">{{
          t('addProperty.buyVatIncluded')
        }}</span>
      </label>
    </template>

    <div v-if="!isEdit" class="flex flex-col gap-1.5">
      <label for="ap-duration" class="text-sm font-medium text-ink">
        {{ t('addProperty.listingDuration') }}
        <span class="text-red-500">*</span>
      </label>
      <select
        id="ap-duration"
        v-model.number="form.durationMonths"
        class="h-10 rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
      >
        <option v-for="n in 6" :key="n" :value="n">
          {{ n }} {{ t('addProperty.months') }}
        </option>
      </select>
      <p class="text-xs text-ink-3">{{ t('addProperty.durationHint') }}</p>
    </div>

    <div v-if="form.type === 'new_project'" class="flex flex-col gap-1.5">
      <p class="text-sm font-medium text-ink">
        {{ t('addProperty.completionStatus') }}
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
