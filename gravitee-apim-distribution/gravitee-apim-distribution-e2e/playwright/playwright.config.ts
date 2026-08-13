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
import { defineConfig, devices } from '@playwright/test';
import { ADMIN_AUTH_FILE, CONSOLE_BASE_URL } from './utils/config';

export default defineConfig({
  testDir: './tests',
  outputDir: '.tmp/playwright/test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  workers: process.env.CI ? 3 : undefined,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI
    ? [
        ['list'],
        ['html', { outputFolder: '.tmp/playwright/html-report', open: 'never' }],
        ['junit', { outputFile: '.tmp/playwright/junit/results.xml' }],
      ]
    : [['list'], ['html', { outputFolder: '.tmp/playwright/html-report', open: 'never' }]],
  use: {
    baseURL: CONSOLE_BASE_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testDir: './fixtures',
      testMatch: /global\.setup\.ts/,
    },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], storageState: ADMIN_AUTH_FILE },
      dependencies: ['setup'],
    },
  ],
});
