<script setup lang="ts">
import {nextTick, onBeforeUnmount, ref, watch} from 'vue';
import IconClose from './IconClose.vue';

const props = defineProps<{
  open: boolean;
  anchorEl: HTMLElement | null;
  title?: string;
  align?: 'start' | 'center' | 'end';
  width?: number;
}>();

const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const sheetRef = ref<HTMLDivElement | null>(null);
const popRef = ref<HTMLDivElement | null>(null);
const styleObj = ref<Record<string, string>>({});

function position() {
  if (!props.anchorEl || !popRef.value) return;
  const r = props.anchorEl.getBoundingClientRect();
  const w = props.width ?? 320;
  const align = props.align ?? 'start';
  let left = r.left;
  if (align === 'center') left = r.left + r.width / 2 - w / 2;
  if (align === 'end') left = r.right - w;
  left = Math.max(16, Math.min(left, window.innerWidth - w - 16));
  const top = r.bottom + 8;
  styleObj.value = {
    position: 'fixed',
    top: `${top}px`,
    left: `${left}px`,
    width: `${w}px`,
    zIndex: '60',
  };
}

function close() {
  emit('update:open', false);
}

function onDocClick(e: MouseEvent) {
  const t = e.target as Node;
  if (popRef.value?.contains(t) || props.anchorEl?.contains(t)) return;
  if (sheetRef.value?.contains(t)) return;
  close();
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close();
}

watch(
    () => props.open,
    async (val) => {
      if (val) {
        await nextTick();
        position();
        document.addEventListener('mousedown', onDocClick, true);
        document.addEventListener('keydown', onKey, true);
        window.addEventListener('resize', position);
        window.addEventListener('scroll', position, true);
      } else {
        document.removeEventListener('mousedown', onDocClick, true);
        document.removeEventListener('keydown', onKey, true);
        window.removeEventListener('resize', position);
        window.removeEventListener('scroll', position, true);
      }
    },
);

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocClick, true);
  document.removeEventListener('keydown', onKey, true);
  window.removeEventListener('resize', position);
  window.removeEventListener('scroll', position, true);
});
</script>

<template>
  <Teleport to="body">
    <transition name="fade">
      <div
          v-if="open"
          ref="sheetRef"
          class="md:hidden fixed inset-0 z-50 flex flex-col justify-end bg-ink/40 backdrop-blur-sm"
          @click.self="close"
      >
        <div class="bg-bg rounded-t-2xl shadow-lift max-h-[80vh] flex flex-col">
          <div class="flex items-center justify-between px-5 pt-4 pb-3 border-b border-line">
            <p class="micro-label">{{ title ?? 'Filter' }}</p>
            <button
                class="focus-ring size-9 grid place-items-center -mr-2 text-ink-2 hover:text-ink"
                @click="close"
                aria-label="Close"
            >
              <span class="size-4 inline-block"><IconClose/></span>
            </button>
          </div>
          <div class="overflow-y-auto p-5">
            <slot/>
          </div>
        </div>
      </div>
    </transition>
    <transition name="pop">
      <div
          v-if="open"
          ref="popRef"
          :style="styleObj"
          class="hidden md:block bg-bg border border-line rounded-xl shadow-lift p-4"
      >
        <p v-if="title" class="micro-label mb-3">{{ title }}</p>
        <slot/>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 180ms ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.pop-enter-active, .pop-leave-active {
  transition: opacity 160ms ease, transform 200ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.pop-enter-from, .pop-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>