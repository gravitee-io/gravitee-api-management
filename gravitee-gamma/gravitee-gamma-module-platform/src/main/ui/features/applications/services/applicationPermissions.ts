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
import { normalizeCrudMapRecord } from '@gravitee/gamma-modules-sdk';

import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';

/**
 * Fetches the current user's permission set for a specific application and returns
 * normalized flat strings ready for `permissionService.load('application', ...)`.
 */
export async function getApplicationPermissions(environmentId: string, applicationId: string): Promise<string[]> {
    const raw = await apimFetchJsonV1Env<Record<string, string[]>>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members/permissions`,
    );
    return normalizeCrudMapRecord('application', raw);
}
