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

interface AssetEntry {
  glob: string;
  input: string;
  output: string;
}

const SWAGGER_UI_DIST = 'node_modules/swagger-ui/dist';

function globToRegExp(glob: string): RegExp {
  const escaped = glob
    .replace(/[.+^${}()|[\]\\]/g, '\\$&')
    .replace(/\*/g, '[^/]*')
    .replace(/\?/g, '[^/]');
  return new RegExp(`^${escaped}$`);
}

function isCopiedFromSwaggerUiToRoot(file: string): boolean {
  const angularJson = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../../angular.json'), 'utf8'));
  const assets: unknown[] = angularJson.projects['gravitee-portal-webui'].architect.build.options.assets;
  return assets.some(asset => {
    const entry = asset as AssetEntry;
    return typeof asset === 'object' && entry.input === SWAGGER_UI_DIST && entry.output === '/' && globToRegExp(entry.glob).test(file);
  });
}

describe('swagger-ui OAuth2 redirect assets packaging', () => {
  it('should copy oauth2-redirect.html to the build output root', () => {
    expect(isCopiedFromSwaggerUiToRoot('oauth2-redirect.html')).toBe(true);
  });

  it('should copy oauth2-redirect.js next to oauth2-redirect.html in the build output', () => {
    expect(isCopiedFromSwaggerUiToRoot('oauth2-redirect.js')).toBe(true);
  });

  it('should copy every script the shipped oauth2-redirect.html loads', () => {
    const redirectPage = fs.readFileSync(path.resolve(__dirname, '../..', SWAGGER_UI_DIST, 'oauth2-redirect.html'), 'utf8');
    const scripts = Array.from(redirectPage.matchAll(/<script[^>]*\ssrc="([^"]+)"/g), match => match[1]);

    expect(scripts.filter(script => !isCopiedFromSwaggerUiToRoot(script))).toEqual([]);
  });
});
