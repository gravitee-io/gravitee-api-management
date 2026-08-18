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

import { useMemo } from 'react';
import type { DateRange } from 'react-day-picker';

import type { AuditDatePreset } from '../types/auditLog';
import { resolveAuditDateRange } from '../utils/auditListFormat';

/**
 * Snapshots relative presets (24h / 7d / …) against Date.now() when the filter
 * actually changes. Computing from/to on every render would churn the React Query
 * key and refetch forever.
 */
export function useResolvedAuditDateRange(preset: AuditDatePreset, customRange: DateRange | undefined): { from?: number; to?: number } {
    const customFrom = customRange?.from?.getTime();
    const customTo = customRange?.to?.getTime();
    return useMemo(() => {
        const range =
            customFrom === undefined && customTo === undefined
                ? undefined
                : {
                      from: customFrom === undefined ? undefined : new Date(customFrom),
                      to: customTo === undefined ? undefined : new Date(customTo),
                  };
        return resolveAuditDateRange(preset, range);
    }, [preset, customFrom, customTo]);
}
