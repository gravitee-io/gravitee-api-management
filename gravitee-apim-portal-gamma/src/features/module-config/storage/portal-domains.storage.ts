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
import { PORTAL_DOMAINS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import {
    normalizePortalDomain,
    type PortalDomain,
    type PortalDomainInput,
    type PortalDomainPatch,
} from '../types';

function createDomainId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `domain-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function listPortalDomains(): Promise<PortalDomain[]> {
    const domains = await runTransaction<PortalDomain[]>(PORTAL_DOMAINS_STORE_NAME, 'readonly', store =>
        store.getAll(),
    );

    return domains.map(normalizePortalDomain).sort((a, b) => a.hostname.localeCompare(b.hostname));
}

export async function getPortalDomain(id: string): Promise<PortalDomain | undefined> {
    const domain = await runTransaction<PortalDomain | undefined>(PORTAL_DOMAINS_STORE_NAME, 'readonly', store =>
        store.get(id),
    );
    return domain ? normalizePortalDomain(domain) : undefined;
}

export async function savePortalDomain(domain: PortalDomain): Promise<void> {
    await runTransaction(PORTAL_DOMAINS_STORE_NAME, 'readwrite', store => store.put(normalizePortalDomain(domain)));
}

async function clearPrimaryForPortal(portalId: string, exceptId?: string): Promise<void> {
    const domains = await listPortalDomains();
    await Promise.all(
        domains
            .filter(domain => domain.portalId === portalId && domain.primary && domain.id !== exceptId)
            .map(domain =>
                savePortalDomain({
                    ...domain,
                    primary: false,
                    updatedAt: Date.now(),
                }),
            ),
    );
}

export async function createPortalDomain(input: PortalDomainInput): Promise<PortalDomain> {
    const now = Date.now();
    const primary = input.primary ?? false;
    if (primary) {
        await clearPrimaryForPortal(input.portalId);
    }

    const domain = normalizePortalDomain({
        id: createDomainId(),
        hostname: input.hostname.trim().toLowerCase(),
        portalId: input.portalId,
        status: input.status ?? 'Pending',
        primary,
        createdAt: now,
        updatedAt: now,
    });
    await savePortalDomain(domain);
    return domain;
}

export async function updatePortalDomain(
    domainId: string,
    patch: PortalDomainPatch,
): Promise<PortalDomain | undefined> {
    const existing = await getPortalDomain(domainId);
    if (!existing) {
        return undefined;
    }

    const portalId = patch.portalId ?? existing.portalId;
    const primary = patch.primary ?? existing.primary;
    if (primary) {
        await clearPrimaryForPortal(portalId, domainId);
    }

    const updated = normalizePortalDomain({
        ...existing,
        ...patch,
        hostname: patch.hostname !== undefined ? patch.hostname.trim().toLowerCase() : existing.hostname,
        portalId,
        primary,
        updatedAt: Date.now(),
    });
    await savePortalDomain(updated);
    return updated;
}

export async function deletePortalDomain(id: string): Promise<void> {
    await runTransaction(PORTAL_DOMAINS_STORE_NAME, 'readwrite', store => store.delete(id));
}
