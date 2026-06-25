import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { Locale } from '../i18n';

export function useLocaleRoute() {
  const route = useRoute();
  const router = useRouter();

  const locale = computed<Locale>(() => {
    const p = route.params.locale;
    return p === 'en' ? 'en' : 'lv';
  });

  function localePath(path: string): string {
    const prefix = `/${locale.value}`;
    if (path === '/') return prefix;
    return `${prefix}${path.startsWith('/') ? path : '/' + path}`;
  }

  function localePush(path: string): ReturnType<typeof router.push> {
    return router.push(localePath(path));
  }

  function switchLocalePath(target: Locale): string {
    const full = route.fullPath;
    const current = `/${locale.value}`;
    if (full.startsWith(current)) {
      return `/${target}${full.slice(current.length) || ''}`;
    }
    return `/${target}`;
  }

  return { locale, localePath, localePush, switchLocalePath };
}
