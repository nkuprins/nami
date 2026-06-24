import { ref } from 'vue';

export function usePinchZoom(
  opts: {
    maxScale?: number;
    swipeThreshold?: number;
    onSwipe?: (dir: 'left' | 'right') => void;
  } = {}
) {
  const maxScale = opts.maxScale ?? 5;
  const swipeThreshold = opts.swipeThreshold ?? 60;

  const scale = ref(1);
  const panX = ref(0);
  const panY = ref(0);
  const pinching = ref(false);

  let touch0Start = { x: 0, y: 0 };
  let touch1Start = { x: 0, y: 0 };
  let pinchStartDist = 0;
  let pinchStartScale = 1;
  let panStartX = 0;
  let panStartY = 0;
  let swipeStartX = 0;
  let swipeStartY = 0;

  function reset() {
    scale.value = 1;
    panX.value = 0;
    panY.value = 0;
  }

  function dist(t0: Touch, t1: Touch) {
    return Math.hypot(t1.clientX - t0.clientX, t1.clientY - t0.clientY);
  }

  function mid(t0: Touch, t1: Touch) {
    return {
      x: (t0.clientX + t1.clientX) / 2,
      y: (t0.clientY + t1.clientY) / 2,
    };
  }

  function onTouchStart(e: TouchEvent) {
    if (e.touches.length === 2) {
      pinching.value = true;
      touch0Start = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      touch1Start = { x: e.touches[1].clientX, y: e.touches[1].clientY };
      pinchStartDist = dist(e.touches[0], e.touches[1]);
      pinchStartScale = scale.value;
    } else if (e.touches.length === 1) {
      pinching.value = false;
      swipeStartX = e.touches[0].clientX;
      swipeStartY = e.touches[0].clientY;
      panStartX = panX.value;
      panStartY = panY.value;
    }
  }

  function onTouchMove(e: TouchEvent) {
    e.preventDefault();
    if (e.touches.length === 2 && pinching.value) {
      const newDist = dist(e.touches[0], e.touches[1]);
      scale.value = Math.min(
        maxScale,
        Math.max(1, pinchStartScale * (newDist / pinchStartDist))
      );

      const m = mid(e.touches[0], e.touches[1]);
      const s = mid(
        { clientX: touch0Start.x, clientY: touch0Start.y } as Touch,
        { clientX: touch1Start.x, clientY: touch1Start.y } as Touch
      );
      panX.value = panStartX + (m.x - s.x);
      panY.value = panStartY + (m.y - s.y);
    } else if (e.touches.length === 1 && !pinching.value && scale.value > 1) {
      panX.value = panStartX + (e.touches[0].clientX - swipeStartX);
      panY.value = panStartY + (e.touches[0].clientY - swipeStartY);
    }
  }

  function onTouchEnd(e: TouchEvent) {
    if (e.touches.length > 0) return;
    pinching.value = false;

    if (scale.value <= 1) {
      reset();
      const dx = (e.changedTouches[0]?.clientX ?? swipeStartX) - swipeStartX;
      const dy = (e.changedTouches[0]?.clientY ?? swipeStartY) - swipeStartY;
      if (Math.abs(dx) > swipeThreshold && Math.abs(dx) > Math.abs(dy)) {
        opts.onSwipe?.(dx < 0 ? 'left' : 'right');
      }
    }
  }

  return {
    scale,
    panX,
    panY,
    pinching,
    reset,
    onTouchStart,
    onTouchMove,
    onTouchEnd,
  };
}
