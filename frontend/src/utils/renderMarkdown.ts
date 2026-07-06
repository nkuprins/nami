import { marked } from 'marked';

export function renderMarkdown(md: string): string {
  const text = md
    .replaceAll('[DATE]', '26 June 2026')
    .replaceAll('[WEBSITE_URL]', 'baltnami.id.lv');
  return marked(text) as string;
}
