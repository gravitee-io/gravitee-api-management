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
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, input, OnInit, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { isEqual } from 'lodash';
import moment, { Moment } from 'moment';
import { distinctUntilChanged, map, Observable } from 'rxjs';

import { FilterName } from '../../widget/model/request/enum/filter-name';
import { SelectOption } from '../dropdown-search/dropdown-search-overlay/dropdown-search-overlay.component';
import { DropdownSearchComponent, ResultsLoaderInput, ResultsLoaderOutput } from '../dropdown-search/dropdown-search.component';
import { TimeframeSelectorComponent } from '../timeframe-selector/timeframe-selector.component';
import { BasicTimeframe, customTimeFrames, timeFrames } from '../timeframe-selector/utils/timeframe-ranges';

export interface SelectedFilter {
  parentKey: string;
  value: string;
}

export interface Filter {
  key: FilterName | 'period';
  label: string;
  data?: SelectOption[];
  data$?: Observable<SelectOption[]>;
  dataLoader?: (input: ResultsLoaderInput) => Observable<ResultsLoaderOutput>;
}

export interface TimeframeValue {
  period: string;
  from?: Moment;
  to?: Moment;
}

type FilterControlValue = { period: TimeframeValue | undefined | null } & Record<string, string[] | TimeframeValue | undefined | null>;

type FilterFormControls = {
  period: FormControl<TimeframeValue | undefined | null>;
} & Record<string, FormControl<string[] | TimeframeValue | undefined | null>>;

@Component({
  selector: 'gd-generic-filter-bar',
  imports: [DropdownSearchComponent, AsyncPipe, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, TimeframeSelectorComponent],
  templateUrl: './generic-filter-bar.component.html',
  styleUrl: './generic-filter-bar.component.scss',
})
export class GenericFilterBarComponent implements OnInit {
  filters = input.required<Filter[]>();
  currentSelectedFilters = input.required<SelectedFilter[]>();
  defaultPeriod = input.required<BasicTimeframe>();

  selectedFilters = output<SelectedFilter[]>();
  refresh = output<void>();
  form = new FormGroup<FilterFormControls>({
    period: new FormControl<TimeframeValue>({ period: '', from: undefined, to: undefined }, { nonNullable: true }),
  });

  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  protected readonly customPeriodKey = 'custom';
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const currentFilters = this.currentSelectedFilters();
      this.filters().forEach(filter => {
        if (filter.key !== 'period' && !this.form.contains(filter.key)) {
          const values = currentFilters.filter(f => f.parentKey === filter.key).map(f => f.value);
          this.form.addControl(filter.key, new FormControl<string[]>(values, { nonNullable: true }), { emitEvent: false });
        }
      });
    });

    effect(() => {
      const currentFilters = this.currentSelectedFilters();
      const defaultPeriod = this.defaultPeriod();
      const periodControl = this.getPeriodControl();

      if (periodControl.value.period === '') {
        periodControl.setValue({ period: defaultPeriod, from: undefined, to: undefined }, { emitEvent: false });
      }

      this.updateFormFromInputs(currentFilters);
    });
  }

  ngOnInit(): void {
    if (this.currentSelectedFilters().length === 0) {
      this.selectedFilters.emit([{ parentKey: 'period', value: this.defaultPeriod() }]);
    }

    this.form.valueChanges
      .pipe(
        map(() => {
          const formValues = this.form.getRawValue() as Partial<FilterControlValue>;
          return this.transformFormValuesToSelectedFilters(formValues);
        }),
        distinctUntilChanged((prev, curr) => isEqual(prev, curr)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(selected => {
        this.selectedFilters.emit(selected);
      });
  }

  applyCustomTimeframe(): void {
    const control = this.getPeriodControl();
    control.setValue({
      ...control.value,
      period: this.customPeriodKey,
    });
  }

  refreshFilters(): void {
    this.refresh.emit();
  }

  private getPeriodControl(): FormControl<TimeframeValue> {
    return this.form.controls['period'] as FormControl<TimeframeValue>;
  }

  private transformFormValuesToSelectedFilters(formValues: Partial<FilterControlValue>): SelectedFilter[] {
    const selected: SelectedFilter[] = [];

    const periodValue = formValues.period;
    if (periodValue && this.isTimeframeValue(periodValue) && periodValue.period) {
      selected.push({ parentKey: 'period', value: periodValue.period });

      if (periodValue.period === this.customPeriodKey) {
        if (periodValue.from) {
          selected.push({ parentKey: 'from', value: periodValue.from.valueOf().toString() });
        }
        if (periodValue.to) {
          selected.push({ parentKey: 'to', value: periodValue.to.valueOf().toString() });
        }
      }
    }

    Object.keys(this.form.controls).forEach(key => {
      if (key === 'period') return;

      const control = this.form.get(key);
      if (!control) return;

      const controlValue = control.value;
      if (Array.isArray(controlValue) && controlValue.length > 0) {
        controlValue.forEach(val => {
          selected.push({ parentKey: key, value: val });
        });
      }
    });

    return selected;
  }

  private updateFormFromInputs(currentFilters: SelectedFilter[]): void {
    this.filters()
      .filter(f => f.key !== 'period')
      .forEach(filter => {
        const values = currentFilters.filter(f => f.parentKey === filter.key).map(f => f.value);

        const control = this.form.get(filter.key);
        if (control && !isEqual(control.value, values)) {
          control.setValue(values, { emitEvent: false });
        }
      });

    this.updatePeriodFromInput(currentFilters);
  }

  private updatePeriodFromInput(currentFilters: SelectedFilter[]): void {
    const periodFilter = currentFilters.find(f => f.parentKey === 'period');
    const control = this.getPeriodControl();

    if (periodFilter) {
      const fromFilter = currentFilters.find(f => f.parentKey === 'from');
      const toFilter = currentFilters.find(f => f.parentKey === 'to');

      const newTimeframe: TimeframeValue = {
        period: periodFilter.value,
        from: fromFilter ? moment(Number.parseInt(fromFilter.value, 10)) : undefined,
        to: toFilter ? moment(Number.parseInt(toFilter.value, 10)) : undefined,
      };

      if (!isEqual(control.value, newTimeframe)) {
        control.setValue(newTimeframe, { emitEvent: false });
      }
    } else if (this.currentSelectedFilters().length === 0) {
      const defaultState = {
        period: this.defaultPeriod(),
        from: undefined,
        to: undefined,
      };
      if (!isEqual(control.value, defaultState)) {
        control.setValue(defaultState, { emitEvent: false });
      }
    }
  }

  private isTimeframeValue(value: unknown): value is TimeframeValue {
    return typeof value === 'object' && value !== null && 'period' in value && typeof value.period === 'string';
  }
}
