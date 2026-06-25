<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import Drawer from '../ui/Drawer.vue';
import FormField from '../ui/FormField.vue';
import { useAuthStore } from '../../stores/authStore';
import {
  MIN_PASSWORD_LENGTH,
  ERROR_EMAIL_NOT_VERIFIED,
} from '../../api/authApi';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const auth = useAuthStore();
const { login, signup, forgotPassword, resendVerification } = auth;

type Mode = 'signin' | 'signup' | 'forgot';
const tab = ref<'signin' | 'signup'>('signin');
const mode = ref<Mode>('signin');
const error = ref('');
const pendingVerification = ref(false);
const forgotSent = ref(false);
const submitting = ref(false);
const unverifiedEmail = ref('');
const verificationResent = ref(false);
const agreedToTerms = ref(false);
const termsError = ref(false);

const name = ref('');
const email = ref('');
const password = ref('');

function reset() {
  name.value = '';
  email.value = '';
  password.value = '';
  error.value = '';
  tab.value = 'signin';
  mode.value = 'signin';
  pendingVerification.value = false;
  forgotSent.value = false;
  unverifiedEmail.value = '';
  agreedToTerms.value = false;
  termsError.value = false;
}

function close() {
  emit('update:open', false);
  reset();
}

function switchTab(t: 'signin' | 'signup') {
  tab.value = t;
  mode.value = t;
  error.value = '';
  termsError.value = false;
}

async function submit() {
  error.value = '';
  submitting.value = true;
  try {
    if (mode.value === 'forgot') {
      if (!email.value) {
        error.value = 'Please enter your email.';
        return;
      }
      await forgotPassword(email.value);
      forgotSent.value = true;
      return;
    }

    if (tab.value === 'signin') {
      if (!email.value || !password.value) {
        error.value = 'Please fill in all fields.';
        return;
      }
      const result = await login(email.value, password.value);
      if (result === null) {
        close();
        return;
      }
      if (result === ERROR_EMAIL_NOT_VERIFIED) {
        unverifiedEmail.value = email.value;
        error.value = ERROR_EMAIL_NOT_VERIFIED;
        return;
      }
      error.value = result;
    } else {
      if (!name.value || !email.value || !password.value) {
        error.value = 'Please fill in all fields.';
        return;
      }
      if (password.value.length < MIN_PASSWORD_LENGTH) {
        error.value = `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
        return;
      }
      if (!agreedToTerms.value) {
        termsError.value = true;
        return;
      }
      const result = await signup(name.value, email.value, password.value);
      if (typeof result === 'object') {
        pendingVerification.value = true;
        return;
      }
      error.value = result;
    }
  } finally {
    submitting.value = false;
  }
}

async function handleResend() {
  await resendVerification(unverifiedEmail.value);
  verificationResent.value = true;
}
</script>

<template>
  <Drawer
    :open="open"
    :title="
      pendingVerification
        ? 'Check your email'
        : mode === 'forgot'
          ? 'Reset password'
          : tab === 'signin'
            ? 'Sign in'
            : 'Create account'
    "
    @update:open="close"
  >
    <!-- Verification pending -->
    <div
      v-if="pendingVerification"
      class="flex flex-col gap-4 text-sm text-ink-2"
    >
      <p>
        We sent a verification link to
        <span class="text-ink font-medium">{{ email }}</span
        >.
      </p>
      <p>Click the link in the email to activate your account, then sign in.</p>
      <button
        type="button"
        class="text-ink underline underline-offset-2 self-start"
        @click="switchTab('signin')"
      >
        Back to sign in
      </button>
    </div>

    <!-- Forgot password -->
    <div v-else-if="mode === 'forgot'" class="flex flex-col gap-6">
      <div v-if="forgotSent" class="text-sm text-ink-2">
        <p>
          If that email is registered, we've sent a reset link. Check your
          inbox.
        </p>
        <button
          type="button"
          class="mt-4 text-ink underline underline-offset-2"
          @click="switchTab('signin')"
        >
          Back to sign in
        </button>
      </div>
      <form v-else class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          id="forgot-email"
          label="Email"
          v-model="email"
          type="email"
          autocomplete="email"
          placeholder="you@example.com"
        />
        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
        <button
          type="submit"
          :disabled="submitting"
          class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
        >
          Send reset link
        </button>
        <button
          type="button"
          class="text-sm text-ink-3 underline underline-offset-2"
          @click="switchTab('signin')"
        >
          Back to sign in
        </button>
      </form>
    </div>

    <!-- Sign in / Sign up -->
    <div v-else class="flex flex-col gap-6">
      <div class="flex rounded-lg border border-line overflow-hidden text-sm">
        <button
          type="button"
          class="flex-1 h-9 font-medium transition-colors"
          :class="
            tab === 'signin'
              ? 'bg-ink text-bg'
              : 'text-ink-2 hover:text-ink hover:bg-surface'
          "
          @click="switchTab('signin')"
        >
          Sign in
        </button>
        <button
          type="button"
          class="flex-1 h-9 font-medium transition-colors"
          :class="
            tab === 'signup'
              ? 'bg-ink text-bg'
              : 'text-ink-2 hover:text-ink hover:bg-surface'
          "
          @click="switchTab('signup')"
        >
          Create account
        </button>
      </div>

      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          v-if="tab === 'signup'"
          id="auth-name"
          label="Full name"
          v-model="name"
          autocomplete="name"
          placeholder="Your name"
        />

        <FormField
          id="auth-email"
          label="Email"
          v-model="email"
          type="email"
          autocomplete="email"
          placeholder="you@example.com"
        />

        <div class="flex flex-col gap-1.5">
          <div class="flex items-center justify-between">
            <label class="text-sm font-medium text-ink" for="auth-password"
              >Password</label
            >
            <button
              v-if="tab === 'signin'"
              type="button"
              class="text-xs text-ink-3 hover:text-ink underline underline-offset-2"
              @click="
                mode = 'forgot';
                error = '';
              "
            >
              Forgot password?
            </button>
          </div>
          <input
            id="auth-password"
            v-model="password"
            type="password"
            :autocomplete="
              tab === 'signin' ? 'current-password' : 'new-password'
            "
            placeholder="••••••••••••••••"
            class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors"
          />
        </div>

        <!-- Terms checkbox — signup only -->
        <div v-if="tab === 'signup'" class="flex flex-col gap-1.5">
          <label class="flex items-start gap-2.5 cursor-pointer group">
            <input
              v-model="agreedToTerms"
              type="checkbox"
              class="mt-0.5 size-4 rounded border-line accent-ink shrink-0 cursor-pointer"
              @change="termsError = false"
            />
            <span class="text-sm text-ink-2 leading-snug">
              I have read and agree to the
              <RouterLink
                to="/terms"
                target="_blank"
                class="text-ink underline underline-offset-2 hover:text-accent-2 transition-colors"
              >
                Terms of Service
              </RouterLink>
              and
              <RouterLink
                to="/privacy"
                target="_blank"
                class="text-ink underline underline-offset-2 hover:text-accent-2 transition-colors"
              >
                Privacy Policy
              </RouterLink>
            </span>
          </label>
          <p v-if="termsError" class="text-xs text-red-500">
            You must agree to the Terms and Privacy Policy to create an account.
          </p>
        </div>

        <!-- EMAIL_NOT_VERIFIED state -->
        <div
          v-if="error === ERROR_EMAIL_NOT_VERIFIED"
          class="flex flex-col gap-2 rounded-lg bg-surface border border-line p-3"
        >
          <p class="text-sm text-ink-2">
            Please verify your email before signing in.
          </p>
          <button
            type="button"
            class="text-sm text-ink underline underline-offset-2 self-start"
            @click="handleResend"
          >
            {{ verificationResent ? 'Sent!' : 'Resend verification email' }}
          </button>
        </div>
        <p v-else-if="error" class="text-sm text-red-500">{{ error }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
        >
          {{ tab === 'signin' ? 'Sign in' : 'Create account' }}
        </button>
      </form>

      <p class="text-center text-sm text-ink-3">
        <template v-if="tab === 'signin'">
          No account?
          <button
            type="button"
            class="text-ink underline underline-offset-2"
            @click="switchTab('signup')"
          >
            Sign up
          </button>
        </template>
        <template v-else>
          Already have an account?
          <button
            type="button"
            class="text-ink underline underline-offset-2"
            @click="switchTab('signin')"
          >
            Sign in
          </button>
        </template>
      </p>
    </div>
  </Drawer>
</template>
