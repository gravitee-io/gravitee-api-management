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

import { useQuery } from '@tanstack/react-query';

import { listOrgEnvironments } from '../services/environments';
import { getPortalSettingsByEnvironmentId } from '../services/portalSettings';
import type { EnvironmentEntrypointConfig } from '../types/entrypoint';
import { orgEnvironmentKeys, portalSettingsKeys } from '../utils/queryKeys';

export interface EntrypointConfigurationsResult {
    configs: EnvironmentEntrypointConfig[];
    failedEnvironmentNames: string[];
}

export function useEntrypointConfigurations() {
    return useQuery({
        queryKey: [...orgEnvironmentKeys.list(), ...portalSettingsKeys.all, 'entrypoint-configurations'],
        queryFn: async (): Promise<EntrypointConfigurationsResult> => {
            const environments = await listOrgEnvironments();
            const settled = await Promise.allSettled(
                environments.map(async environment => {
                    const portalSettings = await getPortalSettingsByEnvironmentId(environment.id);
                    return { environment, portalSettings } satisfies EnvironmentEntrypointConfig;
                }),
            );

            const configs: EnvironmentEntrypointConfig[] = [];
            const failedEnvironmentNames: string[] = [];

            settled.forEach((result, index) => {
                const environment = environments[index]!;
                if (result.status === 'fulfilled') {
                    configs.push(result.value);
                } else {
                    failedEnvironmentNames.push(environment.name || environment.id);
                }
            });

            return { configs, failedEnvironmentNames };
        },
    });
}
