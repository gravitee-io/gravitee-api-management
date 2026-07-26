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
import { PORTAL_ACCESS_GRANTS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type {
    PortalAccessGrant,
    PortalAccessGrantInput,
    PortalAccessGrantPatch,
    PortalAccessLevel,
    PortalNavigationOverride,
} from '../types/permissions.types';

function createGrantId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `grant-${crypto.randomUUID()}`;
    }
    return `grant-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

/** Drops provisioning details that only make sense for a `CONSUME` grant. */
function normalizeGrant(grant: PortalAccessGrant): PortalAccessGrant {
    if (grant.access !== 'CONSUME') {
        const { provisioning: _provisioning, defaultPlanId: _defaultPlanId, ...rest } = grant;
        return { ...rest, overrides: grant.overrides ?? [] };
    }

    const provisioning = grant.provisioning ?? 'CLASSIC';
    return {
        ...grant,
        provisioning,
        defaultPlanId: provisioning === 'AUTO' ? grant.defaultPlanId : undefined,
        overrides: grant.overrides ?? [],
    };
}

export async function getGrantsByGroupId(groupId: string): Promise<PortalAccessGrant[]> {
    const grants = await runTransaction<PortalAccessGrant[]>(
        PORTAL_ACCESS_GRANTS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('groupId');
            return index.getAll(groupId);
        },
    );

    return grants.map(normalizeGrant).sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export async function getGrantsByTenantId(tenantId: string): Promise<PortalAccessGrant[]> {
    const grants = await runTransaction<PortalAccessGrant[]>(
        PORTAL_ACCESS_GRANTS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('tenantId');
            return index.getAll(tenantId);
        },
    );
    return grants.map(normalizeGrant);
}

export async function getGrantsByScopeId(scopeId: string): Promise<PortalAccessGrant[]> {
    const grants = await runTransaction<PortalAccessGrant[]>(
        PORTAL_ACCESS_GRANTS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('scopeId');
            return index.getAll(scopeId);
        },
    );
    return grants.map(normalizeGrant);
}

export async function getPortalAccessGrant(id: string): Promise<PortalAccessGrant | undefined> {
    const grant = await runTransaction<PortalAccessGrant | undefined>(
        PORTAL_ACCESS_GRANTS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    return grant ? normalizeGrant(grant) : undefined;
}

export async function savePortalAccessGrant(grant: PortalAccessGrant): Promise<void> {
    await runTransaction(PORTAL_ACCESS_GRANTS_STORE_NAME, 'readwrite', store =>
        store.put(normalizeGrant(grant)),
    );
}

export async function createPortalAccessGrant(input: PortalAccessGrantInput): Promise<PortalAccessGrant> {
    const now = new Date().toISOString();
    const grant = normalizeGrant({
        id: createGrantId(),
        groupId: input.groupId,
        tenantId: input.tenantId,
        scopeType: input.scopeType,
        scopeId: input.scopeId,
        access: input.access,
        provisioning: input.provisioning,
        defaultPlanId: input.defaultPlanId,
        overrides: [],
        createdAt: now,
        updatedAt: now,
    });
    await savePortalAccessGrant(grant);
    return grant;
}

export async function updatePortalAccessGrant(
    id: string,
    patch: PortalAccessGrantPatch,
): Promise<PortalAccessGrant | undefined> {
    const existing = await getPortalAccessGrant(id);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeGrant({
        ...existing,
        ...patch,
        updatedAt: new Date().toISOString(),
    });
    await savePortalAccessGrant(updated);
    return updated;
}

export async function setNavigationOverride(
    grantId: string,
    override: PortalNavigationOverride | { navigationItemId: string; portalId: string; access: 'INHERIT' },
): Promise<PortalAccessGrant | undefined> {
    const existing = await getPortalAccessGrant(grantId);
    if (!existing) {
        return undefined;
    }

    const remaining = existing.overrides.filter(
        item => item.navigationItemId !== override.navigationItemId,
    );
    const overrides =
        override.access === 'INHERIT'
            ? remaining
            : [...remaining, { ...override, access: override.access as PortalAccessLevel | 'NONE' }];

    return updatePortalAccessGrant(grantId, { overrides });
}

export async function deletePortalAccessGrant(id: string): Promise<void> {
    await runTransaction(PORTAL_ACCESS_GRANTS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteGrantsForGroup(groupId: string): Promise<void> {
    const grants = await getGrantsByGroupId(groupId);
    await Promise.all(grants.map(grant => deletePortalAccessGrant(grant.id)));
}
