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
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

import { useEnvironmentStore } from '../../features/environment/environment.store';
import type { Environment } from '../../features/environment/environment.types';
import type { CloudProduct } from './cloud.config';

const CLOUD_ENVIRONMENTS_STORAGE_KEY = 'gamma.cloud.created-environments';

interface StoredCloudEnvironment {
    readonly environment: Environment;
    readonly product: CloudProduct;
}

function createCloudEnvironmentId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `cloud-env-${crypto.randomUUID()}`;
    }
    return `cloud-env-${Date.now()}`;
}

function readStoredCloudEnvironments(): StoredCloudEnvironment[] {
    if (typeof window === 'undefined') {
        return [];
    }

    try {
        const raw = window.sessionStorage.getItem(CLOUD_ENVIRONMENTS_STORAGE_KEY);
        if (!raw) {
            return [];
        }

        const parsed = JSON.parse(raw) as unknown;
        if (!Array.isArray(parsed)) {
            return [];
        }

        return parsed.filter(
            (entry): entry is StoredCloudEnvironment =>
                typeof entry === 'object' &&
                entry !== null &&
                typeof (entry as StoredCloudEnvironment).product === 'string' &&
                typeof (entry as StoredCloudEnvironment).environment === 'object' &&
                (entry as StoredCloudEnvironment).environment !== null,
        );
    } catch {
        return [];
    }
}

function writeStoredCloudEnvironments(entries: readonly StoredCloudEnvironment[]): void {
    if (typeof window === 'undefined') {
        return;
    }

    try {
        window.sessionStorage.setItem(CLOUD_ENVIRONMENTS_STORAGE_KEY, JSON.stringify(entries));
    } catch {
        // Ignore quota or privacy mode errors in local dev.
    }
}

function clearStoredCloudEnvironments(): void {
    if (typeof window === 'undefined') {
        return;
    }

    try {
        window.sessionStorage.removeItem(CLOUD_ENVIRONMENTS_STORAGE_KEY);
    } catch {
        // Ignore storage errors.
    }
}

interface CloudEnvironmentMetaState {
    productByEnvironmentId: Record<string, CloudProduct>;
    registerProduct: (environmentId: string, product: CloudProduct) => void;
    reset: () => void;
}

export const useCloudEnvironmentStore = create<CloudEnvironmentMetaState>()(
    devtools(
        set => ({
            productByEnvironmentId: {},
            registerProduct: (environmentId, product) =>
                set(state => ({
                    productByEnvironmentId: { ...state.productByEnvironmentId, [environmentId]: product },
                })),
            reset: () => {
                clearStoredCloudEnvironments();
                set({ productByEnvironmentId: {} });
            },
        }),
        { name: 'cloud-environments' },
    ),
);

export function isCloudEnvironmentId(environmentId: string, productByEnvironmentId: Record<string, CloudProduct>): boolean {
    return environmentId in productByEnvironmentId || environmentId.startsWith('cloud-env-');
}

/** Removes cloud-created environments and keeps only the default (first) environment. */
export function retainDefaultEnvironmentOnly(): void {
    const environmentStore = useEnvironmentStore.getState();
    const defaultEnvironment = environmentStore.environments[0];

    clearStoredCloudEnvironments();
    useCloudEnvironmentStore.setState({ productByEnvironmentId: {} });

    if (!defaultEnvironment) {
        return;
    }

    useEnvironmentStore.setState({
        environments: [defaultEnvironment],
        currentEnvironment: defaultEnvironment,
        environmentId: defaultEnvironment.id,
        organizationId: defaultEnvironment.organizationId || environmentStore.organizationId,
    });
}

/** Re-applies cloud environments created earlier in this browser session. */
export function rehydrateCloudEnvironments(): void {
    const stored = readStoredCloudEnvironments();
    if (stored.length === 0) {
        return;
    }

    const environmentStore = useEnvironmentStore.getState();
    const cloudStore = useCloudEnvironmentStore.getState();

    for (const entry of stored) {
        environmentStore.addEnvironment(entry.environment);
        cloudStore.registerProduct(entry.environment.id, entry.product);
    }
}

export function addCloudEnvironment(payload: { name: string; hrid: string; product: CloudProduct }): Environment | null {
    const organizationId = useEnvironmentStore.getState().organizationId;
    const environment: Environment = {
        id: createCloudEnvironmentId(),
        name: payload.name,
        organizationId: organizationId || 'org-1',
        hrids: [payload.hrid],
    };

    const beforeCount = useEnvironmentStore.getState().environments.length;
    useEnvironmentStore.getState().addEnvironment(environment);
    const afterCount = useEnvironmentStore.getState().environments.length;

    if (afterCount === beforeCount) {
        return null;
    }

    useCloudEnvironmentStore.getState().registerProduct(environment.id, payload.product);

    const stored = readStoredCloudEnvironments();
    writeStoredCloudEnvironments([...stored, { environment, product: payload.product }]);

    return environment;
}
