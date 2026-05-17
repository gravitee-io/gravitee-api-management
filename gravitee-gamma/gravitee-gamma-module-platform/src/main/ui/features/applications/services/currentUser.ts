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
import type { UserRole } from '@gravitee/gamma-modules-sdk/types';

import { apimFetchJsonOrg } from '../../../shared/api/apimClient';

export interface CurrentUser {
    displayName?: string;
    roles?: UserRole[];
}

/** GET /organizations/{orgId}/user — same resource as the control-plane auth store. */
export async function fetchCurrentUser(): Promise<CurrentUser> {
    return apimFetchJsonOrg<CurrentUser>('/user');
}

export function hasOrganizationAdminRole(roles: UserRole[] | undefined): boolean {
    return roles?.some(role => role.scope === 'ORGANIZATION' && role.name === 'ADMIN') ?? false;
}
