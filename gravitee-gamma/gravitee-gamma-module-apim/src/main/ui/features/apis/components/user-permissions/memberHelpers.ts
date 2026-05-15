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
import type { Member, SearchableUser } from '../../types/members.types';

export type EditState = { memberId: string; role: string } | null;

export function getApiRole(member: Member): string {
    return member.roles?.find(role => role.scope === 'API')?.name ?? '';
}

export function getApiProductRole(member: Member): string {
    return member.roles?.find(role => role.scope === 'API_PRODUCT')?.name ?? member.roles?.[0]?.name ?? '';
}

export function isMemberPrimaryOwner(member: Member): boolean {
    return member.roles?.some(role => role.name === 'PRIMARY_OWNER') ?? false;
}

export function isSameUser(userA: SearchableUser, userB: SearchableUser): boolean {
    if (userA.id !== null && userA.id !== undefined && userB.id !== null && userB.id !== undefined) return userA.id === userB.id;
    return userA.reference === userB.reference;
}
