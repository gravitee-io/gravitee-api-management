/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';
import type { IdentityProviderListItem, NewPreRegisterUserPayload, OrganizationUser, OrganizationUserListResponse } from '../types/user';

export async function listOrganizationUsers(params: { query: string; page: number; size: number }): Promise<OrganizationUserListResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('size', String(params.size));
    // Sent verbatim: the backend truncates at the first '@' so full email addresses still match.
    const q = params.query.trim();
    if (q) {
        searchParams.set('q', q);
    }
    return apimFetchJsonOrg<OrganizationUserListResponse>(`/users?${searchParams.toString()}`);
}

export async function createOrganizationUser(payload: NewPreRegisterUserPayload): Promise<OrganizationUser> {
    return apimFetchJsonOrg<OrganizationUser>('/users', { method: 'POST', body: JSON.stringify(payload) });
}

export async function listIdentityProviders(): Promise<IdentityProviderListItem[]> {
    return apimFetchJsonOrg<IdentityProviderListItem[]>('/configuration/identities');
}
