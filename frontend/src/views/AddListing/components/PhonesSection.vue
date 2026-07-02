<script setup lang="ts">
import FormField from '../../../components/ui/FormField.vue';
import IconClose from '../../../components/icons/IconClose.vue';
import type { ListingFieldsForm } from '../composables/formTypes';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

defineProps<{
  form: ListingFieldsForm;
  fieldError: (field: string) => string | undefined;
}>();

defineEmits<{
  (e: 'add-phone'): void;
  (e: 'remove-phone', index: number): void;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      {{ t('addListing.phoneSection') }}<span class="text-red-500"> *</span>
    </h2>

    <div
      v-for="(_, index) in form.phones"
      :key="index"
      class="sm:grid sm:grid-cols-2 gap-4"
    >
      <div class="flex flex-col gap-1.5 w-full">
        <label v-if="index === 0" class="text-sm font-medium text-ink">
          {{ t('addListing.phoneNumber') }}
        </label>

        <div class="grid grid-cols-[1fr_auto] items-center gap-2 w-full">
          <FormField
            :id="`ap-phone-${index}`"
            :label="''"
            v-model="form.phones[index]"
            :error="
              fieldError(`phone_${index}`) ||
              (index === 0 ? fieldError('phones') : '')
            "
            type="tel"
            placeholder="+371 29 123 456"
            class="w-full"
          />
          <button
            v-if="form.phones.length > 1"
            type="button"
            class="size-5 shrink-0 text-ink-3 hover:text-red-500 transition-colors"
            @click="$emit('remove-phone', index)"
          >
            <IconClose />
          </button>
        </div>
      </div>
    </div>

    <button
      type="button"
      class="self-start text-sm text-ink-2 hover:text-ink transition-colors"
      @click="$emit('add-phone')"
    >
      {{ t('addListing.anotherPhone') }}
    </button>
  </section>
</template>
