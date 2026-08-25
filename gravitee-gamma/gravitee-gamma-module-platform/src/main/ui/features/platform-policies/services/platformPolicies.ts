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
import type { Organization } from '../types/platformPolicies';

/** Platform flows are stored on the organization entity itself — there is no dedicated flows endpoint. */
export function getOrganization(): Promise<Organization> {
    return apimFetchJsonOrg<Organization>('');
}

/**
 * Full-entity update: every field read from the organization has to be sent back. The resource binds the
 * body to `UpdateOrganizationEntity`, which is `OrganizationEntity` without `id`; the extra `id` we send
 * is ignored, the mapper has `FAIL_ON_UNKNOWN_PROPERTIES` disabled.
 */
export function updateOrganization(organization: Organization): Promise<Organization> {
    return apimFetchJsonOrg<Organization>('', { method: 'PUT', body: JSON.stringify(organization) });
}
