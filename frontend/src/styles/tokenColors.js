// Plain-JS mirror of the colour tokens in styles/variables.css. Leaflet and
// raw SVG charts render outside of React's DOM tree, so they can't read CSS
// custom properties — these constants exist so map layers and charts stay
// in sync with the app's palette instead of each hardcoding its own copy of
// the same hex values. Keep in sync with variables.css.
export const TOKEN_COLORS = {
  ok: '#059669',
  warning: '#d97706',
  danger: '#e11d48',
  info: '#4f46e5',
  muted: '#64748b',
  line: '#cbd5e1',
  lineLight: '#94a3b8',
};
