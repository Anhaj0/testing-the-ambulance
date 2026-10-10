import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During `npm run dev`, any request the app makes to /api/... is
// transparently forwarded to the Spring Boot backend on :8080.
// This means the React code always calls relative paths like
// "/api/v1/network/blind-spots" — no hardcoded backend URL, and the
// exact same code works after `npm run build` when Spring Boot
// serves the built files itself (same origin, no proxy needed then).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist'
  }
});
