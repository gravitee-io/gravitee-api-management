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
import { useQuery } from '@tanstack/react-query';
import { useEffect, useSyncExternalStore } from 'react';

import { ApimApiError } from '../../shared/api/apimClient';
import { orgConsoleKeys } from '../apis/utils/queryKeys';
import { fetchOrgConsoleSettings } from '../settings/services/orgConsoleSettings';

const ORG_CONSOLE_SETTINGS_TIMEOUT_MS = 10_000;

export interface FederationGate {
    enabled: boolean;
    /** False only while the org settings read is in flight; a failed read resolves to `enabled: false`. */
    isResolved: boolean;
}

/**
 * Any non-expired, non-OSS tier is entitled to federation (APIM-4273); no license ever carries a
 * `federation` entry in `features`, so a feature-id check would read as unlicensed everywhere. Classic
 * console's IntegrationsComponent and the backend's LicenseDomainService gate on the tier the same way.
 */
function isFederationLicensed(license: License | null): boolean {
    return license !== null && !license.isExpired && license.tier !== 'oss';
}

/**
 * Federation needs both the organization setting and an entitled license tier — either one alone is off.
 * The settings read fails closed so an unreachable settings endpoint narrows the feature rather than
 * blocking the callers that wait on `isResolved`. It is time-bounded so an endpoint that hangs instead
 * of answering takes that same path rather than parking those callers forever.
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

    const settingsError = settingsQuery.error;
    useEffect(() => {
        if (settingsError) {
            const failure = settingsError instanceof ApimApiError ? `HTTP ${settingsError.status}` : settingsError.name;
            console.warn(`[Federation] Org console settings read failed (${failure}), treating federation as disabled:`, settingsError);
        }
    }, [settingsError]);

    return {
        enabled: Boolean(settingsQuery.data?.federation?.enabled) && hasFederationLicense,
        isResolved: !settingsQuery.isPending,
    };
}
