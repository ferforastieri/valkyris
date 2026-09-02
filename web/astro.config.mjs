import { defineConfig } from 'astro/config';

export default defineConfig({
  site: 'https://camtacte.vercel.app',
  output: 'static',
  trailingSlash: 'never',
  build: { format: 'directory' },
});

