export function extractYouTubeId(url: URL): string | null {
  const host = url.hostname.replace(/^www\./, '');
  if (host === 'youtu.be') {
    return url.pathname.split('/').find(Boolean) ?? null;
  }
  if (host.includes('youtube.com')) {
    if (url.pathname === '/watch') {
      return url.searchParams.get('v');
    }
    if (url.pathname.startsWith('/shorts/')) {
      return url.pathname.split('/').filter(Boolean)[1] ?? null;
    }
    if (url.pathname.startsWith('/embed/')) {
      return url.pathname.split('/').filter(Boolean)[1] ?? null;
    }
  }
  return null;
}

export function normalizeVideoEmbedUrl(rawUrl: string): string {
  try {
    const url = new URL(rawUrl);
    const host = url.hostname.replace(/^www\./, '');
    const youtubeId = extractYouTubeId(url);
    if (youtubeId) {
      return `https://www.youtube.com/embed/${youtubeId}?rel=0`;
    }
    if (host.includes('vimeo.com')) {
      const id = url.pathname.split('/').findLast(Boolean);
      if (id) return `https://player.vimeo.com/video/${id}?rel=0`;
    }
    return rawUrl;
  } catch {
    return rawUrl;
  }
}

export function getVideoThumbnailUrl(rawUrl: string): string {
  try {
    const url = new URL(rawUrl);
    const youtubeId = extractYouTubeId(url);
    if (youtubeId) {
      return `https://img.youtube.com/vi/${youtubeId}/hqdefault.jpg`;
    }
  } catch {
    // fall through
  }
  return '';
}

export function getVideoSourceLabel(rawUrl: string): string {
  try {
    const host = new URL(rawUrl).hostname.replace(/^www\./, '');
    if (host.includes('youtube.com') || host === 'youtu.be')
      return 'YouTube tour';
    if (host.includes('vimeo.com')) return 'Vimeo tour';
    return 'Video tour';
  } catch {
    return 'Video tour';
  }
}
