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

import { searchAuditLogs } from '../services/auditLogs';
import type { AuditSearchParams } from '../types/auditLogs.types';
import { apiAuditKeys } from '../utils/queryKeys';

export function useAuditLogs(apiId: string | undefined, params: AuditSearchParams) {
    const env = useEnvironment();
    return useQuery({
        queryKey: apiAuditKeys.list(env?.id ?? '', apiId ?? '', params),
        queryFn: () => searchAuditLogs(env!.id, apiId!, params),
        enabled: Boolean(env && apiId),
        staleTime: 30_000,
    });
}
