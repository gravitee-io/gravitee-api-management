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
import { PORTAL_GROUPS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import { slugifyTitle } from '../../portals/utils/slug';
import {
    DEFAULT_PORTAL_TENANT_FEATURES,
    type PortalTenantFeatures,
} from '../../tenants/types/portal-tenant.types';
import type { PortalGroup, PortalGroupManagementMode } from '../types/permissions.types';

export interface PortalGroupInput {
    tenantId: string;
    name: string;
    description?: string;
    managementMode?: PortalGroupManagementMode;
    features?: PortalTenantFeatures;
}

function createGroupId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `group-${crypto.randomUUID()}`;
    }
    return `group-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

/** Older IndexedDB rows may predate group features; fill defaults without writing. */
export function normalizePortalGroup(group: PortalGroup): PortalGroup {
    return {
        ...group,
        features: group.features ?? { ...DEFAULT_PORTAL_TENANT_FEATURES },
    };
}

export async function getGroupsByTenantId(tenantId: string): Promise<PortalGroup[]> {
    const groups = await runTransaction<PortalGroup[]>(PORTAL_GROUPS_STORE_NAME, 'readonly', store => {
        const index = store.index('tenantId');
        return index.getAll(tenantId);
    });

    return groups.map(normalizePortalGroup).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getAllPortalGroups(): Promise<PortalGroup[]> {
    const groups = await runTransaction<PortalGroup[]>(PORTAL_GROUPS_STORE_NAME, 'readonly', store =>
        store.getAll(),
    );
    return groups.map(normalizePortalGroup).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getPortalGroup(id: string): Promise<PortalGroup | undefined> {
    const group = await runTransaction<PortalGroup | undefined>(PORTAL_GROUPS_STORE_NAME, 'readonly', store =>
        store.get(id),
    );
    return group ? normalizePortalGroup(group) : undefined;
}

export async function savePortalGroup(group: PortalGroup): Promise<void> {
    await runTransaction(PORTAL_GROUPS_STORE_NAME, 'readwrite', store =>
        store.put(normalizePortalGroup(group)),
    );
}

export async function createPortalGroup(input: PortalGroupInput): Promise<PortalGroup> {
    const now = new Date().toISOString();
    const name = input.name.trim();
    const group: PortalGroup = {
        id: createGroupId(),
        tenantId: input.tenantId,
        name,
        hrid: slugifyTitle(name),
        description: input.description?.trim() || undefined,
        managementMode: input.managementMode ?? 'SELF_MANAGED',
        features: input.features ?? { ...DEFAULT_PORTAL_TENANT_FEATURES },
        createdAt: now,
        updatedAt: now,
    };
    await savePortalGroup(group);
    return group;
}

export async function renamePortalGroup(id: string, name: string): Promise<PortalGroup | undefined> {
    const existing = await getPortalGroup(id);
    if (!existing) {
        return undefined;
    }

    const trimmed = name.trim();
    const updated: PortalGroup = {
        ...existing,
        name: trimmed,
        hrid: slugifyTitle(trimmed),
        updatedAt: new Date().toISOString(),
    };
    await savePortalGroup(updated);
    return updated;
}

export async function updatePortalGroupFeatures(
    id: string,
    features: PortalTenantFeatures,
): Promise<PortalGroup | undefined> {
    const existing = await getPortalGroup(id);
    if (!existing) {
        return undefined;
    }

    const updated: PortalGroup = {
        ...existing,
        features,
        updatedAt: new Date().toISOString(),
    };
    await savePortalGroup(updated);
    return updated;
}

export async function deletePortalGroup(id: string): Promise<void> {
    await runTransaction(PORTAL_GROUPS_STORE_NAME, 'readwrite', store => store.delete(id));
}
