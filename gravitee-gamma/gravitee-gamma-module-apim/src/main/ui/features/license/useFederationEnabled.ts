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
import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useEffect, useState, useSyncExternalStore } from 'react';

import { ApimApiError } from '../../shared/api/apimClient';
import { orgConsoleKeys } from '../apis/utils/queryKeys';
import { fetchOrgConsoleSettings } from '../settings/services/orgConsoleSettings';

const ORG_CONSOLE_SETTINGS_TIMEOUT_MS = 10_000;
const LICENSE_REPORT_TIMEOUT_MS = 10_000;

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

function readLicenseWaitStart(queryClient: QueryClient): number | undefined {
    return queryClient.getQueryData<number>(orgConsoleKeys.licenseReportWaitStart());
}

function claimLicenseWaitStart(queryClient: QueryClient): number {
    const existingStart = readLicenseWaitStart(queryClient);
    if (existingStart !== undefined) {
        return existingStart;
    }
    const startedAt = Date.now();
    queryClient.setQueryData(orgConsoleKeys.licenseReportWaitStart(), startedAt);
    return startedAt;
}

function remainingLicenseWaitMs(startedAt: number): number {
    return startedAt + LICENSE_REPORT_TIMEOUT_MS - Date.now();
}

function useLicenseReportDeadlinePassed(isWaitingForLicense: boolean): boolean {
    const queryClient = useQueryClient();
    const [deadlinePassed, setDeadlinePassed] = useState(() => {
        const startedAt = readLicenseWaitStart(queryClient);
        return isWaitingForLicense && startedAt !== undefined && remainingLicenseWaitMs(startedAt) <= 0;
    });
    useEffect(() => {
        if (!isWaitingForLicense) {
            queryClient.removeQueries({ queryKey: orgConsoleKeys.licenseReportWaitStart(), exact: true });
            setDeadlinePassed(false);
            return;
        }
        const remainingMs = remainingLicenseWaitMs(claimLicenseWaitStart(queryClient));
        if (remainingMs <= 0) {
            setDeadlinePassed(true);
            return;
        }
        const timer = setTimeout(() => setDeadlinePassed(true), remainingMs);
        return () => clearTimeout(timer);
    }, [isWaitingForLicense, queryClient]);
    return deadlinePassed;
}

/**
 * Federation needs both the organization setting and an entitled license tier — either one alone is off.
 * The settings read fails closed so an unreachable settings endpoint narrows the feature rather than
 * blocking the callers that wait on `isResolved`. It is time-bounded so an endpoint that hangs instead
 * of answering takes that same path rather than parking those callers forever. A null license snapshot
 * means "not reported yet", not "not entitled", so the gate waits for it when the setting is on, for a
 * fixed window that starts once the setting reads on, time-bounded for the same reason the settings read is.
 * The license wait is shared by every caller on the same QueryClient: it starts when the first caller sees the
 * setting on without a license, and a caller that mounts later waits only for what remains of that window (or
 * resolves immediately if it has passed). A license reported after the deadline turns `enabled` back on, so
 * callers keying queries on it refetch with federated APIs included.
 */
export function useFederationEnabled(): FederationGate {
    // The host can push a license after first render, so the gate re-reads it from the store rather than once.
    const license = useSyncExternalStore(
        listener => licenseService.subscribe(listener),
        () => licenseService.getSnapshot(),
        () => licenseService.getSnapshot(),
    );
    const hasFederationLicense = isFederationLicensed(license);

    const settingsQuery = useQuery({
        queryKey: orgConsoleKeys.settings(),
        queryFn: () => fetchOrgConsoleSettings(AbortSignal.timeout(ORG_CONSOLE_SETTINGS_TIMEOUT_MS)),
        staleTime: 60_000,
        // Retrying would hold isResolved false for the whole backoff window, stalling every caller
        // that waits on this gate. One attempt, then fail closed.
        retry: false,
    });
    const isFederationSettingOn = settingsQuery.data?.federation?.enabled === true;
    const isLicenseUnreported = license === null;
    const isLicenseRequiredButUnreported = isFederationSettingOn && isLicenseUnreported;
    const licenseDeadlinePassed = useLicenseReportDeadlinePassed(isLicenseRequiredButUnreported);
    const isAwaitingLicense = isLicenseRequiredButUnreported && !licenseDeadlinePassed;
    const isLicenseReportOverdue = isLicenseRequiredButUnreported && licenseDeadlinePassed;

    const settingsError = settingsQuery.error;
    useEffect(() => {
        if (settingsError) {
            const failure = settingsError instanceof ApimApiError ? `HTTP ${settingsError.status}` : settingsError.name;
            console.warn(`[Federation] Org console settings read failed (${failure}), treating federation as disabled:`, settingsError);
        }
    }, [settingsError]);

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
        isResolved: !settingsQuery.isPending && !isAwaitingLicense,
    };
}
