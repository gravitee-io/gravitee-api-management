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

/** v1 Management API member (GET/POST /applications/{id}/members). */
export interface ApplicationMemberEntity {
    id: string;
    displayName?: string;
    role: string;
}

export interface MemberRole {
    name: string;
    scope: string;
}

/** UI member shape aligned with API user-permissions tables. */
export interface ApplicationUiMember {
    id: string;
    displayName: string;
    email?: string;
    roles: MemberRole[];
}

export interface ApplicationRole {
    name: string;
    scope: string;
    system?: boolean;
    default?: boolean;
}

export interface EnvironmentGroup {
    id: string;
    name: string;
}

export interface GroupsPagedResponse {
    data: EnvironmentGroup[];
}

export interface GroupMember {
    id: string;
    displayName: string;
    roles: Record<string, string>;
}

export type GroupMembersMap = Record<string, GroupMember[]>;

export interface SearchableUser {
    id?: string | null;
    reference: string;
    displayName: string;
    email?: string;
}

export interface ApplicationTransferOwnershipPayload {
    id?: string;
    reference?: string;
    role?: string;
}
