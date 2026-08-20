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
 * Resolves relative presets (24h / 7d / …) against `anchor` — the instant the preset was chosen.
 *
 * The anchor is a caller-owned value on purpose. Reading Date.now() here and leaning on useMemo to
 * hold it still is not safe: React documents useMemo as a performance hint it may discard, and a
 * discarded memo would move from/to, change the React Query key, and refetch. With the anchor passed
 * in, this hook is pure and the memo is only an optimisation.
 */
export function useResolvedAuditDateRange(
    preset: AuditDatePreset,
    customRange: DateRange | undefined,
    anchor: number,
): { from?: number; to?: number } {
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
        return resolveAuditDateRange(preset, range, anchor);
    }, [preset, customFrom, customTo, anchor]);
}
