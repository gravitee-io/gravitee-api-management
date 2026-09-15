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
import { useParams } from 'react-router-dom';

import { listAlertApplications, listAlertPlans, listAlertTenants } from '../services/alertLookups';
import type { AlertMetricLookups } from '../utils/alertMetricValues';
import { apiAlertKeys } from '../utils/queryKeys';

export function useAlertLookupOptions(): AlertMetricLookups {
    const env = useEnvironment();
    const environmentId = env?.id ?? '';
    const { apiId } = useParams<{ apiId: string }>();

    const tenantsQuery = useQuery({
        queryKey: apiAlertKeys.tenants(environmentId),
        queryFn: () => listAlertTenants(environmentId),
        enabled: !!environmentId,
    });
    const applicationsQuery = useQuery({
        queryKey: apiAlertKeys.lookupApplications(environmentId, apiId ?? ''),
        queryFn: () => listAlertApplications(environmentId, apiId!),
        enabled: !!environmentId && !!apiId,
    });
    const plansQuery = useQuery({
        queryKey: apiAlertKeys.lookupPlans(environmentId, apiId ?? ''),
        queryFn: () => listAlertPlans(environmentId, apiId!),
        enabled: !!environmentId && !!apiId,
    });

    return {
        tenants: (tenantsQuery.data ?? []).map(t => ({ value: t.id, label: t.name })),
        applications: (applicationsQuery.data ?? []).map(a => ({ value: a.id, label: a.name })),
        plans: (plansQuery.data ?? []).map(p => ({ value: p.id, label: p.name })),
    };
}
