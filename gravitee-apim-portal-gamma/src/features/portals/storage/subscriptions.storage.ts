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
import type { Subscription } from '../../editor/entities/subscription';
import { runTransaction, SUBSCRIPTIONS_STORE_NAME } from './db';

export const STORE_NAME = SUBSCRIPTIONS_STORE_NAME;

export async function getAllSubscriptions(): Promise<Subscription[]> {
    return runTransaction<Subscription[]>(SUBSCRIPTIONS_STORE_NAME, 'readonly', store => store.getAll());
}

export async function getSubscription(id: string): Promise<Subscription | undefined> {
    return runTransaction<Subscription | undefined>(SUBSCRIPTIONS_STORE_NAME, 'readonly', store => store.get(id));
}

export async function saveSubscription(subscription: Subscription): Promise<void> {
    await runTransaction(SUBSCRIPTIONS_STORE_NAME, 'readwrite', store => store.put(subscription));
}

export async function deleteSubscription(id: string): Promise<void> {
    await runTransaction(SUBSCRIPTIONS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function saveSubscriptions(subscriptions: readonly Subscription[]): Promise<void> {
    await Promise.all(subscriptions.map(subscription => saveSubscription(subscription)));
}

export async function getSubscriptionsByApi(apiId: string): Promise<Subscription[]> {
    return runTransaction<Subscription[]>(SUBSCRIPTIONS_STORE_NAME, 'readonly', store =>
        store.index('api').getAll(apiId),
    );
}
