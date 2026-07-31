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
import type { Metadata, NewMetadataPayload, UpdateMetadataPayload } from '../types/metadata';

export async function listEnvironmentMetadata(environmentId: string): Promise<Metadata[]> {
    return apimFetchJsonV1Env<Metadata[]>(environmentId, '/configuration/metadata');
}

export async function createEnvironmentMetadata(environmentId: string, data: NewMetadataPayload): Promise<Metadata> {
    return apimFetchJsonV1Env<Metadata>(environmentId, '/configuration/metadata', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateEnvironmentMetadata(environmentId: string, data: UpdateMetadataPayload): Promise<Metadata> {
    return apimFetchJsonV1Env<Metadata>(environmentId, '/configuration/metadata', {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteEnvironmentMetadata(environmentId: string, key: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/metadata/${encodeURIComponent(key)}`, {
        method: 'DELETE',
    });
}
