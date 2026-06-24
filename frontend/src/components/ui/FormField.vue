<script setup lang="ts">
defineOptions({ inheritAttrs: false });

defineProps<{
  label: string;
  modelValue: string;
  error?: string;
  required?: boolean;
  type?: string;
  placeholder?: string;
  id?: string;
}>();

defineEmits<{ 'update:modelValue': [value: string] }>();
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label v-if="label" class="text-sm font-medium text-ink" :for="id">
      {{ label }}<span v-if="required" class="text-red-500"> *</span>
    </label>
    <input
      :id="id"
      :value="modelValue"
      @input="
        $emit('update:modelValue', ($event.target as HTMLInputElement).value)
      "
      :type="type ?? 'text'"
      :placeholder="placeholder"
      class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
      :class="error ? 'border-red-400 bg-red-50' : 'border-line bg-bg'"
      v-bind="$attrs"
    />
    <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
  </div>
</template>
