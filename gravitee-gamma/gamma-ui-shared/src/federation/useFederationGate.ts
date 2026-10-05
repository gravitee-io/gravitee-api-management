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
import { useEffect, useState, useSyncExternalStore } from 'react';

import type { LicenseWaitStartStore } from './licenseWaitStartStore';

const LICENSE_REPORT_TIMEOUT_MS = 10_000;

export type FederationSetting = 'pending' | 'on' | 'off';

export interface FederationGate {
    enabled: boolean;
    /** False while the org settings read is in flight, and while the setting is on but the host has not reported a license yet (for up to LICENSE_REPORT_TIMEOUT_MS after the first caller sees the setting on); a failed read or an overdue license resolves to `enabled: false`. */
    isResolved: boolean;
}

/**
 * Any non-expired, non-OSS tier is entitled to federation (APIM-4273); no license ever carries a
 * `federation` entry in `features`, so a feature-id check would read as unlicensed everywhere. Classic
 * console's IntegrationsComponent and the backend's LicenseDomainService gate on the tier the same way.
 */
function isFederationLicensed(license: License | null): boolean {
    if (license === null) {
        return false;
    }

    if (license.isExpired) {
        return false;
    }

    if (license.tier === 'oss') {
        return false;
    }

    return true;
}

function claimLicenseWaitStart(store: LicenseWaitStartStore): number {
    const existingStart = store.read();
    if (existingStart !== undefined) {
        return existingStart;
    }
    const startedAt = Date.now();
    store.write(startedAt);
    return startedAt;
}

function remainingLicenseWaitMs(startedAt: number): number {
    return startedAt + LICENSE_REPORT_TIMEOUT_MS - Date.now();
}

function useLicenseReportDeadlinePassed(isWaitingForLicense: boolean, store: LicenseWaitStartStore): boolean {
    const [deadlinePassed, setDeadlinePassed] = useState(() => {
        const startedAt = store.read();
        return isWaitingForLicense && startedAt !== undefined && remainingLicenseWaitMs(startedAt) <= 0;
    });
    useEffect(() => {
        if (!isWaitingForLicense) {
            store.clear();
            setDeadlinePassed(false);
            return;
        }
        const remainingMs = remainingLicenseWaitMs(claimLicenseWaitStart(store));
        if (remainingMs <= 0) {
            setDeadlinePassed(true);
            return;
        }
        const timer = setTimeout(() => setDeadlinePassed(true), remainingMs);
        return () => clearTimeout(timer);
    }, [isWaitingForLicense, store]);
    return deadlinePassed;
}

/**
 * Federation needs both the organization setting and an entitled license tier — either one alone is off.
 * A null license snapshot means "not reported yet", not "not entitled", so the gate waits for it when the
 * setting is on, for a fixed window that starts once the setting reads on, so a license that never arrives
 * cannot hold callers that wait on `isResolved` forever. The license wait is shared by every caller on the
 * same store: it starts when the first caller sees the setting on without a license, and a caller that mounts
 * later waits only for what remains of that window (or resolves immediately if it has passed). A license
 * reported after the deadline turns `enabled` back on, so callers keying queries on it refetch with federated
 * APIs included. The store must keep the same identity across renders.
 */
export function useFederationGate(setting: FederationSetting, licenseWaitStartStore: LicenseWaitStartStore): FederationGate {
    // The host can push a license after first render, so the gate re-reads it from the store rather than once.
    const license = useSyncExternalStore(
        listener => licenseService.subscribe(listener),
        () => licenseService.getSnapshot(),
        () => licenseService.getSnapshot(),
    );
    const hasFederationLicense = isFederationLicensed(license);

    const isFederationSettingOn = setting === 'on';
    const isLicenseUnreported = license === null;
    const isLicenseRequiredButUnreported = isFederationSettingOn && isLicenseUnreported;
    const licenseDeadlinePassed = useLicenseReportDeadlinePassed(isLicenseRequiredButUnreported, licenseWaitStartStore);
    const isAwaitingLicense = isLicenseRequiredButUnreported && !licenseDeadlinePassed;
    const isLicenseReportOverdue = isLicenseRequiredButUnreported && licenseDeadlinePassed;

    const isLicenseExpired = license?.isExpired === true;
    useEffect(() => {
        if (isFederationSettingOn && isLicenseExpired) {
            console.warn('[Federation] federation.enabled is on but the license has expired, treating federation as disabled');
        }
    }, [isFederationSettingOn, isLicenseExpired]);

    useEffect(() => {
        if (isLicenseReportOverdue) {
            console.warn(
                `[Federation] federation.enabled is on but the host has not reported a license after ${LICENSE_REPORT_TIMEOUT_MS} ms, treating federation as disabled`,
            );
        }
    }, [isLicenseReportOverdue]);

    return {
        enabled: isFederationSettingOn && hasFederationLicense,
        isResolved: setting !== 'pending' && !isAwaitingLicense,
    };
}
