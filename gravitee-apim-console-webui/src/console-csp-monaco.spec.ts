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
import * as path from 'path';

const directive = (csp: string, name: string): string[] => {
  const found = csp
    .split(';')
    .map(d => d.trim())
    .find(d => d.startsWith(`${name} `));
  return found ? found.split(/\s+/).slice(1) : [];
};

describe('Console nginx CSP with Monaco 0.56', () => {
  it.each(['default.conf', 'default.no-ipv6.conf'])('%s lets Monaco create blob: workers and load its data: font', file => {
    const conf = fs.readFileSync(path.resolve(__dirname, '../docker/config', file), 'utf8');
    const csp = /Content-Security-Policy\s+"([^"]*)"/.exec(conf)?.[1] ?? '';

    // Workers fall back to script-src when worker-src is absent
    const workerSources = directive(csp, 'worker-src').length ? directive(csp, 'worker-src') : directive(csp, 'script-src');
    expect(workerSources).toEqual(expect.arrayContaining(["'self'", 'blob:']));
    expect(directive(csp, 'font-src')).toEqual(expect.arrayContaining(["'self'", 'data:']));
  });
});
