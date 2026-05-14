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

import { getEnvironmentPortalSettings } from '../../../services/portal/settings';
import { GATEWAY_URL_PLACEHOLDER } from '../utils/apiProxyMapper';
import { portalSettingsKeys } from '../utils/queryKeys';

export function useGatewayPrefix(): string {
    const env = useEnvironment();
    const { data } = useQuery({
        queryKey: portalSettingsKeys.env(env?.id ?? ''),
        queryFn: () => getEnvironmentPortalSettings(env!.id),
        enabled: Boolean(env?.id),
        staleTime: 5 * 60_000,
    });
    const entrypoint = data?.portal?.entrypoint?.replace(/\/$/, '');
    return entrypoint || GATEWAY_URL_PLACEHOLDER;
}
