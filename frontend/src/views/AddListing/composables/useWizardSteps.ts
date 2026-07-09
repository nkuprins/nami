import { computed, ref } from 'vue';

// Generic linear step state, shared by the create-listing and
// add-listing-to-property wizards. Validation is intentionally not this
// composable's concern — the calling view decides whether the current step
// is valid before calling `next()`, and marks it `touched` itself to reveal
// errors when it isn't.
export function useWizardSteps<T extends string>(steps: readonly T[]) {
  const currentIndex = ref(0);
  const completed = ref(new Set<number>());

  const currentStep = computed(() => steps[currentIndex.value]);
  const isFirst = computed(() => currentIndex.value === 0);
  const isLast = computed(() => currentIndex.value === steps.length - 1);

  function next() {
    completed.value.add(currentIndex.value);
    if (!isLast.value) currentIndex.value++;
  }

  function back() {
    if (!isFirst.value) currentIndex.value--;
  }

  // Lets the calling view revoke a step's completed mark when an earlier
  // step's edit invalidates it (e.g. changing the transaction type leaves
  // stale pricing behind), instead of only catching it on the next jump.
  function uncomplete(index: number) {
    completed.value.delete(index);
  }

  // Jumping forward is only allowed into a step reached before (Confirm's
  // per-field "Edit" links, or clicking a completed step in the rail).
  function goTo(index: number) {
    if (index < 0 || index >= steps.length) return;
    if (index > currentIndex.value && !completed.value.has(index - 1)) return;
    currentIndex.value = index;
  }

  return {
    currentIndex,
    currentStep,
    isFirst,
    isLast,
    completed,
    next,
    back,
    goTo,
    uncomplete,
  };
}
