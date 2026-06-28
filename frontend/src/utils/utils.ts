export function isNew(postedAt: string): boolean {
  const posted = new Date(postedAt).getTime();
  const sevenDays = 7 * 24 * 60 * 60 * 1000;
  return Date.now() - posted < sevenDays;
}

export function numericInput(e: InputEvent) {
  if (e.data !== null && !/^\d+$/.test(e.data)) e.preventDefault();
}

export function decimalInput(e: InputEvent) {
  if (e.data === null) return;
  const input = e.target as HTMLInputElement;
  const sepIdx = Math.max(input.value.indexOf('.'), input.value.indexOf(','));
  const hasDecimal = sepIdx !== -1;

  if (!/^[\d.,]$/.test(e.data)) {
    e.preventDefault();
    return;
  }
  if ((e.data === '.' || e.data === ',') && hasDecimal) {
    e.preventDefault();
    return;
  }

  if (/^\d$/.test(e.data) && hasDecimal) {
    const start = input.selectionStart ?? input.value.length;
    const end = input.selectionEnd ?? input.value.length;
    const afterSep = input.value.slice(sepIdx + 1);
    const selStart = Math.max(0, start - sepIdx - 1);
    const selEnd = Math.max(0, end - sepIdx - 1);
    const remaining = afterSep.slice(0, selStart) + afterSep.slice(selEnd);
    if (remaining.length >= 2) e.preventDefault();
  }
}

export function parseDecimal(s: string): number {
  return Number(s.replace(',', '.'));
}

export function formatPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  const local = digits.startsWith('371') ? digits.slice(3) : digits;
  if (local.length !== 8) return phone.replace(/\s+/g, ' ').trim();
  return `+371 ${local.slice(0, 2)} ${local.slice(2, 5)} ${local.slice(5)}`;
}
