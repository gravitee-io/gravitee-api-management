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

import { keepPreviousData, useQuery } from '@tanstack/react-query';

import {
    listAuditApis,
    listAuditApplications,
    listAuditEnvironments,
    listEnvAuditEvents,
    listOrgAuditApisByEnvironment,
    listOrgAuditApplicationsByEnvironment,
    listOrgAuditEvents,
    searchEnvAudits,
    searchOrgAudits,
} from '../services/auditLogs';
import type { AuditNamedRef, AuditScope, AuditSearchParams } from '../types/auditLog';
import { auditKeys } from '../utils/queryKeys';

export function useAuditLogs(scope: AuditScope, params: AuditSearchParams, environmentId?: string, enabled = true) {
    return useQuery({
        queryKey: auditKeys.search(scope, environmentId, params),
        queryFn: () => (scope === 'organization' ? searchOrgAudits(params) : searchEnvAudits(environmentId ?? '', params)),
        enabled: enabled && (scope === 'organization' || Boolean(environmentId)),
        // Keep the current page on screen while the next one loads instead of flashing skeletons.
        placeholderData: keepPreviousData,
    });
}

export function useAuditEvents(scope: AuditScope, environmentId?: string, enabled = true) {
    return useQuery({
        queryKey: auditKeys.events(scope, environmentId),
        queryFn: () => (scope === 'organization' ? listOrgAuditEvents() : listEnvAuditEvents(environmentId ?? '')),
        enabled: enabled && (scope === 'organization' || Boolean(environmentId)),
    });
}

export function useAuditEnvironments(enabled: boolean) {
    return useQuery({
        queryKey: auditKeys.environments(),
        queryFn: listAuditEnvironments,
        enabled,
    });
}

export function useAuditApplications(environmentId: string | undefined, enabled: boolean) {
    return useQuery({
        queryKey: auditKeys.applications(environmentId),
        queryFn: () => listAuditApplications(environmentId ?? ''),
        enabled: enabled && Boolean(environmentId),
    });
}

export function useAuditApis(environmentId: string | undefined, enabled: boolean) {
    return useQuery({
        queryKey: auditKeys.apis(environmentId),
        queryFn: () => listAuditApis(environmentId ?? ''),
        enabled: enabled && Boolean(environmentId),
    });
}

// Org application/API pickers fan out per environment. The page owns `useAuditEnvironments` so
// the environment picker and these two queries share one response instead of each hook
// re-subscribing with its own `enabled` flag.
export function useOrgAuditApplications(environments: readonly AuditNamedRef[] | undefined, enabled: boolean) {
    return useQuery({
        queryKey: auditKeys.orgApplications((environments ?? []).map(environment => environment.id)),
        queryFn: () => listOrgAuditApplicationsByEnvironment(environments ?? []),
        enabled: enabled && environments !== undefined,
    });
}

export function useOrgAuditApis(environments: readonly AuditNamedRef[] | undefined, enabled: boolean) {
    return useQuery({
        queryKey: auditKeys.orgApis((environments ?? []).map(environment => environment.id)),
        queryFn: () => listOrgAuditApisByEnvironment(environments ?? []),
        enabled: enabled && environments !== undefined,
    });
}
