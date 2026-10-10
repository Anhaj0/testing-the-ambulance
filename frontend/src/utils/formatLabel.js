// Turns a raw backend enum value (e.g. "AVAILABLE", "ADVANCED_LIFE_SUPPORT")
// into a human-readable label ("Available", "Advanced Life Support"). Shared
// by every module that displays an enum straight from the API, so status
// text never leaks onto the screen in shouting case.
export function formatLabel(value) {
  if (!value) return '';
  return String(value)
    .toLowerCase()
    .split('_')
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(' ');
}
