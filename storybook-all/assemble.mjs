import { cpSync, existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const hostStatic = resolve(root, 'storybook-all/storybook-static');

const projects = [
  { name: 'console', dir: 'gravitee-apim-console-webui/storybook-static' },
  { name: 'portal-next', dir: 'gravitee-apim-portal-webui-next/storybook-static' },
  { name: 'markdown', dir: 'gravitee-apim-webui-libs/gravitee-markdown/storybook-static' },
  { name: 'dashboard', dir: 'gravitee-apim-webui-libs/gravitee-dashboard/storybook-static' },
  { name: 'kafka-explorer', dir: 'gravitee-apim-webui-libs/gravitee-kafka-explorer/storybook-static' },
];

for (const { name, dir } of projects) {
  const src = resolve(root, dir);
  const dest = resolve(hostStatic, name);

  if (!existsSync(src)) {
    console.warn(`[assemble] Skipping "${name}": ${src} does not exist`);
    continue;
  }

  console.log(`[assemble] Copying ${name} → ${dest}`);
  cpSync(src, dest, { recursive: true });

  // Inject <base href> so webpack chunks resolve from the correct subdirectory
  const iframePath = resolve(dest, 'iframe.html');
  if (existsSync(iframePath)) {
    const html = readFileSync(iframePath, 'utf-8');
    const patched = html.replace('<head>', `<head><base href="/${name}/">`);
    writeFileSync(iframePath, patched, 'utf-8');
    console.log(`[assemble] Patched ${name}/iframe.html with <base href="/${name}/">`);
  }
}

console.log('[assemble] Done.');
