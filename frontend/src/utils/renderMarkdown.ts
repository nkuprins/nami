import { marked } from 'marked';

export function renderMarkdown(md: string): string {
  const text = md
    .replace(/\[DATE\]/g, '26 June 2026')
    .replace(/\[WEBSITE_URL\]/g, 'baltnami.id.lv');
  return marked(text) as string;
}
