import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/ui/',
  build: {
    outDir: '../server/src/main/resources/assets',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/portfolios': 'http://localhost:8080',
      '/watchlists': 'http://localhost:8080',
      '/alerts': 'http://localhost:8080',
      '/screener': 'http://localhost:8080',
      '/analysis': 'http://localhost:8080',
    }
  }
})
