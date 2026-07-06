export type MediaVariant = 'thumb' | 'card';

/**
 * Mirrors the backend `MediaVariants.derive`: insert `_suffix` before the file extension
 * (`…/foo.jpg` → `…/foo_card.jpg`). Variant URLs are never stored — they are derived here on read.
 */
export function mediaVariant(url: string, suffix: MediaVariant): string {
  const slash = url.lastIndexOf('/');
  const dot = url.lastIndexOf('.');
  return dot > slash
    ? `${url.slice(0, dot)}_${suffix}${url.slice(dot)}`
    : `${url}_${suffix}`;
}

/**
 * Falls back to the original once if the resized variant isn't there yet (e.g. the worker hasn't
 * finished, or a publish was lost). Keeps images working without any per-photo "is it processed" flag.
 */
export function onVariantError(event: Event, original: string): void {
  const img = event.target as HTMLImageElement;
  if (img.src !== original) {
    img.src = original;
  }
}
