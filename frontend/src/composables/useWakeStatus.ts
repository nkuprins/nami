import { computed, ref } from 'vue';

/**
 * The hosted backend (Railway) and database (Neon) suspend after inactivity and
 * take ~20-30s to spin back up. A warm request answers in well under a second,
 * so any request still running past this threshold means we are almost certainly
 * waiting on a cold start — the cue to reassure the user instead of showing a
 * frozen, empty screen.
 */
const WAKE_THRESHOLD_MS = 2500;

const slowRequestCount = ref(0);

/** True while at least one request has been slow enough to look like a cold start. */
export const isWaking = computed(() => slowRequestCount.value > 0);

/**
 * Wrap an in-flight request so it flips {@link isWaking} on once it crosses
 * {@link WAKE_THRESHOLD_MS}, and back off when it (and any siblings) settle.
 */
export async function trackWake<T>(request: Promise<T>): Promise<T> {
  let countedAsSlow = false;
  const timer = setTimeout(() => {
    countedAsSlow = true;
    slowRequestCount.value++;
  }, WAKE_THRESHOLD_MS);

  try {
    return await request;
  } finally {
    clearTimeout(timer);
    if (countedAsSlow) slowRequestCount.value--;
  }
}
