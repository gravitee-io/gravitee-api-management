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
import 'dotenv/config';
import path from 'path';

/**
 * Playwright-specific configuration only.
 *
 * Everything the Jest api-test suite already defines — management/portal/gateway base URLs and
 * user credentials — is read from `.env` via `@gravitee/utils/configuration`. Do not redeclare it
 * here: a second copy is exactly how the two suites drift apart.
 */
function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable ${name}. Is .env present in the package root?`);
  }
  return value;
}

/**
 * Playwright resolves `page.goto(path)` as `new URL(path, baseURL)`. Without a trailing slash the
 * base URL's last segment is treated as a file and dropped, so a path-prefixed deployment such as
 * `http://nginx/console` would resolve to the origin root — the Portal, not the Console. Page
 * objects must also pass relative paths (`''`, `'#!/…'`), because a leading `/` resets to the root
 * regardless of the trailing slash.
 */
const consoleBaseUrl = requiredEnv('CONSOLE_BASE_URL');
export const CONSOLE_BASE_URL = consoleBaseUrl.endsWith('/') ? consoleBaseUrl : `${consoleBaseUrl}/`;

export const MANAGEMENT_BASE_URL = requiredEnv('MANAGEMENT_BASE_URL');

const AUTH_DIR = path.join(__dirname, '..', 'fixtures', '.auth');

export const ADMIN_AUTH_FILE = path.join(AUTH_DIR, 'admin.json');
export const API_PUBLISHER_AUTH_FILE = path.join(AUTH_DIR, 'api-publisher.json');
