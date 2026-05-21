<script setup lang="ts">
import {ref} from 'vue'

const title = ref('Baltnami Real Estate')
</script>

<template>
  <main class="min-h-screen bg-slate-100 flex flex-col justify-center items-center p-6">
    <div class="bg-white p-8 rounded-2xl shadow-xl border border-slate-200 max-w-md text-center">
      <h1 class="text-3xl font-extrabold text-blue-600 tracking-tight mb-2">
        {{ title }}
      </h1>
      <p class="text-slate-600 font-medium">
        Tailwind v4 is officially wired and running beautifully!
      </p>
      <button
          class="mt-5 px-5 py-2.5 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-lg shadow-md transition-colors">
        Browse Listings
      </button>
    </div>
  </main>
</template>
