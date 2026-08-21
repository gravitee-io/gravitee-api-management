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
import type {
    IdentityProvider,
    IdentityProviderActivation,
    IdentityProviderListItem,
    NewIdentityProviderPayload,
    UpdateIdentityProviderPayload,
} from '../types/identityProvider';

export async function listIdentityProviders(): Promise<IdentityProviderListItem[]> {
    return apimFetchJsonOrg<IdentityProviderListItem[]>('/configuration/identities');
}

export async function listActivatedIdentityProviders(): Promise<IdentityProviderActivation[]> {
    return apimFetchJsonOrg<IdentityProviderActivation[]>('/identities');
}

export async function updateActivatedIdentityProviders(activatedIds: string[]): Promise<void> {
    await apimFetchJsonOrg<void>('/identities', {
        method: 'PUT',
        body: JSON.stringify(activatedIds.map(identityProvider => ({ identityProvider }))),
    });
}

export async function createIdentityProvider(payload: NewIdentityProviderPayload): Promise<IdentityProvider> {
    return apimFetchJsonOrg<IdentityProvider>('/configuration/identities', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function deleteIdentityProvider(id: string): Promise<void> {
    await apimFetchJsonOrg<void>(`/configuration/identities/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

function withMappings(provider: IdentityProvider): IdentityProvider {
    return {
        ...provider,
        groupMappings: provider.groupMappings ?? [],
        roleMappings: provider.roleMappings ?? [],
    };
}

export async function getIdentityProvider(id: string): Promise<IdentityProvider> {
    const provider = await apimFetchJsonOrg<IdentityProvider>(`/configuration/identities/${encodeURIComponent(id)}`);
    return withMappings(provider);
}

export async function updateIdentityProvider(id: string, payload: UpdateIdentityProviderPayload): Promise<IdentityProvider> {
    const provider = await apimFetchJsonOrg<IdentityProvider>(`/configuration/identities/${encodeURIComponent(id)}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
    return withMappings(provider);
}
