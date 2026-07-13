<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import PricingSection from '../PricingSection.vue';
import PhonesSection from '../PhonesSection.vue';
import FormField from '../../../../components/ui/FormField.vue';
import TurnstileWidget from '../../../../components/ui/TurnstileWidget.vue';
import type { ListingFormState } from '../../composables/formTypes';

const { t } = useI18n();

const form = defineModel<ListingFormState>('form', { required: true });
const turnstileToken = defineModel<string>('turnstileToken', {
  default: '',
});
defineProps<{
  fieldError: (field: string) => string | undefined;
  turnstileEnabled: boolean;
}>();

defineEmits<{
  (e: 'add-phone'): void;
  (e: 'remove-phone', index: number): void;
}>();

const turnstileWidget = ref<InstanceType<typeof TurnstileWidget> | null>(null);

function resetTurnstile() {
  turnstileWidget.value?.reset();
}

defineExpose({ resetTurnstile });
</script>

<template>
  <div class="flex flex-col gap-10">
    <PricingSection
      v-model:form="form"
      :field-error="fieldError"
      :is-edit="false"
    />
    <PhonesSection
      v-model:form="form"
      :field-error="fieldError"
      @add-phone="$emit('add-phone')"
      @remove-phone="$emit('remove-phone', $event)"
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
    <TurnstileWidget
      v-if="turnstileEnabled"
      ref="turnstileWidget"
      v-model="turnstileToken"
    />
  </div>
</template>
