/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import {
    emptyIdpConfiguration,
    normalizeTransversalIdentityProvider,
    type TransversalIdentityProvider,
    type TransversalIdentityProviderInput,
    type TransversalIdentityProviderPatch,
} from '../types';

function createProviderId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `tidp-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function listTransversalIdentityProviders(): Promise<TransversalIdentityProvider[]> {
    const providers = await runTransaction<TransversalIdentityProvider[]>(
        TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME,
        'readonly',
        store => store.getAll(),
    );

    return providers.map(normalizeTransversalIdentityProvider).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getTransversalIdentityProvider(
    id: string,
): Promise<TransversalIdentityProvider | undefined> {
    const provider = await runTransaction<TransversalIdentityProvider | undefined>(
        TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    return provider ? normalizeTransversalIdentityProvider(provider) : undefined;
}

export async function saveTransversalIdentityProvider(provider: TransversalIdentityProvider): Promise<void> {
    await runTransaction(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME, 'readwrite', store =>
        store.put(normalizeTransversalIdentityProvider(provider)),
    );
}

export async function createTransversalIdentityProvider(
    input: TransversalIdentityProviderInput,
): Promise<TransversalIdentityProvider> {
    const now = Date.now();
    const provider = normalizeTransversalIdentityProvider({
        id: createProviderId(),
        type: input.type,
        name: input.name.trim(),
        description: (input.description ?? '').trim(),
        enabled: input.enabled ?? true,
        syncMappings: input.syncMappings ?? false,
        emailRequired: input.emailRequired ?? true,
        configuration: { ...emptyIdpConfiguration(), ...(input.configuration ?? {}) },
        portalIds: [...(input.portalIds ?? [])],
        createdAt: now,
        updatedAt: now,
    });
    await saveTransversalIdentityProvider(provider);
    return provider;
}

export async function updateTransversalIdentityProvider(
    providerId: string,
    patch: TransversalIdentityProviderPatch,
): Promise<TransversalIdentityProvider | undefined> {
    const existing = await getTransversalIdentityProvider(providerId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeTransversalIdentityProvider({
        ...existing,
        ...patch,
        name: patch.name !== undefined ? patch.name.trim() : existing.name,
        description: patch.description !== undefined ? patch.description.trim() : existing.description,
        configuration: patch.configuration
            ? { ...existing.configuration, ...patch.configuration }
            : existing.configuration,
        portalIds: patch.portalIds !== undefined ? [...patch.portalIds] : existing.portalIds,
        updatedAt: Date.now(),
    });
    await saveTransversalIdentityProvider(updated);
    return updated;
}

export async function setTransversalIdentityProviderEnabled(
    providerId: string,
    enabled: boolean,
): Promise<TransversalIdentityProvider | undefined> {
    return updateTransversalIdentityProvider(providerId, { enabled });
}

export async function deleteTransversalIdentityProvider(id: string): Promise<void> {
    await runTransaction(TRANSVERSAL_IDENTITY_PROVIDERS_STORE_NAME, 'readwrite', store => store.delete(id));
}
