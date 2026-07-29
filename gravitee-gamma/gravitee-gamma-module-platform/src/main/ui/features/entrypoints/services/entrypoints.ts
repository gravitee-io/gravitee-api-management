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
import type { Entrypoint, NewEntrypointPayload, UpdateEntrypointPayload } from '../types/entrypoint';

export async function listEntrypoints(): Promise<Entrypoint[]> {
    return apimFetchJsonOrg<Entrypoint[]>('/configuration/entrypoints');
}

export async function createEntrypoint(data: NewEntrypointPayload): Promise<Entrypoint> {
    return apimFetchJsonOrg<Entrypoint>('/configuration/entrypoints', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateEntrypoint(data: UpdateEntrypointPayload): Promise<Entrypoint> {
    return apimFetchJsonOrg<Entrypoint>('/configuration/entrypoints', {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteEntrypoint(id: string): Promise<void> {
    return apimFetchJsonOrg<void>(`/configuration/entrypoints/${encodeURIComponent(id)}`, {
        method: 'DELETE',
    });
}
