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

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notify } from '../../../shared/notify';
import { savePortalSettingsByEnvironmentId } from '../services/portalSettings';
import type { EntrypointPortalSettings } from '../types/entrypoint';
import { orgEnvironmentKeys, portalSettingsKeys } from '../utils/queryKeys';

export interface SaveEntrypointConfigurationInput {
    environmentId: string;
    settings: EntrypointPortalSettings;
}

export interface SaveEntrypointConfigurationsResult {
    succeededEnvironmentIds: string[];
    failed: { environmentId: string; error: unknown }[];
}

export function useSaveEntrypointConfigurations() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (inputs: SaveEntrypointConfigurationInput[]): Promise<SaveEntrypointConfigurationsResult> => {
            const settled = await Promise.allSettled(
                inputs.map(({ environmentId, settings }) => savePortalSettingsByEnvironmentId(environmentId, settings)),
            );

            const succeededEnvironmentIds: string[] = [];
            const failed: { environmentId: string; error: unknown }[] = [];
            settled.forEach((outcome, index) => {
                const { environmentId } = inputs[index];
                if (outcome.status === 'fulfilled') {
                    succeededEnvironmentIds.push(environmentId);
                } else {
                    failed.push({ environmentId, error: outcome.reason });
                }
            });

            return { succeededEnvironmentIds, failed };
        },
        onSuccess: result => {
            if (result.succeededEnvironmentIds.length > 0) {
                // Two distinct query keys: environment list and portal settings must be invalidated separately.
                void queryClient.invalidateQueries({ queryKey: orgEnvironmentKeys.list() });
                void queryClient.invalidateQueries({ queryKey: portalSettingsKeys.all });
            }

            if (result.failed.length === 0) {
                notify.success('Configuration saved!');
            } else if (result.succeededEnvironmentIds.length > 0) {
                notify.error(
                    result.failed[0].error,
                    `Saved ${result.succeededEnvironmentIds.length} environment(s), but ${result.failed.length} failed to save`,
                );
            } else {
                notify.error(result.failed[0].error, 'Failed to save entrypoint configuration');
            }
        },
        onError: error => notify.error(error, 'Failed to save entrypoint configuration'),
    });
}
