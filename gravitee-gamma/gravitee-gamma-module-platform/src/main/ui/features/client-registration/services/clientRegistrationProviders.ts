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
import {
    toClientRegistrationProviderUpdateBody,
    type ClientRegistrationProvider,
    type ClientRegistrationProviderWrite,
} from '../types/clientRegistrationProvider';

const PROVIDERS_PATH = '/configuration/applications/registration/providers';

function providerPath(providerId: string): string {
    return `${PROVIDERS_PATH}/${encodeURIComponent(providerId)}`;
}

/** Classic `ClientRegistrationProvidersService.list()` → GET `{env.baseURL}/configuration/applications/registration/providers` */
export async function listClientRegistrationProviders(environmentId: string): Promise<ClientRegistrationProvider[]> {
    return apimFetchJsonV1Env<ClientRegistrationProvider[]>(environmentId, PROVIDERS_PATH);
}

/** Classic `get(id)` → GET `.../providers/{id}` */
export async function getClientRegistrationProvider(environmentId: string, providerId: string): Promise<ClientRegistrationProvider> {
    return apimFetchJsonV1Env<ClientRegistrationProvider>(environmentId, providerPath(providerId));
}

/** Classic `create(provider)` → POST `.../providers` with the full provider object (no id). */
export async function createClientRegistrationProvider(
    environmentId: string,
    provider: ClientRegistrationProviderWrite,
): Promise<ClientRegistrationProvider> {
    return apimFetchJsonV1Env<ClientRegistrationProvider>(environmentId, PROVIDERS_PATH, {
        method: 'POST',
        body: JSON.stringify(provider),
    });
}

/** Classic `update(provider)` → PUT `.../providers/{id}` with the explicit field whitelist (no id in body). */
export async function updateClientRegistrationProvider(
    environmentId: string,
    providerId: string,
    provider: ClientRegistrationProviderWrite,
): Promise<ClientRegistrationProvider> {
    return apimFetchJsonV1Env<ClientRegistrationProvider>(environmentId, providerPath(providerId), {
        method: 'PUT',
        body: JSON.stringify(toClientRegistrationProviderUpdateBody(provider)),
    });
}

/** Classic `delete(id)` → DELETE `.../providers/{id}` (no body). */
export async function deleteClientRegistrationProvider(environmentId: string, providerId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, providerPath(providerId), {
        method: 'DELETE',
    });
}
