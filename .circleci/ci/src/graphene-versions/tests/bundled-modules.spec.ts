/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { readBundledModules, ZipEntries } from '../bundled-modules';

const MANIFEST = JSON.stringify({
  name: 'esm',
  shared: [{ name: '@gravitee/graphene-core', version: '3.13.0' }],
});

/** Stands in for the zips on disk: what each archive holds, keyed by entry path. */
function zipsHolding(archives: Record<string, Record<string, string>>): ZipEntries {
  const entriesOf = (zipFile: string) => archives[path.basename(zipFile)] ?? {};
  return {
    read: (zipFile, entry) => entriesOf(zipFile)[entry],
    contains: (zipFile, directory) => Object.keys(entriesOf(zipFile)).some((entry) => entry.startsWith(directory)),
  };
}

function pluginsDirHolding(artifacts: string[]): string {
  const distribution = fs.mkdtempSync(path.join(os.tmpdir(), 'graphene-check-'));
  fs.mkdirSync(path.join(distribution, 'plugins'));
  artifacts.forEach((artifact) => fs.writeFileSync(path.join(distribution, 'plugins', artifact), ''));
  return distribution;
}

describe('readBundledModules', () => {
  it('reads what a module registers out of its ui/mf-manifest.json', () => {
    const distribution = pluginsDirHolding(['gravitee-gamma-module-esm-1.5.0.zip']);

    const bundled = readBundledModules(
      distribution,
      zipsHolding({ 'gravitee-gamma-module-esm-1.5.0.zip': { 'ui/mf-manifest.json': MANIFEST } }),
    );

    expect(bundled.modules).toEqual([
      {
        module: 'esm',
        artifact: 'gravitee-gamma-module-esm-1.5.0.zip',
        registered: { '@gravitee/graphene-core': '3.13.0' },
      },
    ]);
    expect(bundled.unreadable).toEqual([]);
  });

  // The defect this replaced: any unreadable manifest was treated as "ships no UI", so a module
  // registering a graphene far above the pin passed as clean.
  it('reports a module that ships a ui/ directory but no manifest at the expected path', () => {
    const distribution = pluginsDirHolding(['gravitee-gamma-module-esm-1.5.0.zip']);

    const bundled = readBundledModules(
      distribution,
      zipsHolding({ 'gravitee-gamma-module-esm-1.5.0.zip': { 'ui/browser/mf-manifest.json': MANIFEST } }),
    );

    expect(bundled.modules).toEqual([]);
    expect(bundled.unreadable).toEqual([
      { artifact: 'gravitee-gamma-module-esm-1.5.0.zip', reason: 'it ships a ui/ directory but no ui/mf-manifest.json' },
    ]);
  });

  it('reports a manifest that is not valid JSON', () => {
    const distribution = pluginsDirHolding(['gravitee-gamma-module-esm-1.5.0.zip']);

    const bundled = readBundledModules(
      distribution,
      zipsHolding({ 'gravitee-gamma-module-esm-1.5.0.zip': { 'ui/mf-manifest.json': 'not json' } }),
    );

    expect(bundled.modules).toEqual([]);
    expect(bundled.unreadable).toHaveLength(1);
  });

  it('passes over a module that ships no UI at all, which registers nothing', () => {
    const distribution = pluginsDirHolding(['gravitee-gamma-module-headless-1.0.0.zip']);

    const bundled = readBundledModules(distribution, zipsHolding({ 'gravitee-gamma-module-headless-1.0.0.zip': { 'lib/module.jar': '' } }));

    expect(bundled.modules).toEqual([]);
    expect(bundled.unreadable).toEqual([]);
    expect(bundled.artifacts).toEqual(['gravitee-gamma-module-headless-1.0.0.zip']);
  });

  it('counts every gamma module zip, so a module that lost its UI stays visible', () => {
    const distribution = pluginsDirHolding([
      'gravitee-gamma-module-aim-4.3.0.zip',
      'gravitee-gamma-module-esm-1.5.0.zip',
      'gravitee-apim-policy-unrelated-1.0.0.zip',
    ]);

    const bundled = readBundledModules(
      distribution,
      zipsHolding({
        'gravitee-gamma-module-aim-4.3.0.zip': { 'ui/mf-manifest.json': MANIFEST },
        'gravitee-gamma-module-esm-1.5.0.zip': { 'ui/browser/mf-manifest.json': MANIFEST },
      }),
    );

    expect(bundled.artifacts).toHaveLength(2);
    expect(bundled.modules).toHaveLength(1);
    expect(bundled.unreadable).toHaveLength(1);
  });

  it('fails loudly when there is no distribution to read', () => {
    expect(() => readBundledModules('/nowhere/at/all')).toThrow(/No plugins directory/);
  });
});
