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

import type { AuditDatePreset, AuditReferenceType } from '../types/auditLog';

export interface AuditFilterValues {
    readonly event: string;
    readonly referenceType: AuditReferenceType | '';
    readonly environmentId: string;
    readonly applicationId: string;
    readonly apiId: string;
    readonly datePreset: AuditDatePreset;
    readonly customRange: { from?: Date; to?: Date } | undefined;
}

/**
 * Single definition of "the user has narrowed the results", used both by the toolbar (to show Reset)
 * and by the table (to pick the no-results empty state over the first-use one). Keeping one predicate
 * stops the two from drifting apart.
 */
export function hasActiveAuditFilters(filters: AuditFilterValues): boolean {
    return Boolean(
        filters.event ||
            filters.referenceType ||
            filters.environmentId ||
            filters.applicationId ||
            filters.apiId ||
            filters.datePreset ||
            filters.customRange?.from ||
            filters.customRange?.to,
    );
}
