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

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import { listFederatedApis } from '../services/integrationDetail';
import { integrationKeys } from '../utils/queryKeys';

const INGESTION_POLL_INTERVAL_MS = 5_000;

export function useIntegrationFederatedApis(
    integrationId: string,
    page: number,
    perPage: number,
    options?: Readonly<{ pollWhileIngesting?: boolean }>,
) {
    const env = useEnvironment();
    const pollWhileIngesting = options?.pollWhileIngesting ?? false;

    return useQuery({
        queryKey: integrationKeys.federatedApisList(env?.id ?? '', integrationId, page, perPage),
        queryFn: () => listFederatedApis(env!.id, integrationId, page, perPage),
        enabled: Boolean(env && integrationId),
        refetchInterval: pollWhileIngesting ? INGESTION_POLL_INTERVAL_MS : false,
    });
}
