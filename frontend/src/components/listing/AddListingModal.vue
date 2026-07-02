<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../ui/Drawer.vue';
import FormField from '../ui/FormField.vue';
import ToggleButtons from '../ui/ToggleButtons.vue';
import IconClose from '../icons/IconClose.vue';
import IconSpinner from '../icons/IconSpinner.vue';
import { addListing, ListingTypeExistsError } from '../../api/listingsApi';
import { usePropertyLabels } from '../../composables/usePropertyLabels';
import { decimalInput } from '../../utils/utils';
import {
  compatibleListingTypes,
  type ListingType,
  type PropertyCompletion,
  type Translations,
} from '../../types/listingItem';

const props = defineProps<{
  open: boolean;
  propertyId: string;
  alreadyHas: ListingType[];
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  added: [];
}>();

const { t } = useI18n();
const { typeOptions, completionOptions } = usePropertyLabels();

const availableTypeOptions = computed(() => {
  const compatible = new Set(compatibleListingTypes(props.alreadyHas));
  return typeOptions.value.filter((o) => compatible.has(o.id));
});

const form = reactive({
  type: '' as ListingType | '',
  price: '',
  vatIncluded: false,
  titleLv: '',
  titleEn: '',
  titleRu: '',
  descriptionLv: '',
  descriptionEn: '',
  descriptionRu: '',
  phones: [''],
  completion: '' as PropertyCompletion | '',
  durationMonths: 3,
});

watch(
  () => props.open,
  (isOpen) => {
    if (!isOpen) return;
    form.type = availableTypeOptions.value[0]?.id ?? '';
    form.price = '';
    form.vatIncluded = false;
    form.titleLv = '';
    form.titleEn = '';
    form.titleRu = '';
    form.descriptionLv = '';
    form.descriptionEn = '';
    form.descriptionRu = '';
    form.phones = [''];
    form.completion = '';
    form.durationMonths = 3;
    submitError.value = '';
  }
);

function addPhone() {
  form.phones.push('');
}
function removePhone(i: number) {
  form.phones.splice(i, 1);
}

const submitting = ref(false);
const submitError = ref('');

function parseDecimal(v: string): number {
  return Number(v.replace(',', '.'));
}

function buildTranslations(): Translations {
  const translations: Translations = {};
  if (form.titleLv.trim())
    translations.lv = {
      title: form.titleLv.trim(),
      description: form.descriptionLv.trim() || undefined,
    };
  if (form.titleEn.trim())
    translations.en = {
      title: form.titleEn.trim(),
      description: form.descriptionEn.trim() || undefined,
    };
  if (form.titleRu.trim())
    translations.ru = {
      title: form.titleRu.trim(),
      description: form.descriptionRu.trim() || undefined,
    };
  return translations;
}

const isValid = computed(() => {
  if (!form.type) return false;
  if (
    !form.price ||
    isNaN(parseDecimal(form.price)) ||
    parseDecimal(form.price) <= 0
  )
    return false;
  if (!form.titleLv.trim() && !form.titleEn.trim() && !form.titleRu.trim())
    return false;
  if (!form.phones.some((p) => p.trim())) return false;
  if (form.type === 'new_project' && !form.completion) return false;
  return true;
});

async function submit() {
  if (!isValid.value) return;
  submitting.value = true;
  submitError.value = '';
  try {
    await addListing(props.propertyId, {
      type: form.type as ListingType,
      price: {
        amount: parseDecimal(form.price),
        vatIncluded: form.vatIncluded || undefined,
      },
      translations: buildTranslations(),
      phones: form.phones.filter((p) => p.trim()),
      completion:
        form.type === 'new_project' && form.completion
          ? form.completion
          : undefined,
      durationMonths: form.durationMonths,
    });
    emit('added');
    emit('update:open', false);
  } catch (e) {
    submitError.value =
      e instanceof ListingTypeExistsError
        ? t('drawers.addListingTypeExists')
        : t('drawers.addListingFailed');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Drawer
    :open="open"
    :title="t('drawers.addListingTypeTitle')"
    @update:open="emit('update:open', $event)"
  >
    <form class="flex flex-col gap-6" @submit.prevent="submit">
      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">
          {{ t('addListing.transactionType') }}
        </p>
        <ToggleButtons
          :options="availableTypeOptions"
          :model-value="form.type"
          @update:model-value="form.type = $event as typeof form.type"
        />
      </div>

      <div class="flex flex-col gap-3">
        <FormField
          id="al-price"
          :label="t('addListing.priceField')"
          v-model="form.price"
          required
          inputmode="decimal"
          placeholder="e.g. 185000"
          @beforeinput="decimalInput"
        />
        <label class="flex items-center gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            v-model="form.vatIncluded"
            class="size-4 rounded border-line accent-ink cursor-pointer"
          />
          <span class="text-sm text-ink">{{
            t('addListing.vatIncluded')
          }}</span>
        </label>
      </div>

      <div class="flex flex-col gap-3">
        <FormField
          id="al-title-lv"
          :label="`${t('addListing.titleLabel')} (LV)`"
          v-model="form.titleLv"
        />
        <FormField
          id="al-title-en"
          :label="`${t('addListing.titleLabel')} (EN)`"
          v-model="form.titleEn"
        />
        <FormField
          id="al-title-ru"
          :label="`${t('addListing.titleLabel')} (RU)`"
          v-model="form.titleRu"
        />
      </div>

      <div class="flex flex-col gap-2">
        <p class="text-sm font-medium text-ink">
          {{ t('addListing.phoneSection') }}
        </p>
        <div
          v-for="(_, i) in form.phones"
          :key="i"
          class="grid grid-cols-[1fr_auto] items-center gap-2"
        >
          <FormField
            :id="`al-phone-${i}`"
            label=""
            v-model="form.phones[i]"
            type="tel"
            placeholder="+371 29 123 456"
          />
          <button
            v-if="form.phones.length > 1"
            type="button"
            class="size-5 shrink-0 text-ink-3 hover:text-red-500 transition-colors"
            @click="removePhone(i)"
          >
            <IconClose />
          </button>
        </div>
        <button
          type="button"
          class="self-start text-sm text-ink-2 hover:text-ink transition-colors"
          @click="addPhone"
        >
          {{ t('addListing.anotherPhone') }}
        </button>
      </div>

      <div v-if="form.type === 'new_project'" class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">
          {{ t('addListing.completionStatus') }}
        </p>
        <ToggleButtons
          :options="completionOptions"
          :model-value="form.completion"
          @update:model-value="
            form.completion = $event as typeof form.completion
          "
        />
      </div>

      <div class="flex flex-col gap-1.5">
        <label for="al-duration" class="text-sm font-medium text-ink">
          {{ t('addListing.listingDuration') }}
        </label>
        <select
          id="al-duration"
          v-model.number="form.durationMonths"
          class="h-10 rounded-lg border border-line bg-bg px-3 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-accent-2"
        >
          <option v-for="n in 6" :key="n" :value="n">
            {{ n }} {{ t('addListing.months') }}
          </option>
        </select>
      </div>

      <p v-if="submitError" class="text-sm text-red-500">{{ submitError }}</p>

      <button
        type="submit"
        :disabled="!isValid || submitting"
        class="h-11 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
      >
        <IconSpinner v-if="submitting" class="size-4" />
        {{ t('drawers.addListingTypeSubmit') }}
      </button>
    </form>
  </Drawer>
</template>
