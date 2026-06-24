<script setup lang="ts">
import { ref } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import IconCheck from '../components/icons/IconCheck.vue';
import FormField from '../components/ui/FormField.vue';

const route = useRoute();
const { resetPassword } = useAuthStore();

const password = ref('');
const confirm = ref('');
const error = ref('');
const submitting = ref(false);
const done = ref(false);

const token = route.query.token as string | undefined;

async function submit() {
  error.value = '';
  if (!password.value) {
    error.value = 'Please enter a new password.';
    return;
  }
  if (password.value.length < 15) {
    error.value = 'Password must be at least 15 characters.';
    return;
  }
  if (password.value !== confirm.value) {
    error.value = 'Passwords do not match.';
    return;
  }
  if (!token) {
    error.value = 'Invalid reset link.';
    return;
  }

  submitting.value = true;
  const err = await resetPassword(token, password.value);
  submitting.value = false;

  if (err) {
    error.value = err;
    return;
  }
  done.value = true;
}
</script>

<template>
  <div class="max-w-md mx-auto px-4 py-20">
    <div v-if="done" class="text-center flex flex-col items-center gap-4">
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconCheck />
      </div>
      <h1 class="text-lg font-medium text-ink">Password updated</h1>
      <p class="text-sm text-ink-2">
        You can now sign in with your new password.
      </p>
      <RouterLink to="/" class="text-sm text-ink underline underline-offset-2"
        >Go to home
      </RouterLink>
    </div>

    <div v-else>
      <h1 class="text-lg font-medium text-ink mb-6">Set new password</h1>
      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          id="rp-password"
          label="New password"
          v-model="password"
          type="password"
          autocomplete="new-password"
          placeholder="••••••••••••••••"
        />
        <FormField
          id="rp-confirm"
          label="Confirm password"
          v-model="confirm"
          type="password"
          autocomplete="new-password"
          placeholder="••••••••••••••••"
        />
        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
        <button
          type="submit"
          :disabled="submitting"
          class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
        >
          Update password
        </button>
      </form>
    </div>
  </div>
</template>
