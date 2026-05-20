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
import { apimFetchJsonOrg, apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { ApplicationRole, BroadcastPayload } from '../types/broadcast';

/** Fetch APPLICATION-scoped roles — used to build the recipients dropdown. */
export async function listApplicationRoles(): Promise<ApplicationRole[]> {
    return apimFetchJsonOrg<ApplicationRole[]>('/configuration/rolescopes/APPLICATION/roles');
}

/** POST a broadcast message to all API consumers. Returns the number of recipients reached. */
export async function sendBroadcast(envId: string, apiId: string, payload: BroadcastPayload): Promise<number> {
    return apimFetchJsonV1Env<number>(envId, `/apis/${encodeURIComponent(apiId)}/messages`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}
