import path from 'node:path'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  test: {
    environment: 'node',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.ts'],
    // Local `.env` may set VITE_USE_MOCK=true; auth/token tests require real API client path.
    env: {
      VITE_USE_MOCK: 'false',
    },
  },
})
