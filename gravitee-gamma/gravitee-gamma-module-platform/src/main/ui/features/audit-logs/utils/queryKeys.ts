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

import type { AuditScope, AuditSearchParams } from '../types/auditLog';

export const auditKeys = {
    all: ['audit-logs'] as const,
    search: (scope: AuditScope, environmentId: string | undefined, params: AuditSearchParams) =>
        [...auditKeys.all, 'search', scope, environmentId ?? '', params] as const,
    events: (scope: AuditScope, environmentId: string | undefined) => [...auditKeys.all, 'events', scope, environmentId ?? ''] as const,
    environments: () => [...auditKeys.all, 'environments'] as const,
    applications: (environmentId: string | undefined) => [...auditKeys.all, 'applications', environmentId ?? ''] as const,
    apis: (environmentId: string | undefined) => [...auditKeys.all, 'apis', environmentId ?? ''] as const,
    orgApplications: () => [...auditKeys.all, 'org-applications'] as const,
    orgApis: () => [...auditKeys.all, 'org-apis'] as const,
} as const;
