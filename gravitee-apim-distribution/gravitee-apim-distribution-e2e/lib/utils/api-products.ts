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
import { ADMIN_USER, API_USER, forManagementAsAdminUser, SIMPLE_USER } from './configuration';
import { ApplicationsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationsApi';

const managementV2BaseUrl = process.env.MANAGEMENT_V2_BASE_URL;
const adminUsername = ADMIN_USER.username;
const adminPassword = ADMIN_USER.password;

if (!managementV2BaseUrl) {
  throw new Error('MANAGEMENT_V2_BASE_URL must be defined to run API Product e2e tests');
}
if (!adminUsername || !adminPassword) {
  throw new Error('ADMIN_USERNAME and ADMIN_PASSWORD must be defined to run API Product e2e tests');
}

const createBasicAuthHeader = (user: { username?: string; password?: string }) =>
  user?.username && user?.password ? `Basic ${Buffer.from(`${user.username}:${user.password}`).toString('base64')}` : undefined;

export { managementV2BaseUrl };
export const adminAuthHeader = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`;
export const apiAuthHeader = createBasicAuthHeader(API_USER);
export const simpleAuthHeader = createBasicAuthHeader(SIMPLE_USER);
export const applicationsResourceAsAdmin = new ApplicationsApi(forManagementAsAdminUser());

export const envId = 'DEFAULT';
export const orgId = 'DEFAULT';
