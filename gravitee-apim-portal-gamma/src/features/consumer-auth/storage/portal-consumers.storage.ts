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
import { PORTAL_CONSUMERS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import type { PortalConsumer } from '../types/consumer-auth.types';

export const STORE_NAME = PORTAL_CONSUMERS_STORE_NAME;

export async function getConsumerById(id: string): Promise<PortalConsumer | undefined> {
    return runTransaction<PortalConsumer | undefined>(PORTAL_CONSUMERS_STORE_NAME, 'readonly', store => store.get(id));
}

export async function getConsumersByPortalId(portalId: string): Promise<PortalConsumer[]> {
    return runTransaction<PortalConsumer[]>(PORTAL_CONSUMERS_STORE_NAME, 'readonly', store => {
        const index = store.index('portalId');
        return index.getAll(portalId);
    });
}

export async function getConsumerByPortalAndEmail(
    portalId: string,
    email: string,
): Promise<PortalConsumer | undefined> {
    return runTransaction<PortalConsumer | undefined>(PORTAL_CONSUMERS_STORE_NAME, 'readonly', store => {
        const index = store.index('portalId_email');
        return index.get([portalId, email.trim().toLowerCase()]);
    });
}

export async function findConsumerByCredential(
    portalId: string,
    identifier: string,
    password: string,
): Promise<PortalConsumer | undefined> {
    const normalized = identifier.trim().toLowerCase();
    const consumers = await getConsumersByPortalId(portalId);

    return consumers.find(consumer => {
        const matchesIdentity =
            consumer.email.toLowerCase() === normalized
            || consumer.username?.toLowerCase() === normalized;
        return matchesIdentity && consumer.password === password;
    });
}

function normalizeConsumerEmail(email: string): string {
    return email.trim().toLowerCase();
}

export async function savePortalConsumer(consumer: PortalConsumer): Promise<void> {
    const normalized: PortalConsumer = {
        ...consumer,
        email: normalizeConsumerEmail(consumer.email),
    };
    await runTransaction(PORTAL_CONSUMERS_STORE_NAME, 'readwrite', store => store.put(normalized));
}
