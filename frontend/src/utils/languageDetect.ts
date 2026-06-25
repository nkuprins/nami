const LATVIAN_DIACRITICS = /[āčēģīķļņšūžĀČĒĢĪĶĻŅŠŪŽ]/;
const BILINGUAL_SEPARATOR = / \/ | \| /;

export type LangWarning = 'latvianInEn' | 'separator' | null;

export function detectEnFieldWarning(value: string): LangWarning {
  if (!value) return null;
  if (LATVIAN_DIACRITICS.test(value)) return 'latvianInEn';
  if (BILINGUAL_SEPARATOR.test(value)) return 'separator';
  return null;
}

export function detectLvFieldWarning(value: string): LangWarning {
  if (!value) return null;
  if (BILINGUAL_SEPARATOR.test(value)) return 'separator';
  return null;
}
