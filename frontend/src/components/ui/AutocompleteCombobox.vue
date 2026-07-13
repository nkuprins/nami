<script setup lang="ts" generic="T extends { name: string }">
import { computed, ref, watch } from 'vue';
import IconSpinner from '../icons/IconSpinner.vue';
import { normalizeAddress } from '../../utils/addressMatch';

// A strict-selection combobox: typing only filters (via the debounced `search`
// event — the parent owns fetching); the value is always a picked option or
// null. Text that unambiguously equals an option (case/diacritics-insensitive)
// commits on Enter or blur without an explicit click; anything else snaps back
// to the current selection on blur, so free text can never masquerade as a value.

const props = defineProps<{
  modelValue: T | null;
  options: T[];
  label: string;
  id: string;
  placeholder?: string;
  error?: string;
  required?: boolean;
  disabled?: boolean;
  loading?: boolean;
  noResultsText: string;
  // Secondary text shown right-aligned in an option row (e.g. territory name).
  secondary?: (option: T) => string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: T | null];
  search: [query: string];
}>();

const SEARCH_DEBOUNCE_MS = 250;

const inputEl = ref<HTMLInputElement | null>(null);
const query = ref(props.modelValue?.name ?? '');
const open = ref(false);
const activeIndex = ref(-1);
let debounceTimer: ReturnType<typeof setTimeout> | undefined;

// External selection changes (reset by parent, draft restore) sync the text.
watch(
  () => props.modelValue,
  (value) => {
    query.value = value?.name ?? '';
  }
);

const showNoResults = computed(
  () => open.value && !props.loading && props.options.length === 0
);

function onFocus() {
  open.value = true;
  emitSearch(props.modelValue ? '' : query.value, true);
}

function onInput(e: Event) {
  query.value = (e.target as HTMLInputElement).value;
  open.value = true;
  activeIndex.value = -1;
  // Typing invalidates the previous pick — the value is strictly "chosen from list".
  if (props.modelValue) emit('update:modelValue', null);
  emitSearch(query.value);
}

function emitSearch(q: string, immediate = false) {
  clearTimeout(debounceTimer);
  if (immediate) {
    emit('search', q);
    return;
  }
  debounceTimer = setTimeout(() => emit('search', q), SEARCH_DEBOUNCE_MS);
}

function select(option: T) {
  emit('update:modelValue', option);
  query.value = option.name;
  open.value = false;
  activeIndex.value = -1;
}

// The single option the typed text already spells out exactly, if any.
function exactMatch(): T | null {
  const q = normalizeAddress(query.value);
  if (!q) return null;
  const matches = props.options.filter((o) => normalizeAddress(o.name) === q);
  return matches.length === 1 ? matches[0] : null;
}

function onBlur() {
  // Typed text that exactly spells an option counts as picking it.
  if (!props.modelValue) {
    const match = exactMatch();
    if (match) {
      select(match);
      return;
    }
  }
  // Otherwise snap back: stray text must not look like a value.
  open.value = false;
  activeIndex.value = -1;
  query.value = props.modelValue?.name ?? '';
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    open.value = false;
    return;
  }
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    if (!open.value) {
      onFocus();
      return;
    }
    const delta = e.key === 'ArrowDown' ? 1 : -1;
    const count = props.options.length;
    if (count === 0) return;
    activeIndex.value = (activeIndex.value + delta + count) % count;
    return;
  }
  if (e.key === 'Enter') {
    if (open.value && activeIndex.value >= 0) {
      e.preventDefault();
      select(props.options[activeIndex.value]);
      return;
    }
    if (!open.value) return;
    // No highlight: a lone remaining option or exactly-typed text commits.
    const match = props.options.length === 1 ? props.options[0] : exactMatch();
    if (match) {
      e.preventDefault();
      select(match);
    }
  }
}
</script>

<template>
  <div class="flex flex-col gap-1.5 relative">
    <label class="text-sm font-medium text-ink" :for="id">
      {{ label }}<span v-if="required" class="text-warn"> *</span>
    </label>
    <div class="relative">
      <input
        :id="id"
        ref="inputEl"
        :value="query"
        type="text"
        role="combobox"
        autocomplete="off"
        :aria-expanded="open"
        :aria-controls="`${id}-listbox`"
        :aria-activedescendant="
          activeIndex >= 0 ? `${id}-option-${activeIndex}` : undefined
        "
        :placeholder="placeholder"
        :disabled="disabled"
        class="w-full h-10 px-3 pr-9 rounded-lg border text-sm text-ink placeholder:text-ink-3 focus:outline-none focus:ring-2 focus:ring-ink/20 focus:border-ink transition-colors disabled:bg-surface disabled:text-ink-3 disabled:cursor-not-allowed disabled:border-line"
        :class="error ? 'border-warn/40 bg-warn/5' : 'border-line-2 bg-bg'"
        @focus="onFocus"
        @input="onInput"
        @blur="onBlur"
        @keydown="onKeydown"
      />
      <span
        v-if="loading"
        class="absolute right-3 top-1/2 -translate-y-1/2 text-ink-3"
      >
        <IconSpinner :size="14" />
      </span>
    </div>

    <ul
      v-if="open && (options.length > 0 || showNoResults)"
      :id="`${id}-listbox`"
      role="listbox"
      class="absolute top-full left-0 right-0 mt-1 z-50 max-h-64 overflow-y-auto bg-bg border border-line rounded-lg shadow-lift py-1"
    >
      <li
        v-for="(option, i) in options"
        :id="`${id}-option-${i}`"
        :key="option.name + i"
        role="option"
        :aria-selected="i === activeIndex"
        class="px-3 py-2 text-sm cursor-pointer flex items-baseline justify-between gap-3"
        :class="i === activeIndex ? 'bg-surface text-ink' : 'text-ink-2 hover:bg-surface hover:text-ink'"
        @mousedown.prevent="select(option)"
        @mousemove="activeIndex = i"
      >
        <span class="truncate">{{ option.name }}</span>
        <span v-if="secondary" class="text-xs text-ink-3 shrink-0">
          {{ secondary(option) }}
        </span>
      </li>
      <li
        v-if="showNoResults"
        class="px-3 py-2 text-sm text-ink-3"
        aria-disabled="true"
      >
        {{ noResultsText }}
      </li>
    </ul>

    <p v-if="error" class="text-xs text-warn">{{ error }}</p>
  </div>
</template>
