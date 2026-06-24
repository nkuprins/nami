export function isNew(postedAt: string): boolean {
  const posted = new Date(postedAt).getTime();
  const sevenDays = 7 * 24 * 60 * 60 * 1000;
  return Date.now() - posted < sevenDays;
}

export function numericInput(e: InputEvent) {
  if (e.data !== null && !/^\d+$/.test(e.data)) e.preventDefault();
}
