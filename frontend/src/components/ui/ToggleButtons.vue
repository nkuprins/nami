<script setup lang="ts">
const props = defineProps<{
  options: Array<{ id: string; label: string; hint?: string }>;
  modelValue: string | string[];
  multiple?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string | string[]];
}>();

function isSelected(id: string): boolean {
  return Array.isArray(props.modelValue)
    ? props.modelValue.includes(id)
    : props.modelValue === id;
}

function toggle(id: string) {
  if (props.multiple) {
    const current = props.modelValue as string[];
    emit(
      'update:modelValue',
      current.includes(id) ? current.filter((v) => v !== id) : [...current, id]
    );
  } else {
    emit('update:modelValue', props.modelValue === id ? '' : id);
  }
}
</script>

<template>
  <div class="flex gap-2 flex-wrap">
    <button
      v-for="opt in options"
      :key="opt.id"
      type="button"
      :title="opt.hint"
      class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
      :class="
        isSelected(opt.id)
          ? 'bg-ink text-bg border-ink'
          : 'border-line-2 text-ink-2 hover:border-ink-3 hover:text-ink'
      "
      @click="toggle(opt.id)"
    >
      {{ opt.label }}
    </button>
  </div>
</template>
