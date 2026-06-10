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
import { expect } from '@jest/globals';
import { adminAuthHeader, envId, managementV2BaseUrl, orgId } from '@gravitee/utils/api-products';
import fetch from 'node-fetch';

export interface ApiProduct {
  id: string;
  name: string;
  description?: string;
  version: string;
  groups?: string[];
  primaryOwner?: { id: string; displayName: string; type: string };
}

export interface MemberResponse {
  id: string;
  displayName: string;
  roles: { name: string }[];
}

export interface MembersListResponse {
  data: MemberResponse[];
  pagination?: { totalCount: number };
}

export async function createApiProduct(name: string): Promise<ApiProduct> {
  const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products`, {
    method: 'POST',
    headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description: 'E2E test product', version: '1.0.0' }),
  });
  expect(response.status).toEqual(201);
  return (await response.json()) as ApiProduct;
}

export async function getApiProduct(apiProductId: string): Promise<ApiProduct> {
  const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}`, {
    method: 'GET',
    headers: { Authorization: adminAuthHeader },
  });
  expect(response.status).toEqual(200);
  return (await response.json()) as ApiProduct;
}

export async function updateApiProduct(apiProductId: string, body: Record<string, unknown>): Promise<ApiProduct> {
  const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}`, {
    method: 'PUT',
    headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  expect(response.status).toEqual(200);
  return (await response.json()) as ApiProduct;
}

export async function deleteApiProduct(apiProductId: string): Promise<void> {
  await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}`, {
    method: 'DELETE',
    headers: { Authorization: adminAuthHeader },
  });
}

export async function listMembers(apiProductId: string): Promise<MembersListResponse> {
  const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}/members`, {
    method: 'GET',
    headers: { Authorization: adminAuthHeader },
  });
  expect(response.status).toEqual(200);
  return (await response.json()) as MembersListResponse;
}

export async function addMember(apiProductId: string, userId: string, roleName: string): Promise<{ status: number; body: any }> {
  const response = await fetch(`${managementV2BaseUrl}/environments/${envId}/api-products/${apiProductId}/members`, {
    method: 'POST',
    headers: { Authorization: adminAuthHeader, 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, roleName }),
  });
  const body = response.status !== 204 ? await response.json() : null;
  return { status: response.status, body };
}

export async function lookupUserId(username: string): Promise<string> {
  const response = await fetch(
    `${managementV2BaseUrl.replace('/v2', '')}/organizations/${orgId}/environments/${envId}/users?q=${username}`,
    {
      method: 'GET',
      headers: { Authorization: adminAuthHeader },
    },
  );
  expect(response.status).toEqual(200);
  const body = (await response.json()) as { data: { id: string; displayName: string }[] };
  const user = body.data.find((u) => u.displayName === username);
  expect(user).toBeDefined();
  return user.id;
}
