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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { notify } from '../../../shared/notify';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';
import { getPortalSettings, savePortalSettings } from '../../security-plan-types/services/portalSettings';
import { portalSettingsKeys } from '../../security-plan-types/utils/queryKeys';

export function useSaveEnvironmentPortalSettings() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (payload: PortalSettings) => {
            const envId = env?.id;
            if (!envId) {
                throw new Error('Environment is required to save portal settings.');
            }
            await savePortalSettings(envId, payload);
            return getPortalSettings(envId);
        },
        onSuccess: (fresh: PortalSettings) => {
            const envId = env?.id;
            if (envId) {
                queryClient.setQueryData(portalSettingsKeys.env(envId), fresh);
            }
            notify.success('Configuration successfully saved!');
        },
        onError: error => notify.error(error, 'An error occurred while saving the configuration.'),
    });
}
