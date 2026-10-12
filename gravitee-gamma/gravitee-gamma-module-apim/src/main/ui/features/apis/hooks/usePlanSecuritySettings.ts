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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';

import { useEnvironmentPortalConfiguration } from './useEnvironmentPortalConfiguration';
import { getEnvironmentPortalSettings } from '../../settings/services/portalSettings';
import type { EnvironmentPortalSettings } from '../../settings/services/portalSettings';
import type { PlanSecurityType } from '../types/plan';
import { portalSettingsKeys } from '../utils/queryKeys';

export type PlanSecuritySettingsMap = Record<PlanSecurityType, boolean>;

/**
 * Maps GET /settings `plan.security` (JSON keys from PlanSettings.PlanSecurity:
 * apikey, jwt, oauth2, mtls, keyless) to console PlanSecurityType flags.
 *
 * Absent keys default to enabled — same as parameter defaults
 * (`plan.security.*.enabled` default "true"). Fail-open when /settings is missing
 * or errors is intentional: Management API rejects disabled types via
 * UnauthorizedPlanSecurityTypeException (PlanValidatorDomainService).
 */
export function toPlanSecuritySettings(data: EnvironmentPortalSettings | undefined): PlanSecuritySettingsMap {
    const security = data?.plan?.security ?? {};
    const enabled = (setting: { enabled?: boolean } | undefined) => setting?.enabled ?? true;

    return {
        API_KEY: enabled(security.apikey),
        JWT: enabled(security.jwt),
        OAUTH2: enabled(security.oauth2),
        MTLS: enabled(security.mtls),
        KEY_LESS: enabled(security.keyless),
    };
}

/** Classic `Constants.env.settings.plan.security` — which plan security types are enabled. */
export function usePlanSecuritySettings(): {
    planSecuritySettings: PlanSecuritySettingsMap;
    isLoading: boolean;
} {
    const env = useEnvironment();
    const { data, isPending } = useQuery({
        queryKey: portalSettingsKeys.env(env?.id ?? ''),
        queryFn: () => getEnvironmentPortalSettings(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 5 * 60_000,
    });

    return {
        planSecuritySettings: toPlanSecuritySettings(data),
        // Treat "no env yet" / disabled query as loading so create routes do not
        // briefly treat every type as enabled before the request starts.
        isLoading: !env?.id || isPending,
    };
}

export function useCustomApiKeyEnabled(): boolean {
    const { data } = useEnvironmentPortalConfiguration();
    return data?.plan?.security?.customApiKey?.enabled ?? false;
}

export function useSharedApiKeyEnabled(): boolean {
    const { data } = useEnvironmentPortalConfiguration();
    return data?.plan?.security?.sharedApiKey?.enabled ?? false;
}
