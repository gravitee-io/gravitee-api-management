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
import type { IEnvironmentService, Environment } from '@gravitee/gamma-modules-sdk/types';

type Listener = () => void;

/**
 * Real environment store shared across Gamma host and federated modules.
 * Populated by the host via startEnvironmentSync(); remotes consume via useEnvironment().
 */
export class EnvironmentService implements IEnvironmentService {
    private current: Environment | null = null;
    private readonly listeners = new Set<Listener>();

    setEnvironment(env: Environment | null): void {
        this.current = env;
        this.notify();
    }

    getEnvironment(): Environment | null {
        return this.current;
    }

    getEnvironmentId(): string {
        return this.current?.id ?? '';
    }

    getEnvHrid(): string {
        return this.current?.hrids?.[0] ?? this.current?.id ?? '';
    }

    subscribe(listener: Listener): () => void {
        this.listeners.add(listener);
        return () => this.listeners.delete(listener);
    }

    getSnapshot(): Environment | null {
        return this.current;
    }

    private notify(): void {
        this.listeners.forEach(l => l());
    }
}

export const environmentService = new EnvironmentService();
