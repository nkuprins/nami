<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import IconHeart from '../ui/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { useAuthStore } from '../../stores/authStore';
import AuthModal from '../auth/AuthModal.vue';
import SavedDrawer from '../saved/SavedDrawer.vue';
import MyPropertiesDrawer from '../listing/MyPropertiesDrawer.vue';
import IconBuilding from '../ui/IconBuilding.vue';
import DeleteaccountDialog from '../ui/DeleteaccountDialog.vue';
import IconChevron from '../ui/IconChevron.vue';

const router = useRouter();
const savedStore = useSavedStore();
const auth = useAuthStore();

const authOpen = ref(false);
const savedOpen = ref(false);
const myPropertiesOpen = ref(false);
const deleteAccountOpen = ref(false);
const userMenuOpen = ref(false);
</script>

<template>
  <header class="sticky top-0 z-40 bg-bg/95 backdrop-blur border-b border-line">
    <div
      class="mx-auto max-w-360 px-6 lg:px-10 h-16 flex items-center justify-between gap-6"
    >
      <RouterLink to="/" class="focus-ring flex items-baseline gap-2 group">
        <span
          class="display-headline text-[1.6rem] leading-none text-ink"
          style="
            font-variation-settings:
              'opsz' 96,
              'SOFT' 30,
              'WONK' 1;
          "
        >
          Baltnami
        </span>
        <span class="micro-label hidden sm:inline-block -translate-y-0.5"
          >est. 2026</span
        >
      </RouterLink>

      <div class="flex items-center gap-1 sm:gap-3">
        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-ink-2 hover:text-ink text-sm transition-colors"
          @click="
            auth.isAuthenticated ? (myPropertiesOpen = true) : (authOpen = true)
          "
        >
          <span class="size-4 inline-block">
            <IconBuilding />
          </span>
          <span class="hidden sm:inline">My listings</span>
        </button>

        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-ink-2 hover:text-ink text-sm transition-colors"
          @click="savedOpen = true"
        >
          <span
            class="size-4 inline-block"
            :class="{ 'text-accent-2': savedStore.count > 0 }"
          >
            <IconHeart :filled="savedStore.count > 0" />
          </span>
          <span class="hidden sm:inline">Saved</span>
          <span
            v-if="savedStore.count > 0"
            class="tabular text-[0.6875rem] font-medium px-1.5 h-4.5 inline-flex items-center rounded-full bg-ink text-cream"
          >
            {{ savedStore.count }}
          </span>
        </button>

        <!-- User menu -->
        <template v-if="auth.isAuthenticated">
          <div
            class="relative"
            @mouseenter="userMenuOpen = true"
            @mouseleave="userMenuOpen = false"
          >
            <button
              class="focus-ring inline-flex items-center gap-1.5 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
              @click="userMenuOpen = !userMenuOpen"
            >
              <span class="hidden sm:inline max-w-32 truncate">{{
                auth.user?.name
              }}</span>
              <span
                class="sm:hidden size-6 rounded-full bg-surface border border-line flex items-center justify-center text-xs font-medium text-ink"
              >
                {{ auth.user?.name?.[0]?.toUpperCase() }}
              </span>
              <span class="size-4 text-ink-2">
                <IconChevron :dir="userMenuOpen ? 'up' : 'down'" />
              </span>
            </button>

            <!-- Invisible bridge fills the gap so mouseleave doesn't fire mid-travel -->
            <div class="absolute left-0 right-0 h-3 top-full" />

            <!-- Dropdown -->
            <Transition name="pop">
              <div
                v-if="userMenuOpen"
                class="absolute right-0 top-full mt-3 w-48 bg-bg border border-line rounded-xl shadow-lift overflow-hidden z-50"
              >
                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-ink hover:bg-surface transition-colors"
                  @click="
                    auth.logout();
                    userMenuOpen = false;
                  "
                >
                  Sign out
                </button>

                <div class="border-t border-line" />

                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-warn/70 hover:text-warn hover:bg-warn/5 transition-colors"
                  @click="
                    deleteAccountOpen = true;
                    userMenuOpen = false;
                  "
                >
                  Delete account
                </button>
              </div>
            </Transition>
          </div>
        </template>

        <button
          v-else
          class="focus-ring inline-flex items-center h-9 px-3 sm:px-4 rounded-full text-sm text-ink hover:bg-surface transition-colors"
          @click="authOpen = true"
        >
          Sign in
        </button>

        <button
          class="focus-ring inline-flex items-center h-9 px-3 sm:px-4 rounded-full text-sm bg-ink text-bg hover:bg-accent-2 transition-colors"
          @click="
            auth.isAuthenticated
              ? router.push('/add-property')
              : (authOpen = true)
          "
        >
          <span class="inline md:hidden">Add</span>
          <span class="hidden md:inline">Add a property</span>
        </button>
      </div>
    </div>
  </header>

  <AuthModal v-model:open="authOpen" />
  <SavedDrawer v-model:open="savedOpen" />
  <MyPropertiesDrawer v-model:open="myPropertiesOpen" />
  <DeleteaccountDialog v-model:open="deleteAccountOpen" />
</template>
