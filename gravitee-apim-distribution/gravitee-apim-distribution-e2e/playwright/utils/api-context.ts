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
import path from 'path';
import { APIRequestContext, request } from '@playwright/test';
import { BasicAuthentication } from '@model/users';

export const ADMIN_AUTH_FILE = path.join(__dirname, '..', 'fixtures', '.auth', 'admin.json');

export const MANAGEMENT_API_URL = process.env.PW_MANAGEMENT_API ?? 'http://localhost:8083';
export const GATEWAY_URL = process.env.PW_GATEWAY_SERVER ?? 'http://localhost:8082';
export const DEFAULT_ORG_PATH = '/management/organizations/DEFAULT';
export const DEFAULT_ORG_ENV_PATH = `${DEFAULT_ORG_PATH}/environments/DEFAULT`;

// cy.request's `auth` option performed HTTP Basic auth automatically; APIRequestContext has no
// per-request equivalent, so callers merge this header in explicitly.
export function basicAuthHeader(auth: BasicAuthentication): { Authorization: string } {
  const token = Buffer.from(`${auth.username}:${auth.password}`).toString('base64');
  return { Authorization: `Basic ${token}` };
}

export function newManagementApiContext(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: MANAGEMENT_API_URL });
}
