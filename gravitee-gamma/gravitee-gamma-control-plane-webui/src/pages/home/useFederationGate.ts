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
import { useEffect, useState } from 'react';

import {
    useFederationGate as useSharedFederationGate,
    type FederationGate,
    type FederationSetting,
} from '@gravitee/gamma-ui-shared/federation';

import { ApiError, managementApi } from '../../shared/api/api-client';

const CONSOLE_SETTINGS_TIMEOUT_MS = 10_000;

function describeFailure(error: unknown): string {
    if (error instanceof ApiError) return `HTTP ${error.status}`;
    if (error instanceof Error) return error.name;
    return String(error);
}

function useFederationSetting(): FederationSetting {
    const [setting, setSetting] = useState<FederationSetting>('pending');

    useEffect(() => {
        let cancelled = false;
        managementApi
            .get<{ federation?: { enabled?: boolean } }>('/console', { signal: AbortSignal.timeout(CONSOLE_SETTINGS_TIMEOUT_MS) })
            .then(settings => {
                if (!cancelled) setSetting(settings?.federation?.enabled === true ? 'on' : 'off');
            })
            .catch((error: unknown) => {
                console.warn(
                    `[Federation] Org console settings read failed (${describeFailure(error)}), treating federation as disabled:`,
                    error,
                );
                if (!cancelled) setSetting('off');
            });
        return () => {
            cancelled = true;
        };
    }, []);

    return setting;
}

/**
 * Host-side settings read for the federation gate the APIM module also uses, so the home page API count
 * asks for the same API types as the API Proxies list. A failed or timed-out settings read fails closed.
 */
export function useFederationGate(): FederationGate {
    return useSharedFederationGate(useFederationSetting());
}
