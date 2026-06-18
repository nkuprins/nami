<script setup lang="ts"></script>

<template>
  <Transition name="fade" appear>
    <div
      class="grain relative min-h-screen bg-cream px-6 py-12 flex flex-col justify-between items-center overflow-hidden selection:bg-ink selection:text-cream"
    >
      <main
        class="w-full max-w-3xl text-center my-auto flex flex-col items-center justify-center z-10 gap-6 md:gap-8"
      >
        <h1
          class="display-headline text-8xl sm:text-[12rem] text-ink select-none tabular tracking-tighter opacity-90"
        >
          404
        </h1>

        <div class="space-y-3 max-w-lg">
          <p class="display-eyebrow text-xl md:text-2xl text-accent-2">
            This page has drifted off the map
          </p>
          <h2
            class="font-sans font-normal text-ink-2 text-base md:text-lg leading-relaxed"
          >
            The resource you are looking for might have been removed, had its
            name changed, or is temporarily unavailable.
          </h2>
        </div>
      </main>
    </div>
  </Transition>
</template>
