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
import { CONSOLE_DOC_GRANTS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { ConsoleDocGrant, ConsoleDocGrantInput, ConsoleDocRole } from '../types/permissions.types';

const SEED_CREATED_AT = '2026-01-10T09:00:00.000Z';

/**
 * Two authoring stories side by side: the Payments team owns its own API documentation, while the
 * portal managers own the portal itself and the docs of APIs whose teams do not want to write them.
 */
const SEED_GRANTS: readonly ConsoleDocGrant[] = [
    {
        id: 'console-grant-payments-owner',
        principalType: 'TEAM',
        principalId: 'team-payments-api',
        scopeType: 'API',
        scopeId: 'api-payments',
        role: 'OWNER',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
    {
        id: 'console-grant-payments-reviewer',
        principalType: 'USER',
        principalId: 'console-user-nina',
        scopeType: 'API',
        scopeId: 'api-payments',
        role: 'AUTHOR',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
    {
        id: 'console-grant-accounts-managed-centrally',
        principalType: 'TEAM',
        principalId: 'team-portal-managers',
        scopeType: 'API',
        scopeId: 'api-accounts',
        role: 'OWNER',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
    {
        id: 'console-grant-accounts-team-reader',
        principalType: 'TEAM',
        principalId: 'team-accounts-api',
        scopeType: 'API',
        scopeId: 'api-accounts',
        role: 'READER',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
    {
        id: 'console-grant-portal-managers',
        principalType: 'TEAM',
        principalId: 'team-portal-managers',
        scopeType: 'PORTAL',
        scopeId: 'portal-payments',
        role: 'OWNER',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
    {
        id: 'console-grant-ai-workspace',
        principalType: 'TEAM',
        principalId: 'team-ai-platform',
        scopeType: 'AI_WORKSPACE',
        scopeId: 'ai-workspace-enterprise',
        role: 'OWNER',
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_CREATED_AT,
    },
];

function createConsoleGrantId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `console-grant-${crypto.randomUUID()}`;
    }
    return `console-grant-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function getAllConsoleDocGrants(): Promise<ConsoleDocGrant[]> {
    return runTransaction<ConsoleDocGrant[]>(CONSOLE_DOC_GRANTS_STORE_NAME, 'readonly', store => store.getAll());
}

export async function getConsoleDocGrantsByScopeId(scopeId: string): Promise<ConsoleDocGrant[]> {
    return runTransaction<ConsoleDocGrant[]>(CONSOLE_DOC_GRANTS_STORE_NAME, 'readonly', store => {
        const index = store.index('scopeId');
        return index.getAll(scopeId);
    });
}

export async function saveConsoleDocGrant(grant: ConsoleDocGrant): Promise<void> {
    await runTransaction(CONSOLE_DOC_GRANTS_STORE_NAME, 'readwrite', store => store.put(grant));
}

export async function createConsoleDocGrant(input: ConsoleDocGrantInput): Promise<ConsoleDocGrant> {
    const now = new Date().toISOString();
    const grant: ConsoleDocGrant = { id: createConsoleGrantId(), ...input, createdAt: now, updatedAt: now };
    await saveConsoleDocGrant(grant);
    return grant;
}

export async function setConsoleDocGrantRole(
    id: string,
    role: ConsoleDocRole,
): Promise<ConsoleDocGrant | undefined> {
    const existing = await runTransaction<ConsoleDocGrant | undefined>(
        CONSOLE_DOC_GRANTS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    if (!existing) {
        return undefined;
    }

    const updated: ConsoleDocGrant = { ...existing, role, updatedAt: new Date().toISOString() };
    await saveConsoleDocGrant(updated);
    return updated;
}

export async function deleteConsoleDocGrant(id: string): Promise<void> {
    await runTransaction(CONSOLE_DOC_GRANTS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function seedConsoleDocGrantsIfEmpty(): Promise<void> {
    const existing = await getAllConsoleDocGrants();
    if (existing.length > 0) {
        return;
    }

    await Promise.all(SEED_GRANTS.map(grant => saveConsoleDocGrant(grant)));
}
