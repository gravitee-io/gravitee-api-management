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

import { Component, DestroyRef, computed, inject } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import moment, { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CommonFilterForm, CommonFilters, FilterChip } from './common-filters.types';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';

@Component({
  template: '', // Abstract base component - no template
})
export abstract class CommonFiltersComponent<TFilters extends CommonFilters = CommonFilters> {
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  customPeriod: string = 'custom';

  protected readonly destroyRef = inject(DestroyRef);
  protected readonly formBuilder = inject(FormBuilder);

  public planOptions = computed<SelectOption[]>(() => {
    const plans = this.plans() || [];
    return plans.map((plan) => ({ value: plan.id, label: plan.name }));
  });

  public isFiltering = computed(() => this.currentFilterChips().length > 0);

  public currentFilterChips = computed(() => this.getAllFilterChips());

  abstract activeFilters: any;
  abstract filtersChange: any;
  abstract refresh: any;
  abstract plans: any;
  abstract form: FormGroup<any>;

  protected createCommonFormControls(): CommonFilterForm {
    return {
      timeframe: this.formBuilder.control<{ period: string; from: Moment | null; to: Moment | null } | null>(null),
      plans: this.formBuilder.control<string[] | null>(null),
    };
  }

  protected setupCommonFormSubscriptions(): void {
    this.form
      .get('timeframe')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((tf: any) => {
        if (tf?.period && tf.period !== this.customPeriod) {
          this.emitFilters({ period: tf.period, from: null, to: null } as Partial<TFilters>);
        }
      });

    this.form
      .get('plans')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((plans: any) => {
        this.emitFilters({ plans } as Partial<TFilters>);
      });
  }

  applyCustomTimeframe() {
    const tf = this.form.get('timeframe')?.value;
    const from = tf?.from?.valueOf() ?? null;
    const to = tf?.to?.valueOf() ?? null;

    const currentFilters = this.activeFilters();
    const updatedFilters = {
      ...currentFilters,
      from,
      to,
      period: this.customPeriod,
    };

    this.filtersChange.emit(updatedFilters);
  }

  refreshFilters() {
    this.refresh.emit();
  }

  protected getCommonFilterChips(filters: TFilters): FilterChip[] {
    const chips: FilterChip[] = [];

    if (filters?.plans?.length) {
      const plans = this.plans() || [];
      filters.plans.forEach((planId) => {
        const plan = plans?.find((p) => p.id === planId);
        const display = plan ? plan.name : planId;
        chips.push({
          key: 'plans',
          value: planId,
          display: display,
        });
      });
    }

    return chips;
  }

  protected getAllFilterChips(): FilterChip[] {
    const filters = this.activeFilters();
    return this.getCommonFilterChips(filters);
  }

  removeFilter(key: string, value: string) {
    if (key === 'plans') {
      const plansControl = this.form.get('plans') as FormControl<string[] | null>;
      this.removeValueFromFilter(plansControl.value, value, plansControl);
    } else {
      this.removeSpecificFilter(key, value);
    }
  }

  resetAllFilters() {
    const commonReset = { plans: null } as Partial<TFilters>;
    const specificReset = this.getSpecificFilterReset();
    this.emitFilters({ ...commonReset, ...specificReset });
  }

  protected removeValueFromFilter(currentList: string[] | null, value: string, formControl: FormControl<string[] | null>): void {
    const filteredList = (currentList || []).filter((item) => item !== value);
    formControl.setValue(filteredList.length > 0 ? filteredList : null);
  }

  protected emitFilters(partial: Partial<TFilters>) {
    this.filtersChange.emit({
      ...this.activeFilters(),
      ...partial,
    });
  }

  protected updateFormFromFilters(filters: TFilters) {
    if (this.form) {
      const commonFormData = {
        timeframe: {
          period: filters.period,
          from: filters.from ? moment(filters.from) : null,
          to: filters.to ? moment(filters.to) : null,
        },
        plans: filters.plans,
      };

      const specificFormData = this.getSpecificFormData(filters);

      this.form.patchValue({
        ...commonFormData,
        ...specificFormData,
      });
    }
  }

  protected removeSpecificFilter(_key: string, _value: string): void {
    // Override in subclasses for specific filters
  }

  protected getSpecificFilterReset(): Partial<TFilters> {
    return {} as Partial<TFilters>;
  }

  protected getSpecificFormData(_filters: TFilters): any {
    return {};
  }
}
