import { ref } from 'vue';

export function useShare() {
  const justCopied = ref(false);

  function markCopied() {
    justCopied.value = true;
    setTimeout(() => {
      justCopied.value = false;
    }, 2000);
  }

  async function share(data: { title: string; url: string }) {
    if (navigator.share) {
      try {
        await navigator.share({ title: data.title, url: data.url });
        return;
      } catch (e) {
        if ((e as DOMException).name === 'AbortError') return;
      }
    }

    try {
      await navigator.clipboard.writeText(data.url);
    } catch {
      const ta = document.createElement('textarea');
      ta.value = data.url;
      ta.style.cssText = 'position:fixed;opacity:0';
      document.body.appendChild(ta);
      ta.select();
      document.execCommand('copy');
      ta.remove();
    }
    markCopied();
  }

  return { share, justCopied };
}
