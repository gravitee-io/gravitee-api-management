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

export interface MemberRole {
    name: string;
    scope: string;
}

export interface Member {
    id: string;
    displayName: string;
    roles: MemberRole[];
}

export interface MembersResponse {
    data: Member[];
    pagination: {
        page: number;
        perPage: number;
        totalCount: number;
        pageCount: number;
        pageItemsCount: number;
    };
}

export interface Group {
    id: string;
    name: string;
    apiPrimaryOwner?: string | null;
}

export interface GroupsResponse {
    data: Group[];
    pagination: {
        page: number;
        perPage: number;
        totalCount: number;
        pageCount: number;
        pageItemsCount: number;
    };
}

/** Map of group name → members array (roles keyed by scope) */
export interface GroupMember {
    id: string;
    displayName: string;
    roles: Record<string, string>;
}

export type GroupMembersMap = Record<string, GroupMember[]>;

export interface ApiRole {
    name: string;
    scope: string;
    system?: boolean;
}

export interface SearchableUser {
    id?: string | null;
    reference: string;
    displayName: string;
    email?: string;
}

export interface TransferOwnershipPayload {
    userId?: string;
    userReference?: string;
    userType: 'USER' | 'GROUP';
    poRole: string;
}
