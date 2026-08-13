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
export const CONSOLE_BASE_URL = process.env.CONSOLE_BASE_URL ?? 'http://localhost:4000';

export const ADMIN_AUTH_FILE = path.join(__dirname, '..', 'fixtures', '.auth', 'admin.json');
