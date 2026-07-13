import { ref, watch, type Ref } from 'vue';
import type { Location } from '../../../data/rawLocations';
import type { ListingFormState } from './formTypes';
import { LISTING_WIZARD_STEPS } from './useWizardStepValidity';

// Persists the create-listing wizard so a reload (or coming back later) resumes
// where the user left off. Photos are deliberately out of scope — they're
// un-uploaded File blobs that can't be serialized — so the restored position is
// clamped to at most the Photos step and any completed marks from there on are
// dropped: everything before Photos is kept, and the user re-adds photos.
const DRAFT_KEY = 'baltnami:add-listing-draft';
// v2: free-text address replaced by street/building register picks + apartment.
// v3: phones entries changed from plain strings to {phone, name, email} objects.
const DRAFT_VERSION = 3;
const SAVE_DEBOUNCE_MS = 400;
const PHOTOS_STEP = LISTING_WIZARD_STEPS.indexOf('photos');

interface DraftSnapshot {
  version: number;
  form: ListingFormState;
  location: Location | null;
  stepIndex: number;
  completed: number[];
}

export function useListingDraft(
  form: ListingFormState,
  selectedLocation: Ref<Location | null>,
  wizard: {
    currentIndex: Ref<number>;
    completed: Ref<Set<number>>;
  }
) {
  const hasDraft = ref(false);

  restore();

  function restore() {
    let snap: DraftSnapshot;
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (!raw) return;
      snap = JSON.parse(raw);
    } catch {
      return;
    }
    if (snap.version !== DRAFT_VERSION) {
      clear();
      return;
    }
    Object.assign(form, snap.form);
    selectedLocation.value = snap.location;
    wizard.currentIndex.value = Math.min(snap.stepIndex ?? 0, PHOTOS_STEP);
    wizard.completed.value = new Set(
      (snap.completed ?? []).filter((i) => i < PHOTOS_STEP)
    );
    hasDraft.value = true;
  }

  function clear() {
    hasDraft.value = false;
    try {
      localStorage.removeItem(DRAFT_KEY);
    } catch {
      // Ignore — a blocked localStorage just means no draft is kept.
    }
  }

  let timer: ReturnType<typeof setTimeout> | undefined;
  watch(
    [form, selectedLocation, wizard.currentIndex, wizard.completed],
    () => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        const snap: DraftSnapshot = {
          version: DRAFT_VERSION,
          form,
          location: selectedLocation.value,
          stepIndex: wizard.currentIndex.value,
          completed: [...wizard.completed.value],
        };
        try {
          localStorage.setItem(DRAFT_KEY, JSON.stringify(snap));
          hasDraft.value = true;
        } catch {
          // Ignore — quota/private mode; the draft just won't be kept.
        }
      }, SAVE_DEBOUNCE_MS);
    },
    { deep: true }
  );

  return { clear, hasDraft };
}
