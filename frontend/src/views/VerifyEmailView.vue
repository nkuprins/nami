<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import IconFail from '../components/icons/IconFail.vue';
import IconCheck from '../components/icons/IconCheck.vue';

const route = useRoute();
const { verifyEmail } = useAuthStore();

const status = ref<'loading' | 'success' | 'error'>('loading');
const errorMsg = ref('');

onMounted(async () => {
  const token = route.query.token as string | undefined;
  if (!token) {
    status.value = 'error';
    errorMsg.value = 'No verification token found in the URL.';
    return;
  }
  const err = await verifyEmail(token);
  if (err) {
    status.value = 'error';
    errorMsg.value = err;
  } else {
    status.value = 'success';
  }
});
</script>

<template>
  <div class="max-w-md mx-auto px-4 py-20 text-center">
    <p v-if="status === 'loading'" class="text-sm text-ink-2">Verifying…</p>

    <div
      v-else-if="status === 'success'"
      class="flex flex-col items-center gap-4"
    >
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconCheck />
      </div>
      <h1 class="text-lg font-medium text-ink">Email verified</h1>
      <p class="text-sm text-ink-2">
        Your email has been confirmed. You can now sign in.
      </p>
      <a href="/" class="text-sm text-ink underline underline-offset-2"
        >Go to home</a
      >
    </div>

    <div v-else class="flex flex-col items-center gap-4">
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconFail />
      </div>
      <h1 class="text-lg font-medium text-ink">Verification FAILED</h1>
      <p class="text-sm text-ink-2">{{ errorMsg }}</p>
      <a href="/" class="text-sm text-ink underline underline-offset-2"
        >Go to home</a
      >
    </div>
  </div>
</template>
