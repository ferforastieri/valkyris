import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';
import test from 'node:test';

const pt = await readFile(new URL('../dist/pt-BR/index.html', import.meta.url), 'utf8');
const en = await readFile(new URL('../dist/en/index.html', import.meta.url), 'utf8');
const root = await readFile(new URL('../dist/index.html', import.meta.url), 'utf8');

test('publica as duas localizações com idioma correto', () => {
  assert.match(pt, /<html lang="pt-BR"/);
  assert.match(en, /<html lang="en"/);
  assert.match(pt, /curl -fsSL https:\/\/valkyris\.vercel\.app\/install\.sh \| sh/);
  assert.match(root, /valkyris-locale/);
});

test('controles e navegação possuem nomes acessíveis', () => {
  assert.match(pt, /aria-label="[^"]+"/);
  assert.match(pt, /href="\/en"/);
  assert.match(en, /href="\/pt-BR"/);
  assert.match(pt, /class="floating-dock"/);
  assert.doesNotMatch(pt, /<header[\s>]/);
  assert.doesNotMatch(pt, /<footer[\s>]/);
  assert.match(pt, /© 2026 · MIT/);
  assert.match(pt, /valkyris-mark\.png/);
  assert.match(pt, /href="\/favicon\.png"/);
  assert.match(pt, /href="\/apple-touch-icon\.png"/);
});

test('respeita a preferência por movimento reduzido', async () => {
  const assets = new URL('../dist/_astro/', import.meta.url);
  const files = (await readdir(assets)).filter((file) => file.endsWith('.css'));
  const styles = (await Promise.all(files.map((file) => readFile(new URL(file, assets), 'utf8')))).join('\n');
  assert.match(styles, /prefers-reduced-motion:reduce/);
  assert.match(styles, /\.phone-stage\{height:auto;display:grid/);
  assert.match(styles, /\.floating-dock\{position:fixed/);
});

test('artefatos públicos acompanham a documentação', async () => {
  const installer = await readFile(new URL('../dist/install.sh', import.meta.url), 'utf8');
  const openapi = await readFile(new URL('../dist/openapi.yaml', import.meta.url), 'utf8');
  const favicon = await readFile(new URL('../dist/favicon.png', import.meta.url));
  assert.match(installer, /^#!\/bin\/sh/);
  assert.match(openapi, /^openapi: 3\.1\.0/);
  assert.ok(favicon.length > 1_000);
});
