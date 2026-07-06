import { marked } from 'marked';

export function renderMarkdown(md: string): string {
  const text = md
    .replaceAll(/\[DATE\]/g, '26 June 2026')
    .replaceAll(/\[WEBSITE_URL\]/g, 'baltnami.id.lv');
  return marked(text) as string;
}
