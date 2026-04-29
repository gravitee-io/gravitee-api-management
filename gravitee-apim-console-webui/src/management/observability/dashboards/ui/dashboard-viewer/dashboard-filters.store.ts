/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import {
  BasicTimeframe,
  calculateCustomInterval,
  DEFAULT_VIEW_STATE_PERIOD,
  decodeViewState,
  encodeViewState,
  FilterCondition,
  ID_BASED_FILTER_NAMES,
  RequestFilter,
  TimeRange,
  timeFrameRangesParams,
  timeFrames,
  toRequestFilter,
} from '@gravitee/gravitee-dashboard';

import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import moment, { Moment } from 'moment';
import { Subscription } from 'rxjs';

import { FilterLabelResolver } from './filter-label.resolver';

export interface TimeframeValue {
  period: string;
  from: Moment | null;
  to: Moment | null;
}

const DEFAULT_PERIOD = DEFAULT_VIEW_STATE_PERIOD as BasicTimeframe;
// Module-level fallback: timeFrames is a hardcoded non-empty array, so timeFrames[0] is always defined.
const DEFAULT_TIMEFRAME = timeFrames.find(t => t.id === DEFAULT_PERIOD) ?? timeFrames[0];
const RESOLVING_LABEL = 'Loading...';

@Injectable()
export class DashboardFiltersStore {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly labelResolver = inject(FilterLabelResolver, { optional: true });

  private readonly _conditions = signal<FilterCondition[]>([]);
  readonly conditions = this._conditions.asReadonly();

  readonly periodControl = new FormControl<TimeframeValue>({ period: DEFAULT_PERIOD, from: null, to: null }, { nonNullable: true });

  private readonly _refreshToken = signal(0);
  readonly refreshToken = this._refreshToken.asReadonly();

  private _skipNextQueryParamsEmit = false;
  private labelResolutionSubscription?: Subscription;
  private labelResolutionGeneration = 0;

  /**
   * Mirror of `periodControl.value` exposed as a signal.
   *
   * We intentionally avoid `toSignal(periodControl.valueChanges, …)` here: when
   * the URL is hydrated via `setValue(…, { emitEvent: false })`, `valueChanges`
   * does not fire and the derived signal would stay stuck on the initial
   * default ('5m'). Widgets reading `timeRange()` would then query the wrong
   * period (e.g. last 5 minutes instead of the hydrated 'last week'), producing
   * empty dashboards on hard refresh.
   *
   * Instead we keep an explicit writable signal that we synchronise both from
   * `valueChanges` (user edits) and from `hydrateFromRoute` (URL changes).
   */
  private readonly _periodValue = signal<TimeframeValue>({ period: DEFAULT_PERIOD, from: null, to: null });

  readonly timeRange = computed<TimeRange>(() => {
    const tv = this._periodValue();
    if (tv.period === 'custom' && tv.from && tv.to) {
      return {
        from: tv.from.toISOString(),
        to: tv.to.toISOString(),
      };
    }
    const tf = timeFrames.find(t => t.id === tv.period) ?? DEFAULT_TIMEFRAME;
    const params = timeFrameRangesParams(tf.id);
    return {
      from: moment(params.from).toISOString(),
      to: moment(params.to).toISOString(),
    };
  });

  readonly interval = computed<number | undefined>(() => {
    const tv = this._periodValue();
    if (tv.period === 'custom' && tv.from && tv.to) {
      return calculateCustomInterval(tv.from.valueOf(), tv.to.valueOf());
    }
    const tf = timeFrames.find(t => t.id === tv.period) ?? DEFAULT_TIMEFRAME;
    return timeFrameRangesParams(tf.id).interval;
  });

  readonly timeRangeEpoch = computed<{ from: number; to: number }>(() => {
    const tv = this._periodValue();
    if (tv.period === 'custom' && tv.from && tv.to) {
      return { from: tv.from.valueOf(), to: tv.to.valueOf() };
    }
    const tf = timeFrames.find(t => t.id === tv.period) ?? DEFAULT_TIMEFRAME;
    const params = timeFrameRangesParams(tf.id);
    return { from: params.from, to: params.to };
  });

  readonly requestFilters = computed<RequestFilter[]>(() => this._conditions().map(toRequestFilter));

  constructor() {
    this.hydrateFromRoute(this.route.snapshot.queryParams);
    this.periodControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      this._periodValue.set(value);
      this.syncToRouter();
    });
    this.listenQueryParams();
  }

  add(condition: FilterCondition): void {
    this._conditions.update(prev => {
      const compatIdx = prev.findIndex(c => c.field === condition.field && areMergeableOperators(c.operator, condition.operator));
      if (compatIdx === -1) return [...prev, condition];

      const existing = prev[compatIdx];
      const mergedValues = [...new Set([...existing.values, ...condition.values])];
      if (mergedValues.length === existing.values.length) return prev;

      const existingLabels = existing.valueLabels ?? existing.values;
      const newLabels = condition.valueLabels ?? condition.values;
      const labelMap = new Map<string, string>();
      existing.values.forEach((v, i) => labelMap.set(v, existingLabels[i] ?? v));
      condition.values.forEach((v, i) => labelMap.set(v, newLabels[i] ?? v));
      const mergedLabels = mergedValues.map(v => labelMap.get(v) ?? v);

      const operator = mergedValues.length >= 2 ? promoteMembershipOperator(existing.operator) : existing.operator;

      const next = [...prev];
      next[compatIdx] = { ...existing, operator, values: mergedValues, valueLabels: mergedLabels };
      return next;
    });
    this.syncToRouter();
  }

  edit(index: number, condition: FilterCondition): void {
    this._conditions.update(prev => {
      if (index < 0 || index >= prev.length) return prev;
      const next = [...prev];
      next[index] = condition;
      return next;
    });
    this.syncToRouter();
  }

  remove(index: number): void {
    this._conditions.update(prev => {
      if (index < 0 || index >= prev.length) return prev;
      return prev.filter((_, i) => i !== index);
    });
    this.syncToRouter();
  }

  clear(): void {
    this._conditions.set([]);
    this.syncToRouter();
  }

  refresh(): void {
    this._refreshToken.update(n => n + 1);
  }

  applyCustomTimeframe(): void {
    this.syncToRouter();
  }

  syncPeriodToRouter(): void {
    this.syncToRouter();
  }

  private hydrateFromRoute(params: Params): void {
    const q = params['q'] as string | undefined;
    const v = params['v'] as string | undefined;
    const { conditions, timeframe } = decodeViewState(q, v);

    if (timeframe.period === 'custom' && timeframe.from != null && timeframe.to != null) {
      this.periodControl.setValue({ period: 'custom', from: moment(timeframe.from), to: moment(timeframe.to) }, { emitEvent: false });
    } else {
      this.periodControl.setValue({ period: timeframe.period, from: null, to: null }, { emitEvent: false });
    }
    // Keep the derived signal in sync, since `setValue({ emitEvent: false })`
    // does not trigger `valueChanges` (see comment on `_periodValue`).
    this._periodValue.set(this.periodControl.value);

    this.labelResolutionSubscription?.unsubscribe();
    const generation = ++this.labelResolutionGeneration;
    this._conditions.set(this.labelResolver ? withPendingLabels(conditions) : conditions);

    if (conditions.length > 0 && this.labelResolver) {
      this.labelResolutionSubscription = this.labelResolver
        .resolveLabels(conditions)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(enriched => {
          if (generation !== this.labelResolutionGeneration) return;
          this._conditions.update(current => mergeResolvedLabels(current, enriched));
        });
    }
  }

  private syncToRouter(): void {
    const tv = this.periodControl.value;
    const viewTimeframe = {
      period: tv.period,
      from: tv.period === 'custom' && tv.from ? tv.from.valueOf() : null,
      to: tv.period === 'custom' && tv.to ? tv.to.valueOf() : null,
    };

    const encoded = encodeViewState(this._conditions(), viewTimeframe);

    const queryParams: Record<string, string | null> = {
      q: encoded?.q ?? null,
      v: encoded?.v ?? null,
      period: null,
      from: null,
      to: null,
    };

    this._skipNextQueryParamsEmit = true;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private listenQueryParams(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      if (this._skipNextQueryParamsEmit) {
        this._skipNextQueryParamsEmit = false;
        return;
      }
      this.hydrateFromRoute(params);
    });
  }
}

const MEMBERSHIP_FAMILIES: ReadonlyArray<ReadonlySet<string>> = [new Set(['EQ', 'IN']), new Set(['NEQ', 'NOT_IN'])];

function areMergeableOperators(a: string, b: string): boolean {
  return MEMBERSHIP_FAMILIES.some(family => family.has(a) && family.has(b));
}

function promoteMembershipOperator(op: string): string {
  if (op === 'EQ') return 'IN';
  if (op === 'NEQ') return 'NOT_IN';
  return op;
}

function withPendingLabels(conditions: FilterCondition[]): FilterCondition[] {
  return conditions.map(condition => {
    if (!ID_BASED_FILTER_NAMES.includes(condition.field) || condition.valueLabels?.length === condition.values.length) {
      return condition;
    }
    return { ...condition, valueLabels: condition.values.map(() => RESOLVING_LABEL) };
  });
}

function mergeResolvedLabels(current: FilterCondition[], resolved: FilterCondition[]): FilterCondition[] {
  const resolvedLabelsByField = new Map<string, Map<string, string>>();
  resolved.forEach(condition => {
    const labelsByValue = resolvedLabelsByField.get(condition.field) ?? new Map<string, string>();
    condition.values.forEach((value, index) => {
      const label = condition.valueLabels?.[index];
      if (label) {
        labelsByValue.set(value, label);
      }
    });
    resolvedLabelsByField.set(condition.field, labelsByValue);
  });

  return current.map(condition => {
    const labelsByValue = resolvedLabelsByField.get(condition.field);
    if (!labelsByValue) return condition;

    return {
      ...condition,
      valueLabels: condition.values.map((value, index) => labelsByValue.get(value) ?? condition.valueLabels?.[index] ?? value),
    };
  });
}
