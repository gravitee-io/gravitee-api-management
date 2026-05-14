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
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { ApplicationGroup, ApplicationTypeConfig } from '../types/applicationCreate';

/** GET /organizations/{orgId}/environments/{envId}/configuration/applications/types (console ApplicationTypesService). */
export async function listApplicationTypes(environmentId: string): Promise<ApplicationTypeConfig[]> {
    return apimFetchJsonV1Env<ApplicationTypeConfig[]>(environmentId, '/configuration/applications/types');
}

/** GET /organizations/{orgId}/environments/{envId}/configuration/groups (console GroupService). */
export async function listApplicationGroups(environmentId: string): Promise<ApplicationGroup[]> {
    return apimFetchJsonV1Env<ApplicationGroup[]>(environmentId, '/configuration/groups');
}
