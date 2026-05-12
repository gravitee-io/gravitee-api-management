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
import type { ApimRuntimeConfig } from '../core/context/apimRuntimeContext';

export const apiDetailKeys = {
    all: ['api-detail'] as const,
    detail: (runtime: ApimRuntimeConfig, apiId: string) =>
        [...apiDetailKeys.all, runtime.managementBaseURL, runtime.environmentId, apiId] as const,
};

export const proxyCreationKeys = {
    all: ['proxy-creation'] as const,
    bootstrap: (runtime: ApimRuntimeConfig) =>
        [...proxyCreationKeys.all, 'bootstrap', runtime.managementBaseURL, runtime.organizationId, runtime.environmentId] as const,
    policySchema: (runtime: ApimRuntimeConfig, policyId: string) =>
        [
            ...proxyCreationKeys.all,
            'policy-schema',
            policyId,
            runtime.managementBaseURL,
            runtime.organizationId,
            runtime.environmentId,
        ] as const,
};

export const orgTagKeys = {
    all: ['org-tags'] as const,
    list: (runtime: ApimRuntimeConfig) => [...orgTagKeys.all, runtime.managementBaseURL, runtime.organizationId] as const,
};

export const apiEventsKeys = {
    all: ['api-events'] as const,
    list: (runtime: ApimRuntimeConfig, apiId: string, page: number, perPage: number) =>
        [...apiEventsKeys.all, runtime.managementBaseURL, runtime.environmentId, apiId, page, perPage] as const,
};

export const apiAuditKeys = {
    all: ['api-audits'] as const,
    list: (runtime: ApimRuntimeConfig, apiId: string, params: object) =>
        [...apiAuditKeys.all, runtime.managementBaseURL, runtime.environmentId, apiId, params] as const,
    events: (runtime: ApimRuntimeConfig, apiId: string) =>
        [...apiAuditKeys.all, 'events', runtime.managementBaseURL, runtime.environmentId, apiId] as const,
};

export const apiMemberKeys = {
    all: ['api-members'] as const,
    list: (runtime: ApimRuntimeConfig, apiId: string) =>
        [...apiMemberKeys.all, runtime.managementBaseURL, runtime.environmentId, apiId] as const,
    groups: (runtime: ApimRuntimeConfig, apiId: string) =>
        [...apiMemberKeys.all, 'groups', runtime.managementBaseURL, runtime.environmentId, apiId] as const,
};

export const apiRoleKeys = {
    all: ['api-roles'] as const,
    list: (runtime: ApimRuntimeConfig) => [...apiRoleKeys.all, runtime.managementBaseURL, runtime.organizationId] as const,
};

export const groupKeys = {
    all: ['groups'] as const,
    list: (runtime: ApimRuntimeConfig) => [...groupKeys.all, runtime.managementBaseURL, runtime.environmentId] as const,
};
