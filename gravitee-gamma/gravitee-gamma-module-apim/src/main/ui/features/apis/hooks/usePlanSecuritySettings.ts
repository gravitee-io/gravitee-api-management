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

import { getEnvironmentPortalSettings } from '../../settings/services/portalSettings';
import type { PlanSecurityType } from '../types/plan';
import { portalSettingsKeys } from '../utils/queryKeys';

/** Classic `Constants.env.settings.plan.security` — which plan security types are enabled. */
export function usePlanSecuritySettings(): Record<PlanSecurityType, boolean> {
    const env = useEnvironment();
    const { data } = useQuery({
        queryKey: portalSettingsKeys.env(env?.id ?? ''),
        queryFn: () => getEnvironmentPortalSettings(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 5 * 60_000,
    });
    const security = data?.plan?.security ?? {};

    // Classic gating: a plan type is offered only when its portal setting is enabled.
    // When the setting is absent, default to enabled (same as classic default behaviour).
    const enabled = (setting: { enabled?: boolean } | undefined) => setting?.enabled ?? true;

    return {
        API_KEY: enabled(security.apikey),
        JWT: enabled(security.jwt),
        OAUTH2: enabled(security.oauth2),
        MTLS: enabled(security.mtls),
        KEY_LESS: enabled(security.keyless),
    };
}
