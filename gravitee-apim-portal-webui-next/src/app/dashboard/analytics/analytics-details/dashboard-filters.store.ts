/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import {
  decodeViewState,
  encodeViewState,
  FilterCondition,
  RequestFilter,
  toRequestFilter,
  ViewStateTimeframe,
} from '@gravitee/gravitee-dashboard';

@Injectable()
export class DashboardFiltersStore {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly _conditions = signal<FilterCondition[]>([]);
  readonly conditions = this._conditions.asReadonly();

  readonly requestFilters = computed<RequestFilter[]>(() => this._conditions().map(toRequestFilter));

  private _skipNextQueryParamsEmit = false;

  constructor() {
    this.hydrateFromRoute(this.route.snapshot.queryParams);
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

  private hydrateFromRoute(params: Record<string, string>): void {
    const q = params['q'] as string | undefined;
    const v = params['v'] as string | undefined;
    const { conditions } = decodeViewState(q, v);
    this._conditions.set(conditions);
  }

  private syncToRouter(): void {
    const timeframe: ViewStateTimeframe = { period: '5m', from: null, to: null };
    const encoded = encodeViewState(this._conditions(), timeframe);

    const queryParams: Record<string, string | null> = {
      q: encoded?.q ?? null,
      v: encoded?.v ?? null,
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
