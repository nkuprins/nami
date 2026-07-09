import { nextTick, type Ref } from 'vue';
import { useWizardSteps } from './useWizardSteps';
import { stepHasErrors, type ListingWizardStep } from './useWizardStepValidity';

// Wraps `useWizardSteps` with the Continue/Back/jump handlers shared verbatim
// by the create-listing and add-listing-to-property wizards. The step-error
// gating reads the same flat `errors` record every form composable produces,
// and reveals a step's red errors by flipping the caller's `touched` flag.
export function useWizardNavigation(
  steps: ListingWizardStep[],
  errors: Ref<Record<string, string>>,
  touched: Ref<boolean>
) {
  const wizard = useWizardSteps(steps);

  async function handleContinue() {
    if (stepHasErrors(wizard.currentStep.value, errors.value)) {
      touched.value = true;
      await nextTick();
      document
        .querySelector(
          `[data-step="${wizard.currentStep.value}"] .text-red-500`
        )
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    touched.value = false;
    wizard.next();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function handleBack() {
    wizard.back();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function jumpToStep(step: ListingWizardStep) {
    wizard.goTo(steps.indexOf(step));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // A step marked "completed" can go stale: editing an earlier step (e.g.
  // changing the transaction type) can invalidate a later one that was already
  // visited. Re-validate every step up to the jump target so the stepper rail
  // can't be used to skip past a now-broken step straight to Confirm.
  function handleJump(index: number) {
    if (index > wizard.currentIndex.value) {
      const invalidStep = steps
        .slice(0, index)
        .find((step) => stepHasErrors(step, errors.value));
      if (invalidStep) {
        touched.value = true;
        wizard.goTo(steps.indexOf(invalidStep));
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return;
      }
    }
    wizard.goTo(index);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  return { wizard, handleContinue, handleBack, jumpToStep, handleJump };
}
