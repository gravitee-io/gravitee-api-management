/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { licenseService } from '@gravitee/gamma-modules-sdk';
import type { License } from '@gravitee/gamma-modules-sdk/types';
import { useEffect, useSyncExternalStore } from 'react';

export type FederationSetting = 'pending' | 'on' | 'off';

export interface FederationGate {
    enabled: boolean;
    /** False while the org settings read is in flight; a failed read resolves to `enabled: false`. */
    isResolved: boolean;
}

/**
 * Federation needs both the organization setting and an entitled license tier — either one alone is off.
 * Any non-OSS tier is entitled (APIM-4273); no license ever carries a `federation` entry in `features`, so a
 * feature-id check would read as unlicensed everywhere. Classic console's IntegrationsComponent and the
 * backend's LicenseDomainService gate on the tier the same way.
 *
 * A null license means the host has not reported one yet, not that none is installed — an installation
 * without one reports `tier: 'oss'`. Granting on null would turn a license fetch that has not landed, or
 * failed, into an entitlement.
 *
 * Expiry is a separate term because the backend's tier does not carry it: an expired enterprise
 * license still reports `tier: 'enterprise'` (LicenseDomainService.isFederationFeatureAllowed).
 */
export function isFederationEntitled({
    federationEnabled,
    license,
}: Readonly<{ federationEnabled: boolean; license: License | null }>): boolean {
    return federationEnabled && license !== null && !license.isExpired && license.tier !== 'oss';
}

/**
 * Like every other Gamma license check, an unreported (null) license denies at once rather than waiting for
 * one. A license the host reports later turns `enabled` back on, so callers keying queries on it refetch with
 * federated APIs included.
 */
export function useFederationGate(setting: FederationSetting): FederationGate {
    // The host can push a license after first render, so the gate re-reads it from the store rather than once.
    const license = useSyncExternalStore(
        listener => licenseService.subscribe(listener),
        () => licenseService.getSnapshot(),
        () => licenseService.getSnapshot(),
    );

    const isFederationSettingOn = setting === 'on';
    const isLicenseExpired = license?.isExpired === true;
    useEffect(() => {
        if (isFederationSettingOn && isLicenseExpired) {
            console.warn('[Federation] federation.enabled is on but the license has expired, treating federation as disabled');
        }
    }, [isFederationSettingOn, isLicenseExpired]);

    return {
        enabled: isFederationEntitled({ federationEnabled: isFederationSettingOn, license }),
        isResolved: setting !== 'pending',
    };
}
