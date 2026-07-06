<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../ui/Drawer.vue';
import FormField from '../ui/FormField.vue';
import { useAuthStore } from '../../stores/authStore';
import {
  MIN_PASSWORD_LENGTH,
  ERROR_EMAIL_NOT_VERIFIED,
} from '../../api/authApi';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

const { t } = useI18n();
const { localePath } = useLocaleRoute();

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

async function submitForgot() {
  if (!email.value) {
    error.value = t('auth.enterEmail');
    return;
  }
  await forgotPassword(email.value);
  forgotSent.value = true;
}

async function submitSignin() {
  if (!email.value || !password.value) {
    error.value = t('auth.fillAllFields');
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
}

async function submitSignup() {
  if (!name.value || !email.value || !password.value) {
    error.value = t('auth.fillAllFields');
    return;
  }
  if (password.value.length < MIN_PASSWORD_LENGTH) {
    error.value = t('auth.passwordLength');
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

async function submit() {
  error.value = '';
  submitting.value = true;
  try {
    if (mode.value === 'forgot') {
      await submitForgot();
    } else if (tab.value === 'signin') {
      await submitSignin();
    } else {
      await submitSignup();
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
        ? t('auth.checkEmail')
        : mode === 'forgot'
          ? t('auth.resetPassword')
          : tab === 'signin'
            ? t('auth.signIn')
            : t('auth.createAccount')
    "
    @update:open="close"
  >
    <!-- Verification pending -->
    <div
      v-if="pendingVerification"
      class="flex flex-col gap-4 text-sm text-ink-2"
    >
      <p>
        {{ t('auth.verificationSent') }}
        <span class="text-ink font-medium">{{ email }}</span
        >.
      </p>
      <p>{{ t('auth.clickToActivate') }}</p>
      <button
        type="button"
        class="text-ink underline underline-offset-2 self-start"
        @click="switchTab('signin')"
      >
        {{ t('auth.backToSignIn') }}
      </button>
    </div>

    <!-- Forgot password -->
    <div v-else-if="mode === 'forgot'" class="flex flex-col gap-6">
      <div v-if="forgotSent" class="text-sm text-ink-2">
        <p>{{ t('auth.resetSent') }}</p>
        <button
          type="button"
          class="mt-4 text-ink underline underline-offset-2"
          @click="switchTab('signin')"
        >
          {{ t('auth.backToSignIn') }}
        </button>
      </div>
      <form v-else class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          id="forgot-email"
          :label="t('auth.emailLabel')"
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
          {{ t('auth.sendResetLink') }}
        </button>
        <button
          type="button"
          class="text-sm text-ink-3 underline underline-offset-2"
          @click="switchTab('signin')"
        >
          {{ t('auth.backToSignIn') }}
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
          {{ t('auth.signIn') }}
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
          {{ t('auth.createAccount') }}
        </button>
      </div>

      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          v-if="tab === 'signup'"
          id="auth-name"
          :label="t('auth.fullNameLabel')"
          v-model="name"
          autocomplete="name"
          :placeholder="t('auth.namePlaceholder')"
        />

        <FormField
          id="auth-email"
          :label="t('auth.emailLabel')"
          v-model="email"
          type="email"
          autocomplete="email"
          placeholder="you@example.com"
        />

        <div class="flex flex-col gap-1.5">
          <div class="flex items-center justify-between">
            <label class="text-sm font-medium text-ink" for="auth-password">
              {{ t('auth.passwordLabel') }}
            </label>
            <button
              v-if="tab === 'signin'"
              type="button"
              class="text-xs text-ink-3 hover:text-ink underline underline-offset-2"
              @click="
                mode = 'forgot';
                error = '';
              "
            >
              {{ t('auth.forgotPassword') }}
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
              {{ t('auth.iAgree') }}
              <RouterLink
                :to="localePath('/terms')"
                target="_blank"
                class="text-ink underline underline-offset-2 hover:text-accent-2 transition-colors"
              >
                {{ t('auth.termsOfService') }}
              </RouterLink>
              {{ t('auth.and') }}
              <RouterLink
                :to="localePath('/privacy')"
                target="_blank"
                class="text-ink underline underline-offset-2 hover:text-accent-2 transition-colors"
              >
                {{ t('auth.privacyPolicy') }}
              </RouterLink>
            </span>
          </label>
          <p v-if="termsError" class="text-xs text-red-500">
            {{ t('auth.agreeToTerms') }}
          </p>
        </div>

        <!-- EMAIL_NOT_VERIFIED state -->
        <div
          v-if="error === ERROR_EMAIL_NOT_VERIFIED"
          class="flex flex-col gap-2 rounded-lg bg-surface border border-line p-3"
        >
          <p class="text-sm text-ink-2">
            {{ t('auth.verifyBeforeSignIn') }}
          </p>
          <button
            type="button"
            class="text-sm text-ink underline underline-offset-2 self-start"
            @click="handleResend"
          >
            {{
              verificationResent ? t('auth.sent') : t('auth.resendVerification')
            }}
          </button>
        </div>
        <p v-else-if="error" class="text-sm text-red-500">{{ error }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
        >
          {{ tab === 'signin' ? t('auth.signIn') : t('auth.createAccount') }}
        </button>
      </form>

      <p class="text-center text-sm text-ink-3">
        <template v-if="tab === 'signin'">
          {{ t('auth.noAccount') }}
          <button
            type="button"
            class="text-ink underline underline-offset-2"
            @click="switchTab('signup')"
          >
            {{ t('auth.signUp') }}
          </button>
        </template>
        <template v-else>
          {{ t('auth.alreadyHaveAccount') }}
          <button
            type="button"
            class="text-ink underline underline-offset-2"
            @click="switchTab('signin')"
          >
            {{ t('auth.signIn') }}
          </button>
        </template>
      </p>
    </div>
  </Drawer>
</template>
