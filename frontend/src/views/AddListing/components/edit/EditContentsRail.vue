<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{
  sections: { id: string; label: string }[];
}>();

const activeId = ref(props.sections[0]?.id ?? '');
let observer: IntersectionObserver | null = null;

// The threshold line the rail highlights against: whichever section's card
// currently spans this point in the viewport.
const THRESHOLD_Y = 160;

function recompute(els: HTMLElement[]) {
  // Near the bottom of the page the last section's card may be too short for
  // its top to ever cross the threshold line, so it would never get picked
  // by the loop below — force it active once we've hit the scroll bottom.
  const atBottom =
    window.innerHeight + window.scrollY >=
    document.documentElement.scrollHeight - 2;
  if (atBottom) {
    const last = els[els.length - 1];
    if (last) activeId.value = last.id;
    return;
  }

  let current = els[0];
  for (const el of els) {
    if (el.getBoundingClientRect().top <= THRESHOLD_Y) current = el;
  }
  if (current) activeId.value = current.id;
}

let onScroll: (() => void) | null = null;

onMounted(() => {
  const els = props.sections
    .map((s) => document.getElementById(s.id))
    .filter((el): el is HTMLElement => !!el);

  // IntersectionObserver's first callback is async (fires after layout, e.g.
  // once the map has finished sizing itself), so compute the initial state
  // synchronously too — otherwise the rail can briefly highlight the wrong
  // section on load.
  recompute(els);

  observer = new IntersectionObserver(() => recompute(els), {
    rootMargin: `-${THRESHOLD_Y}px 0px -70% 0px`,
    threshold: 0,
  });
  els.forEach((el) => observer?.observe(el));

  // The IntersectionObserver only fires on intersection changes, which can
  // miss the final few pixels of scroll (no section crosses the band there)
  // — a plain scroll listener catches the bottom-of-page case reliably.
  onScroll = () => recompute(els);
  window.addEventListener('scroll', onScroll, { passive: true });
});

onBeforeUnmount(() => {
  observer?.disconnect();
  if (onScroll) window.removeEventListener('scroll', onScroll);
});

function jumpTo(id: string) {
  document
    .getElementById(id)
    ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
</script>

<template>
  <nav
    class="hidden lg:flex flex-col gap-1 sticky top-24 w-40 shrink-0 self-start"
  >
    <button
      v-for="section in sections"
      :key="section.id"
      type="button"
      class="text-left px-3 py-1.5 rounded-md text-sm transition-colors"
      :class="
        activeId === section.id
          ? 'bg-surface text-ink font-medium'
          : 'text-ink-3 hover:text-ink'
      "
      @click="jumpTo(section.id)"
    >
      {{ section.label }}
    </button>
  </nav>
</template>
