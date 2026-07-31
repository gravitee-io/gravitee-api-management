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
import type { ILicenseService, License } from '@gravitee/gamma-modules-sdk/types';

type Listener = () => void;

/**
 * Real license store shared across Gamma host and federated modules.
 * Populated by the host via startLicenseSync(); remotes consume via useHasFeature() / useHasPack().
 */
export class LicenseService implements ILicenseService {
    private current: License | null = null;
    private readonly listeners = new Set<Listener>();

    setLicense(license: License | null): void {
        this.current = license;
        this.notify();
    }

    getLicense(): License | null {
        return this.current;
    }

    hasFeature(feature: string): boolean {
        return this.current?.features?.includes(feature) ?? false;
    }

    hasPack(pack: string): boolean {
        return this.current?.packs?.includes(pack) ?? false;
    }

    isExpired(): boolean {
        return this.current?.isExpired ?? false;
    }

    subscribe(listener: Listener): () => void {
        this.listeners.add(listener);
        return () => this.listeners.delete(listener);
    }

    getSnapshot(): License | null {
        return this.current;
    }

    private notify(): void {
        this.listeners.forEach(l => l());
    }
}

export const licenseService = new LicenseService();
