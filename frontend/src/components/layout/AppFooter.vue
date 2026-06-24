<script setup lang="ts"></script>

<template>
  <footer class="border-t border-line mt-auto">
    <div
      class="mx-auto max-w-360 px-6 lg:px-10 py-8 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <p class="text-xs text-ink-3">
        © {{ new Date().getFullYear() }} Baltnami. All rights reserved.
      </p>
      <nav class="flex items-center gap-5">
        <RouterLink
          to="/privacy"
          class="text-xs text-ink-3 hover:text-ink underline underline-offset-2 transition-colors"
        >
          Privacy Policy
        </RouterLink>
        <RouterLink
          to="/terms"
          class="text-xs text-ink-3 hover:text-ink underline underline-offset-2 transition-colors"
        >
          Terms of Service
        </RouterLink>
      </nav>
    </div>
  </footer>
</template>
