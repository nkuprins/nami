<script setup lang="ts">
import { TYPE_OPTIONS, KIND_OPTIONS } from '../../../types/propertyLabels';
import type { PropertyFormState } from '../composables/usePropertyForm';

defineProps<{ form: PropertyFormState }>();
</script>

<template>
  <section class="flex flex-col gap-4">
    <h2 class="text-base font-semibold text-ink border-b border-line pb-2">
      Listing type
    </h2>
    <div class="flex flex-col gap-3">
      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">Transaction type</p>
        <div class="flex gap-2 flex-wrap">
          <button
            v-for="opt in TYPE_OPTIONS"
            :key="opt.id"
            type="button"
            class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
            :class="
              form.type === opt.id
                ? 'bg-ink text-bg border-ink'
                : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
            "
            @click="form.type = opt.id"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="flex flex-col gap-1.5">
        <p class="text-sm font-medium text-ink">Property kind</p>
        <div class="flex gap-2">
          <button
            v-for="opt in KIND_OPTIONS"
            :key="opt.id"
            type="button"
            class="h-9 px-4 rounded-full text-sm font-medium border transition-colors"
            :class="
              form.propertyKind === opt.id
                ? 'bg-ink text-bg border-ink'
                : 'border-line text-ink-2 hover:border-ink/40 hover:text-ink'
            "
            @click="form.propertyKind = opt.id"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
