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

import { Injectable } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup } from '@angular/forms';
import moment, { Moment } from 'moment';

import { customTimeFrames, timeFrames } from '../../../../../../shared/utils/timeFrameRanges';
import { SelectOption } from '../../../../../../shared/components/gio-select-search/gio-select-search.component';
import { Plan } from '../../../../../../entities/management-api-v2';

// Base interface for all filter types
export interface BaseFilterBarFilters {
  period: string;
  from?: number | null;
  to?: number | null;
  plans?: string[] | null;
}

// Base interface for filter chips
export interface FilterChip {
  key: string;
  value: string;
  display: string;
}

// Base form structure that all filter bars share
export interface BaseFilterBarForm {
  timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
  plans: FormControl<string[] | null>;
}

@Injectable({
  providedIn: 'root'
})
export class ApiAnalyticsBaseFilterBarService {
  
  // Common timeframes available to all filter bars
  public readonly timeFrames = [...timeFrames, ...customTimeFrames];
  public readonly customPeriod: string = 'custom';

  /**
   * Generate plan options for select components
   */
  generatePlanOptions(plans: Plan[] | null | undefined): SelectOption[] {
    if (!plans) return [];
    return plans.map((plan) => ({ value: plan.id, label: plan.name }));
  }

  /**
   * Generate filter chips for plans
   */
  generatePlanFilterChips(planIds: string[] | null, plans: Plan[] | null | undefined): FilterChip[] {
    if (!planIds?.length || !plans) return [];
    
    return planIds.map((planId) => {
      const plan = plans.find((p) => p.id === planId);
      return {
        key: 'plans',
        value: planId,
        display: plan ? plan.name : planId,
      };
    });
  }

  /**
   * Check if any filters are active based on filter chips
   */
  isFiltering(filterChips: FilterChip[]): boolean {
    return filterChips.length > 0;
  }

  /**
   * Apply custom timeframe logic
   */
  applyCustomTimeframe<T extends BaseFilterBarFilters>(
    timeframeValue: { period: string; from: Moment | null; to: Moment | null } | null,
    activeFilters: T,
    onFiltersChange: (filters: T) => void
  ): void {
    const from = timeframeValue?.from?.valueOf() ?? null;
    const to = timeframeValue?.to?.valueOf() ?? null;

    const updatedFilters = {
      ...activeFilters,
      from,
      to,
      period: this.customPeriod,
    };

    onFiltersChange(updatedFilters);
  }

  /**
   * Handle timeframe form control changes
   */
  handleTimeframeChange<T extends BaseFilterBarFilters>(
    timeframeValue: { period: string; from: Moment | null; to: Moment | null } | null,
    onFiltersChange: (partial: Partial<T>) => void
  ): void {
    if (timeframeValue?.period && timeframeValue.period !== this.customPeriod) {
      onFiltersChange({ period: timeframeValue.period, from: null, to: null } as Partial<T>);
    }
  }

  /**
   * Handle plans form control changes
   */
  handlePlansChange<T extends BaseFilterBarFilters>(
    plans: string[] | null,
    onFiltersChange: (partial: Partial<T>) => void
  ): void {
    onFiltersChange({ plans } as Partial<T>);
  }

  /**
   * Remove a single value from a filter list
   */
  removeValueFromFilter(
    currentList: string[] | null, 
    value: string, 
    formControl: FormControl<string[] | null>
  ): void {
    const filteredList = (currentList || []).filter((item) => item !== value);
    formControl.setValue(filteredList.length > 0 ? filteredList : null);
  }

  /**
   * Remove a plan filter
   */
  removePlanFilter(
    value: string,
    plansControl: FormControl<string[] | null>
  ): void {
    this.removeValueFromFilter(plansControl.value, value, plansControl);
  }

  /**
   * Update form timeframe from filters
   */
  updateTimeframeFromFilters<T extends BaseFilterBarFilters>(
    filters: T,
    timeframeControl: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>
  ): void {
    timeframeControl.setValue({
      period: filters.period,
      from: filters.from ? moment(filters.from) : null,
      to: filters.to ? moment(filters.to) : null,
    });
  }

  /**
   * Update form plans from filters
   */
  updatePlansFromFilters<T extends BaseFilterBarFilters>(
    filters: T,
    plansControl: FormControl<string[] | null>
  ): void {
    plansControl.setValue(filters.plans || null);
  }

  /**
   * Create base form structure with common controls
   */
  createBaseForm(formBuilder: FormBuilder): { 
    timeframe: FormControl<{ period: string; from: Moment | null; to: Moment | null } | null>;
    plans: FormControl<string[] | null>;
  } {
    return {
      timeframe: formBuilder.control<{ period: string; from: Moment | null; to: Moment | null } | null>(null),
      plans: formBuilder.control<string[] | null>(null),
    };
  }

  /**
   * Reset base filters (plans only - timeframe typically not reset)
   */
  getBaseFilterReset(): { plans: null } {
    return { plans: null };
  }

  /**
   * Emit partial filter updates
   */
  emitFilters<T extends BaseFilterBarFilters>(
    activeFilters: T,
    partial: Partial<T>,
    onFiltersChange: (filters: T) => void
  ): void {
    onFiltersChange({
      ...activeFilters,
      ...partial,
    });
  }

  /**
   * Handle generic removeFilter logic for different filter types
   */
  handleRemoveFilter<TForm extends { [K in keyof TForm]: AbstractControl<any, any> }>(
    key: string,
    value: string,
    form: FormGroup<TForm>,
    supportedFilters: string[]
  ): void {
    if (key === 'plans' && supportedFilters.includes('plans')) {
      const plansControl = (form.controls as any).plans as FormControl<string[] | null>;
      this.removePlanFilter(value, plansControl);
    } else {
      // Handle other filter types by finding the matching form control
      const control = (form.controls as any)[key] as FormControl<string[] | null>;
      if (control && supportedFilters.includes(key)) {
        this.removeValueFromFilter(control.value, value, control);
      }
    }
  }

  /**
   * Generic form update from filters
   */
  updateFormFromFilters<T extends BaseFilterBarFilters, TForm extends { [K in keyof TForm]: AbstractControl<any, any> }>(
    filters: T,
    form: FormGroup<TForm>,
    supportedFilters: string[]
  ): void {
    if (!form) return;

    // Update common fields
    this.updateTimeframeFromFilters(filters, (form.controls as any).timeframe);
    
    if (supportedFilters.includes('plans')) {
      this.updatePlansFromFilters(filters, (form.controls as any).plans);
    }

    // Update additional fields dynamically
    supportedFilters.forEach(filterKey => {
      if (['timeframe', 'plans', 'period', 'from', 'to'].includes(filterKey)) {
        return; // Already handled
      }

      const control = (form.controls as any)[filterKey] as FormControl<any>;
      const filterValue = (filters as any)[filterKey];
      if (control && filterValue !== undefined) {
        control.setValue(filterValue);
      }
    });
  }

  /**
   * Generic reset filters based on supported filter types
   */
  getResetFiltersObject<T>(supportedFilters: string[]): Partial<T> {
    const reset: any = {};
    
    supportedFilters.forEach(filterKey => {
      if (['period', 'from', 'to'].includes(filterKey)) {
        return; // Don't reset timeframe
      }
      reset[filterKey] = null;
    });

    return reset;
  }
}
