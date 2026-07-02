// Mirror of the backend AddressMatcher: a normalized-exact match is a hard
// duplicate; a small edit distance is a near match (likely typo or a disguised
// copy such as an added house number). Keep the threshold in sync with
// backend/.../validation/AddressMatcher.java.
const NEAR_MATCH_THRESHOLD = 0.72;

export function normalizeAddress(address: string): string {
  return address
    .normalize('NFD')
    .replace(/\p{M}+/gu, '') // fold diacritics (ī → i)
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

export function isExactAddress(a: string, b: string): boolean {
  return normalizeAddress(a) === normalizeAddress(b);
}

export function isNearAddress(a: string, b: string): boolean {
  const na = normalizeAddress(a);
  const nb = normalizeAddress(b);
  if (na === nb || !na || !nb) return false;
  const distance = levenshtein(na, nb);
  const similarity = 1 - distance / Math.max(na.length, nb.length);
  return similarity >= NEAR_MATCH_THRESHOLD;
}

function levenshtein(a: string, b: string): number {
  let prev = Array.from({ length: b.length + 1 }, (_, j) => j);
  let curr = new Array<number>(b.length + 1);
  for (let i = 1; i <= a.length; i++) {
    curr[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      curr[j] = Math.min(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    [prev, curr] = [curr, prev];
  }
  return prev[b.length];
}
