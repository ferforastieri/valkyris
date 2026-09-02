import { copyFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, '../..');
const publicDir = resolve(here, '../public');

await mkdir(publicDir, { recursive: true });
await Promise.all([
  copyFile(resolve(root, 'install.sh'), resolve(publicDir, 'install.sh')),
  copyFile(resolve(root, 'backend/internal/api/openapi.yaml'), resolve(publicDir, 'openapi.yaml')),
]);
