<script setup lang="ts">
defineProps<{
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  confirm: [];
}>();
</script>

<template>
  <Teleport to="body">
    <Transition name="scrim">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-4"
      >
        <!-- Scrim -->
        <div
          class="absolute inset-0 bg-ink/40 backdrop-blur-sm"
          @click="emit('update:open', false)"
        />

        <!-- Dialog -->
        <dialog
          open
          aria-modal="true"
          class="relative z-10 w-full max-w-sm bg-bg rounded-2xl shadow-lift border border-line p-6 flex flex-col gap-5"
        >
          <div class="flex flex-col gap-1.5">
            <h2 class="text-base font-semibold text-ink">{{ title }}</h2>
            <p v-if="description" class="text-sm text-ink-2 leading-relaxed">
              {{ description }}
            </p>
            <slot />
          </div>

          <div class="flex gap-2 justify-end">
            <button
              class="focus-ring h-9 px-4 rounded-lg border border-line text-sm text-ink-2 hover:bg-surface transition-colors"
              @click="emit('update:open', false)"
            >
              {{ cancelLabel ?? 'Cancel' }}
            </button>
            <button
              class="focus-ring h-9 px-4 rounded-lg text-sm font-medium transition-colors"
              :class="
                danger
                  ? 'bg-warn text-bg hover:bg-warn/90'
                  : 'bg-ink text-bg hover:bg-accent-2'
              "
              @click="emit('confirm')"
            >
              {{ confirmLabel ?? 'Confirm' }}
            </button>
          </div>
        </dialog>
      </div>
    </Transition>
  </Teleport>
</template>
