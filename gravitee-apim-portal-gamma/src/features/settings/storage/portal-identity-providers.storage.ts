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
import { PORTAL_IDENTITY_PROVIDERS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import {
    emptyIdpConfiguration,
    normalizeIdentityProvider,
    type PortalIdentityProvider,
    type PortalIdentityProviderInput,
    type PortalIdentityProviderPatch,
} from '../types';

function createIdentityProviderId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `idp-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function getIdentityProvidersByPortalId(portalId: string): Promise<PortalIdentityProvider[]> {
    const providers = await runTransaction<PortalIdentityProvider[]>(
        PORTAL_IDENTITY_PROVIDERS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('portalId');
            return index.getAll(portalId);
        },
    );

    return providers.map(normalizeIdentityProvider).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getEnabledIdentityProvidersByPortalId(
    portalId: string,
): Promise<PortalIdentityProvider[]> {
    const providers = await getIdentityProvidersByPortalId(portalId);
    return providers.filter(provider => provider.enabled);
}

export async function getPortalIdentityProvider(id: string): Promise<PortalIdentityProvider | undefined> {
    const provider = await runTransaction<PortalIdentityProvider | undefined>(
        PORTAL_IDENTITY_PROVIDERS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    return provider ? normalizeIdentityProvider(provider) : undefined;
}

export async function savePortalIdentityProvider(provider: PortalIdentityProvider): Promise<void> {
    await runTransaction(PORTAL_IDENTITY_PROVIDERS_STORE_NAME, 'readwrite', store =>
        store.put(normalizeIdentityProvider(provider)),
    );
}

export async function createPortalIdentityProvider(
    portalId: string,
    input: PortalIdentityProviderInput,
): Promise<PortalIdentityProvider> {
    const now = Date.now();
    const provider = normalizeIdentityProvider({
        id: createIdentityProviderId(),
        portalId,
        type: input.type,
        name: input.name.trim(),
        description: (input.description ?? '').trim(),
        enabled: input.enabled ?? true,
        syncMappings: input.syncMappings ?? false,
        emailRequired: input.emailRequired ?? true,
        configuration: { ...emptyIdpConfiguration(), ...(input.configuration ?? {}) },
        createdAt: now,
        updatedAt: now,
    });
    await savePortalIdentityProvider(provider);
    return provider;
}

export async function updatePortalIdentityProvider(
    providerId: string,
    patch: PortalIdentityProviderPatch,
): Promise<PortalIdentityProvider | undefined> {
    const existing = await getPortalIdentityProvider(providerId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeIdentityProvider({
        ...existing,
        ...patch,
        name: patch.name !== undefined ? patch.name.trim() : existing.name,
        description: patch.description !== undefined ? patch.description.trim() : existing.description,
        configuration: patch.configuration
            ? { ...existing.configuration, ...patch.configuration }
            : existing.configuration,
        updatedAt: Date.now(),
    });
    await savePortalIdentityProvider(updated);
    return updated;
}

export async function setIdentityProviderEnabled(
    providerId: string,
    enabled: boolean,
): Promise<PortalIdentityProvider | undefined> {
    return updatePortalIdentityProvider(providerId, { enabled });
}

export async function deletePortalIdentityProvider(id: string): Promise<void> {
    await runTransaction(PORTAL_IDENTITY_PROVIDERS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteIdentityProvidersForPortal(portalId: string): Promise<void> {
    const providers = await getIdentityProvidersByPortalId(portalId);
    await Promise.all(providers.map(provider => deletePortalIdentityProvider(provider.id)));
}
