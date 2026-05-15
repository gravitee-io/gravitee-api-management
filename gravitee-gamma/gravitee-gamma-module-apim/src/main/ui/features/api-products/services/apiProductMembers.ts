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
import { apimFetchJsonOrg, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ApiRole, Member, MembersResponse, TransferOwnershipPayload } from '../../apis/types/members.types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function getApiProductMembers(environmentId: string, productId: string): Promise<MembersResponse> {
    return apimFetchJsonV2<MembersResponse>(environmentId, `/api-products/${productId}/members`);
}

export async function addApiProductMember(
    environmentId: string,
    productId: string,
    payload: { userId?: string; externalReference?: string; roleName: string },
): Promise<Member> {
    return apimFetchJsonV2<Member>(environmentId, `/api-products/${productId}/members`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(payload),
    });
}

export async function updateApiProductMember(
    environmentId: string,
    productId: string,
    memberId: string,
    roleName: string,
): Promise<Member> {
    return apimFetchJsonV2<Member>(environmentId, `/api-products/${productId}/members/${memberId}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ memberId, roleName }),
    });
}

export async function deleteApiProductMember(environmentId: string, productId: string, memberId: string): Promise<void> {
    return apimFetchJsonV2<void>(environmentId, `/api-products/${productId}/members/${memberId}`, { method: 'DELETE' });
}

export async function transferApiProductOwnership(
    environmentId: string,
    productId: string,
    payload: TransferOwnershipPayload,
): Promise<void> {
    // API Products uses different field names than regular APIs
    const body = {
        newPrimaryOwnerId: payload.userId,
        userReference: payload.userReference,
        userType: payload.userType,
        currentPrimaryOwnerNewRole: payload.poRole,
    };
    return apimFetchJsonV2<void>(environmentId, `/api-products/${productId}/members/_transfer-ownership`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}

export async function getApiProductRoles(): Promise<ApiRole[]> {
    return apimFetchJsonOrg<ApiRole[]>(`/configuration/rolescopes/API_PRODUCT/roles`);
}
