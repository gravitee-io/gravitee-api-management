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
import { createTenantMemberId } from '../../tenants/utils/tenant-hrid';
import { getMembersByTenantId, savePortalTenantMember } from '../../tenants/storage/portal-tenant-members.storage';
import { getTenantsByPortalId } from '../../tenants/storage/portal-tenants.storage';
import { createConsumerId } from '../utils/consumer-auth-ids';
import type { PortalConsumer } from '../types/consumer-auth.types';
import { getConsumerByPortalAndEmail, savePortalConsumer } from './portal-consumers.storage';

export const DEMO_CONSUMER_EMAIL = 'user@demo.local';
export const DEMO_CONSUMER_USERNAME = 'user';
export const DEMO_CONSUMER_PASSWORD = 'user';

const seedInFlightByPortalId = new Map<string, Promise<void>>();

async function seedDemoConsumerForPortalInternal(portalId: string): Promise<void> {
    const tenants = await getTenantsByPortalId(portalId);
    if (tenants.length === 0) {
        return;
    }

    const existing = await getConsumerByPortalAndEmail(portalId, DEMO_CONSUMER_EMAIL);
    if (existing) {
        return;
    }

    const defaultTenant = tenants[0];
    const consumer: PortalConsumer = {
        id: createConsumerId(),
        portalId,
        tenantId: defaultTenant.id,
        email: DEMO_CONSUMER_EMAIL,
        username: DEMO_CONSUMER_USERNAME,
        firstName: 'Demo',
        lastName: 'User',
        company: 'Acme Corp',
        password: DEMO_CONSUMER_PASSWORD,
        authProvider: 'local',
        createdAt: new Date().toISOString(),
    };

    try {
        await savePortalConsumer(consumer);
    } catch (error) {
        // React StrictMode can run seed effects concurrently; treat duplicate as success.
        if (error instanceof DOMException && error.name === 'ConstraintError') {
            return;
        }
        throw error;
    }

    const members = await getMembersByTenantId(defaultTenant.id);
    const alreadyMember = members.some(member => member.userId === consumer.id);
    if (!alreadyMember) {
        await savePortalTenantMember({
            id: createTenantMemberId(),
            tenantId: defaultTenant.id,
            userId: consumer.id,
            displayName: `${consumer.firstName} ${consumer.lastName}`,
            email: consumer.email,
            role: 'member',
        });
    }
}

export async function seedDemoConsumerForPortal(portalId: string): Promise<void> {
    const inFlight = seedInFlightByPortalId.get(portalId);
    if (inFlight) {
        return inFlight;
    }

    const promise = seedDemoConsumerForPortalInternal(portalId).finally(() => {
        seedInFlightByPortalId.delete(portalId);
    });
    seedInFlightByPortalId.set(portalId, promise);
    return promise;
}

/** Test helper — reset in-flight seed dedupe between specs. */
export function resetDemoConsumerSeedState(): void {
    seedInFlightByPortalId.clear();
}
