<script setup lang="ts">
import FormField from '../../../components/ui/FormField.vue';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{
  form: PropertyFormState;
  fieldError: (field: string) => string | undefined;
}>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Basic info
    </h2>

    <FormField
      id="ap-title"
      label="Title"
      v-model="form.title"
      :error="fieldError('title')"
      required
      placeholder="e.g. Bright 3-room apartment in Centrs"
    />

    <div class="flex flex-col gap-1.5">
      <label class="text-sm font-medium text-ink" for="ap-description"
        >Description</label
      >
      <textarea
        id="ap-description"
        v-model="form.description"
        rows="4"
        placeholder="Describe the property…"
        class="px-3 py-2.5 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 resize-none focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
      />
    </div>
  </section>
</template>
