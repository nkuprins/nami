<script setup lang="ts">
import {ref} from 'vue';
import Drawer from '../ui/Drawer.vue';
import {useAuth} from '../../composables/useAuth';

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const {login, signup} = useAuth();

const tab = ref<'signin' | 'signup'>('signin');
const error = ref('');

const name = ref('');
const email = ref('');
const password = ref('');

function reset() {
  name.value = '';
  email.value = '';
  password.value = '';
  error.value = '';
  tab.value = 'signin';
}

function close() {
  emit('update:open', false);
  reset();
}

function switchTab(t: 'signin' | 'signup') {
  tab.value = t;
  error.value = '';
}

function submit() {
  error.value = '';
  if (tab.value === 'signin') {
    if (!email.value || !password.value) {
      error.value = 'Please fill in all fields.';
      return;
    }
    const ok = login(email.value, password.value);
    if (!ok) {
      error.value = 'Invalid credentials.';
      return;
    }
  } else {
    if (!name.value || !email.value || !password.value) {
      error.value = 'Please fill in all fields.';
      return;
    }
    if (password.value.length < 15) {
      error.value = 'Password must be at least 15 characters.';
      return;
    }
    const ok = signup(name.value, email.value, password.value);
    if (!ok) {
      error.value = 'Could not create account.';
      return;
    }
  }
  close();
}
</script>

<template>
  <Drawer :open="open" :title="tab === 'signin' ? 'Sign in' : 'Create account'" @update:open="close">
    <div class="flex flex-col gap-6">
      <!-- Tab switcher -->
      <div class="flex rounded-lg border border-line overflow-hidden text-sm">
        <button
            type="button"
            class="flex-1 h-9 font-medium transition-colors"
            :class="tab === 'signin' ? 'bg-ink text-bg' : 'text-ink-2 hover:text-ink hover:bg-surface'"
            @click="switchTab('signin')"
        >
          Sign in
        </button>
        <button
            type="button"
            class="flex-1 h-9 font-medium transition-colors"
            :class="tab === 'signup' ? 'bg-ink text-bg' : 'text-ink-2 hover:text-ink hover:bg-surface'"
            @click="switchTab('signup')"
        >
          Create account
        </button>
      </div>

      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <div v-if="tab === 'signup'" class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="auth-name">Full name</label>
          <input
              id="auth-name"
              v-model="name"
              type="text"
              autocomplete="name"
              placeholder="Your name"
              class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink
                     placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20
                     focus:border-ink transition-colors"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="auth-email">Email</label>
          <input
              id="auth-email"
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="you@example.com"
              class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink
                     placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20
                     focus:border-ink transition-colors"
          />
        </div>

        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-ink" for="auth-password">Password</label>
          <input
              id="auth-password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••••••••••"
              class="h-10 px-3 rounded-lg border border-line bg-bg text-sm text-ink
                     placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20
                     focus:border-ink transition-colors"
          />
        </div>

        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>

        <button
            type="submit"
            class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors"
        >
          {{ tab === 'signin' ? 'Sign in' : 'Create account' }}
        </button>
      </form>

      <p class="text-center text-sm text-ink-3">
        <template v-if="tab === 'signin'">
          No account?
          <button type="button" class="text-ink underline underline-offset-2" @click="switchTab('signup')">Sign up
          </button>
        </template>
        <template v-else>
          Already have an account?
          <button type="button" class="text-ink underline underline-offset-2" @click="switchTab('signin')">Sign in
          </button>
        </template>
      </p>
    </div>
  </Drawer>
</template>
