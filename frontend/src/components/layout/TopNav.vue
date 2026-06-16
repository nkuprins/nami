<script setup lang="ts">
import {ref} from 'vue';
import {RouterLink, useRouter} from 'vue-router';
import IconHeart from '../ui/IconHeart.vue';
import {useSaved} from '../../composables/useSaved';
import {useAuth} from '../../composables/useAuth';
import AuthModal from '../auth/AuthModal.vue';
import SavedDrawer from '../saved/SavedDrawer.vue';
import MyPropertiesDrawer from '../listing/MyPropertiesDrawer.vue';

const router = useRouter();
const {count: savedCount} = useSaved();
const {isAuthenticated, user, logout} = useAuth();

const authOpen = ref(false);
const savedOpen = ref(false);
const myPropertiesOpen = ref(false);
</script>

<template>
  <header class="sticky top-0 z-40 bg-bg/95 backdrop-blur border-b border-line">
    <div class="mx-auto max-w-360 px-6 lg:px-10 h-16 flex items-center justify-between gap-6">
      <RouterLink
          to="/"
          class="focus-ring flex items-baseline gap-2 group"
      >
        <span class="display-headline text-[1.6rem] leading-none text-ink"
              style="font-variation-settings: 'opsz' 96, 'SOFT' 30, 'WONK' 1;">
          Baltnami
        </span>
        <span class="micro-label hidden sm:inline-block -translate-y-0.5">est. 2026</span>
      </RouterLink>

      <div class="flex items-center gap-1 sm:gap-3">
        <button
            class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full
                 text-ink-2 hover:text-ink text-sm transition-colors"
            @click="isAuthenticated ? myPropertiesOpen = true : authOpen = true"
        >
          <i class="ti ti-building-estate text-base" aria-hidden="true"/>
          <span class="hidden sm:inline">My listings</span>
        </button>

        <button
            class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full
                 text-ink-2 hover:text-ink text-sm transition-colors"
            @click="savedOpen = true"
        >
          <span class="size-4 inline-block" :class="{ 'text-accent-2': savedCount > 0 }">
            <IconHeart :filled="savedCount > 0"/>
          </span>
          <span class="hidden sm:inline">Saved</span>
          <span
              v-if="savedCount > 0"
              class="tabular text-[0.6875rem] font-medium px-1.5 h-4.5 inline-flex items-center
                   rounded-full bg-ink text-cream"
          >
            {{ savedCount }}
          </span>
        </button>

        <template v-if="isAuthenticated">
          <span class="hidden sm:inline text-sm text-ink-2 max-w-32 truncate">{{ user?.name }}</span>
          <button
              class="focus-ring inline-flex items-center h-9 px-3 sm:px-4 rounded-full
                   text-sm text-ink hover:bg-surface transition-colors"
              @click="logout"
          >
            Sign out
          </button>
        </template>
        <button
            v-else
            class="focus-ring inline-flex items-center h-9 px-3 sm:px-4 rounded-full
                 text-sm text-ink hover:bg-surface transition-colors"
            @click="authOpen = true"
        >
          Sign in
        </button>

        <button
            class="focus-ring inline-flex items-center h-9 px-3 sm:px-4 rounded-full
                 text-sm bg-ink text-bg hover:bg-accent-2 transition-colors"
            @click="isAuthenticated ? router.push('/add-property') : authOpen = true"
        >
          <span class="inline md:hidden">Add</span>
          <span class="hidden md:inline">Add a property</span>
        </button>
      </div>
    </div>
  </header>

  <AuthModal v-model:open="authOpen"/>
  <SavedDrawer v-model:open="savedOpen"/>
  <MyPropertiesDrawer v-model:open="myPropertiesOpen"/>
</template>
