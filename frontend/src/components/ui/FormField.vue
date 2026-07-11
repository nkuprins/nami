<script setup lang="ts">
defineOptions({ inheritAttrs: false });

const props = defineProps<{
  label: string;
  modelValue: string;
  error?: string;
  required?: boolean;
  type?: string;
  placeholder?: string;
  id?: string;
  // Optional display/parse pair for fields whose typed value differs from
  // what's shown (e.g. grouping digits with thousands separators).
  format?: (value: string) => string;
  parse?: (raw: string) => string;
}>();

const emit = defineEmits<{ 'update:modelValue': [value: string] }>();

function onInput(e: Event) {
  const raw = (e.target as HTMLInputElement).value;
  emit('update:modelValue', props.parse ? props.parse(raw) : raw);
}
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label v-if="label" class="text-sm font-medium text-ink" :for="id">
      {{ label }}<span v-if="required" class="text-warn"> *</span>
    </label>
    <input
      :id="id"
      :value="format ? format(modelValue) : modelValue"
      @input="onInput"
      :type="type ?? 'text'"
      :placeholder="placeholder"
      class="h-10 px-3 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors disabled:bg-surface disabled:text-ink-3 disabled:cursor-not-allowed disabled:border-line"
      :class="error ? 'border-warn/40 bg-warn/5' : 'border-line-2 bg-bg'"
      v-bind="$attrs"
    />
    <p v-if="error" class="text-xs text-warn">{{ error }}</p>
  </div>
</template>
