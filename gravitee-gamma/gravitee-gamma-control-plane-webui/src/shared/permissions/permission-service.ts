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
import type { PermissionScope } from './types';

type Listener = () => void;

/**
 * Vanilla permission store shared across Gamma host and federated modules.
 * Not tied to Zustand/Redux; consumers can subscribe for custom integrations.
 */
export class PermissionService {
    private readonly scopes = new Map<PermissionScope, string[]>();
    private version = 0;
    private readonly listeners = new Set<Listener>();

    load(scope: PermissionScope, permissions: string[]): void {
        this.scopes.set(scope, permissions);
        this.bump();
    }

    clear(scope: PermissionScope): void {
        this.scopes.delete(scope);
        this.bump();
    }

    reset(): void {
        this.scopes.clear();
        this.bump();
    }

    /** All normalized permission strings from every loaded scope (flattened). */
    getAllPermissions(): string[] {
        return [...this.scopes.values()].flat();
    }

    hasAnyOf(required: string[] | undefined): boolean {
        if (!required?.length) {
            return false;
        }
        const all = this.getAllPermissions();
        return required.some(p => all.includes(p));
    }

    hasAllOf(required: string[] | undefined): boolean {
        if (!required?.length) {
            return false;
        }
        const all = new Set(this.getAllPermissions());
        return required.every(p => all.has(p));
    }

    subscribe(listener: Listener): () => void {
        this.listeners.add(listener);
        return () => this.listeners.delete(listener);
    }

    /** Version token for useSyncExternalStore. */
    getSnapshot(): number {
        return this.version;
    }

    private bump(): void {
        this.version++;
        this.listeners.forEach(l => l());
    }
}

export const permissionService = new PermissionService();
