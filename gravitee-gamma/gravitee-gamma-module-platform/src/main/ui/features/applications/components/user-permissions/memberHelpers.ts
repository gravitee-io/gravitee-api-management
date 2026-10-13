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
import type { ApplicationUiMember } from '../../types/applicationMembers.types';

const APPLICATION_SCOPE = 'APPLICATION';

export function getApplicationRole(member: ApplicationUiMember): string {
    return member.roles?.find(role => role.scope === APPLICATION_SCOPE)?.name ?? member.roles?.[0]?.name ?? '';
}

export function isMemberPrimaryOwner(member: ApplicationUiMember): boolean {
    return member.roles?.some(role => role.name === 'PRIMARY_OWNER') ?? false;
}
