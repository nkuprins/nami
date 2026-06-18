<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';
import IconClose from './IconClose.vue';

const props = defineProps<{ open: boolean; title: string }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

function close() {
  emit('update:open', false);
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close();
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      document.addEventListener('keydown', onKey);
      document.body.style.overflow = 'hidden';
    } else {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    }
  }
);

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey);
  document.body.style.overflow = '';
});
</script>

<template>
  <Teleport to="body">
    <transition name="scrim">
      <div
        v-if="open"
        class="fixed inset-0 z-50 bg-ink/45 backdrop-blur-sm"
        @click="close"
        aria-hidden="true"
      />
    </transition>

    <transition name="drawer-right">
      <aside
        v-if="open"
        class="hidden md:flex fixed inset-y-0 right-0 w-120 z-50 bg-bg shadow-lift border-l border-line flex-col"
      >
        <header
          class="flex items-center justify-between px-6 h-16 border-b border-line"
        >
          <div>
            <p class="micro-label">{{ title }}</p>
            <p class="text-xs text-ink-3 mt-0.5">Esc to close</p>
          </div>
          <button
            type="button"
            class="focus-ring size-10 -mr-2 grid place-items-center text-ink-2 hover:text-ink"
            @click="close"
          >
            <span class="size-5"><IconClose /></span>
          </button>
        </header>
        <div class="flex-1 overflow-y-auto px-6 py-6">
          <slot />
        </div>
        <footer
          v-if="$slots.footer"
          class="border-t border-line px-6 py-4 bg-cream/40"
        >
          <slot name="footer" />
        </footer>
      </aside>
    </transition>

    <transition name="drawer-bottom">
      <aside
        v-if="open"
        class="md:hidden fixed inset-x-0 bottom-0 z-50 max-h-[90vh] bg-bg rounded-t-2xl shadow-lift flex flex-col"
      >
        <header
          class="flex items-center justify-between px-5 pt-4 pb-3 border-b border-line"
        >
          <p class="micro-label">{{ title }}</p>
          <button
            type="button"
            class="focus-ring size-9 -mr-1 grid place-items-center text-ink-2 hover:text-ink"
            @click="close"
          >
            <span class="size-5"><IconClose /></span>
          </button>
        </header>
        <div class="flex-1 overflow-y-auto px-5 py-5">
          <slot />
        </div>
        <footer
          v-if="$slots.footer"
          class="border-t border-line px-5 py-4 bg-cream/40"
        >
          <slot name="footer" />
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>
