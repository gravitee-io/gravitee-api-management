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
import {
    useFederationGate,
    type FederationGate,
    type FederationSetting,
    type LicenseWaitStartStore,
} from '@gravitee/gamma-ui-shared/federation';
import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';

import { ApimApiError } from '../../shared/api/apimClient';
import { orgConsoleKeys } from '../apis/utils/queryKeys';
import { fetchOrgConsoleSettings } from '../settings/services/orgConsoleSettings';

const ORG_CONSOLE_SETTINGS_TIMEOUT_MS = 10_000;

export type { FederationGate };

function queryClientLicenseWaitStartStore(queryClient: QueryClient): LicenseWaitStartStore {
    return {
        read: () => queryClient.getQueryData<number>(orgConsoleKeys.licenseReportWaitStart()),
        write: startedAt => {
            queryClient.setQueryData(orgConsoleKeys.licenseReportWaitStart(), startedAt);
        },
        clear: () => {
            queryClient.removeQueries({ queryKey: orgConsoleKeys.licenseReportWaitStart(), exact: true });
        },
    };
}

function toFederationSetting(isPending: boolean, federationEnabled: boolean | undefined): FederationSetting {
    if (isPending) {
        return 'pending';
    }
    return federationEnabled === true ? 'on' : 'off';
}

/**
 * Federation needs both the organization setting and an entitled license tier — either one alone is off.
 * The settings read fails closed so an unreachable settings endpoint narrows the feature rather than
 * blocking the callers that wait on `isResolved`. It is time-bounded so an endpoint that hangs instead
 * of answering takes that same path rather than parking those callers forever. The license wait window
 * lives in the QueryClient, so every caller on the same QueryClient shares it.
 */
export function useFederationEnabled(): FederationGate {
    const queryClient = useQueryClient();
    const licenseWaitStartStore = useMemo(() => queryClientLicenseWaitStartStore(queryClient), [queryClient]);

    const settingsQuery = useQuery({
        queryKey: orgConsoleKeys.settings(),
        queryFn: () => fetchOrgConsoleSettings(AbortSignal.timeout(ORG_CONSOLE_SETTINGS_TIMEOUT_MS)),
        staleTime: 60_000,
        // Retrying would hold isResolved false for the whole backoff window, stalling every caller
        // that waits on this gate. One attempt, then fail closed.
        retry: false,
    });
    const setting = toFederationSetting(settingsQuery.isPending, settingsQuery.data?.federation?.enabled);

    const settingsError = settingsQuery.error;
    useEffect(() => {
        if (settingsError) {
            const failure = settingsError instanceof ApimApiError ? `HTTP ${settingsError.status}` : settingsError.name;
            console.warn(`[Federation] Org console settings read failed (${failure}), treating federation as disabled:`, settingsError);
        }
    }, [settingsError]);

    return useFederationGate(setting, licenseWaitStartStore);
}
