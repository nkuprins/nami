<script setup lang="ts">
import { ref } from 'vue';
import PricingSection from '../PricingSection.vue';
import PhonesSection from '../PhonesSection.vue';
import TurnstileWidget from '../../../../components/ui/TurnstileWidget.vue';
import type { ListingFormState } from '../../composables/formTypes';

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
    <TurnstileWidget
      v-if="turnstileEnabled"
      ref="turnstileWidget"
      v-model="turnstileToken"
    />
  </div>
</template>
