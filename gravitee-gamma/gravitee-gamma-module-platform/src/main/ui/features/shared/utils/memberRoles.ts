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

export const PRIMARY_OWNER_ROLE = 'PRIMARY_OWNER';

export function formatRoleLabel(role: string): string {
    return role
        .split('_')
        .map(part => part.charAt(0) + part.slice(1).toLowerCase())
        .join(' ');
}

/** Group members carry roles keyed by scope (typically GROUP). */
export function getGroupMemberRole(member: { roles: Record<string, string> }): string {
    return member.roles.GROUP ?? member.roles.APPLICATION ?? Object.values(member.roles)[0] ?? '—';
}
