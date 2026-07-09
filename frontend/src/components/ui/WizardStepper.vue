<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import IconCheck from '../icons/IconCheck.vue';

const props = defineProps<{
  steps: { id: string; label: string }[];
  currentIndex: number;
  completed: Set<number>;
}>();

const emit = defineEmits<{ jump: [index: number] }>();

const { t } = useI18n();

const progressPercent = computed(() =>
  props.steps.length > 1
    ? (props.currentIndex / (props.steps.length - 1)) * 100
    : 0
);

function stepState(i: number): 'current' | 'done' | 'upcoming' {
  if (i === props.currentIndex) return 'current';
  if (props.completed.has(i)) return 'done';
  return 'upcoming';
}
</script>

<template>
  <!-- Desktop: a vertical rail alongside the step content — the same
       skeleton as the edit page's table-of-contents rail, but progress-gated:
       future steps stay inert until reached, completed ones jump back. This
       decouples the stepper's width from the form column's, so labels can
       wrap instead of forcing either to overflow or over-stretch. -->
  <nav
    class="hidden md:block relative sticky top-24 w-44 shrink-0"
    :aria-label="t('addListing.stepperProgress')"
  >
    <div class="absolute left-[11px] top-3 bottom-3 w-px bg-line" />
    <div
      class="absolute left-[11px] top-3 w-px bg-accent transition-all duration-300"
      :style="{ height: `${progressPercent}%` }"
    />
    <button
      v-for="(step, i) in steps"
      :key="step.id"
      type="button"
      class="focus-ring relative flex w-full items-start gap-3 py-2.5 text-left"
      :class="completed.has(i) ? 'cursor-pointer' : 'cursor-default'"
      :disabled="!completed.has(i) && i !== currentIndex"
      @click="emit('jump', i)"
    >
      <span
        class="relative z-10 size-[22px] shrink-0 rounded-full grid place-items-center font-mono text-[0.65rem] transition-colors"
        :class="{
          'bg-ink text-bg': stepState(i) === 'current',
          'bg-bg border border-accent text-accent-2': stepState(i) === 'done',
          'bg-bg border border-line text-ink-3': stepState(i) === 'upcoming',
        }"
      >
        <IconCheck v-if="stepState(i) === 'done'" class="size-3" />
        <template v-else>{{ String(i + 1).padStart(2, '0') }}</template>
      </span>
      <span
        class="micro-label pt-0.5 leading-snug"
        :class="stepState(i) === 'upcoming' ? 'text-ink-3' : 'text-ink'"
      >
        {{ step.label }}
      </span>
    </button>
  </nav>

  <!-- Mobile: condensed "Step N of T" + a thin progress bar -->
  <div class="md:hidden w-full flex flex-col gap-2">
    <p class="micro-label">
      {{
        t('addListing.stepOfTotal', {
          current: currentIndex + 1,
          total: steps.length,
          label: steps[currentIndex].label,
        })
      }}
    </p>
    <div class="h-1 rounded-full bg-line overflow-hidden">
      <div
        class="h-full bg-accent transition-all duration-300"
        :style="{ width: `${progressPercent}%` }"
      />
    </div>
  </div>
</template>
