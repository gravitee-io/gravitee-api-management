/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { FilterCondition } from '../filter.model';

export const DEFAULT_VIEW_STATE_PERIOD = '5m';

const VALID_RELATIVE_PERIODS = new Set(['1m', DEFAULT_VIEW_STATE_PERIOD, '1h', '1d', '1w', '1M']);

export interface FilterUrlEntry {
  field: string;
  operator: string;
  value: string | string[];
}

export interface TimeRangeUrlEntry {
  type: 'relative' | 'absolute';
  period?: string;
  from?: number;
  to?: number;
}

export interface ViewStatePayload {
  filter?: FilterUrlEntry[];
  time_range?: TimeRangeUrlEntry;
}

export interface ViewStateTimeframe {
  period: string;
  from: number | null;
  to: number | null;
}

export function encodeViewState(conditions: FilterCondition[], timeframe: ViewStateTimeframe): { q: string; v: string } | null {
  const payload: ViewStatePayload = {};

  if (conditions.length > 0) {
    payload.filter = conditions.map(c => ({
      field: c.field,
      operator: c.operator,
      value: c.values.length === 1 ? c.values[0] : c.values,
    }));
  }

  if (timeframe.period === 'custom' && timeframe.from != null && timeframe.to != null) {
    payload.time_range = { type: 'absolute', from: timeframe.from, to: timeframe.to };
  } else if (isValidRelativePeriod(timeframe.period) && timeframe.period !== DEFAULT_VIEW_STATE_PERIOD) {
    payload.time_range = { type: 'relative', period: timeframe.period };
  }

  if (!payload.filter && !payload.time_range) return null;

  return { q: JSON.stringify(payload), v: '1' };
}

export function decodeViewState(
  q: string | null | undefined,
  v: string | null | undefined,
): { conditions: FilterCondition[]; timeframe: ViewStateTimeframe } {
  const defaultResult: { conditions: FilterCondition[]; timeframe: ViewStateTimeframe } = {
    conditions: [],
    timeframe: { period: DEFAULT_VIEW_STATE_PERIOD, from: null, to: null },
  };

  if (!q || v !== '1') return defaultResult;

  try {
    const payload = JSON.parse(q) as ViewStatePayload;

    let timeframe: ViewStateTimeframe;
    const tr = payload.time_range;
    if (tr?.type === 'absolute' && tr.from != null && tr.to != null) {
      timeframe = { period: 'custom', from: tr.from, to: tr.to };
    } else if (tr?.type === 'relative' && isValidRelativePeriod(tr.period)) {
      timeframe = { period: tr.period, from: null, to: null };
    } else {
      timeframe = { period: DEFAULT_VIEW_STATE_PERIOD, from: null, to: null };
    }

    const filter = Array.isArray(payload.filter) ? payload.filter : [];
    const conditions: FilterCondition[] = filter
      .filter(entry => entry.field && entry.operator)
      .map(entry => ({
        field: entry.field,
        label: entry.field,
        operator: entry.operator,
        values: Array.isArray(entry.value) ? entry.value : [entry.value],
      }));

    return { conditions, timeframe };
  } catch {
    console.warn('[filter-url-codec] Failed to parse q parameter, ignoring');
    return defaultResult;
  }
}

function isValidRelativePeriod(period: string | undefined): period is string {
  return period != null && VALID_RELATIVE_PERIODS.has(period);
}
