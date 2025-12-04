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
import { Component, effect, input, OnInit, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import moment, { Moment } from 'moment';
import { distinctUntilChanged, Observable } from 'rxjs';

import { FilterName } from '../../widget/model/request/enum/filter-name';
import { SelectOption } from '../dropdown-search/dropdown-search-overlay/dropdown-search-overlay.component';
import { DropdownSearchComponent, ResultsLoaderInput, ResultsLoaderOutput } from '../dropdown-search/dropdown-search.component';
import { TimeframeSelectorComponent } from '../timeframe-selector/timeframe-selector.component';
import { customTimeFrames, timeFrames } from '../timeframe-selector/utils/timeframe-ranges';

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
  from?: Moment | null;
  to?: Moment | null;
}

@Component({
  selector: 'gd-generic-filter-bar',
  imports: [DropdownSearchComponent, AsyncPipe, ReactiveFormsModule, MatFormFieldModule, MatSelectModule, TimeframeSelectorComponent],
  templateUrl: './generic-filter-bar.component.html',
  styleUrl: './generic-filter-bar.component.scss',
})
export class GenericFilterBarComponent implements OnInit {
  filters = input.required<Filter[]>();
  currentSelectedFilters = input.required<SelectedFilter[]>();
  defaultPeriod = input<string>('1h');

  selectedFilters = output<SelectedFilter[]>();
  refresh = output<void>();

  form = new FormGroup<Record<string, FormControl<string[] | TimeframeValue | null>>>({});

  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  protected readonly customPeriodKey = 'custom';

  constructor() {
    this.form.addControl('period', new FormControl<TimeframeValue | null>({ period: this.defaultPeriod(), from: null, to: null }));

    effect(() => {
      this.filters().forEach(filter => {
        if (!this.form.contains(filter.key)) {
          if (filter.key === 'period') {
            return;
          } else {
            this.form.addControl(filter.key, new FormControl<string[]>([], { nonNullable: true }));
          }
        }
      });
    });

    effect(() => {
      const currentFilters = this.currentSelectedFilters();

      this.filters()
        .filter(f => f.key !== 'period')
        .forEach(filter => {
          const values = currentFilters.filter(f => f.parentKey === filter.key).map(f => f.value);
          const control = this.form.get(filter.key);

          if (control) {
            control.setValue(values, { emitEvent: false });
          }
        });

      const periodFilter = currentFilters.find(f => f.parentKey === 'period');
      if (periodFilter) {
        const fromFilter = currentFilters.find(f => f.parentKey === 'from');
        const toFilter = currentFilters.find(f => f.parentKey === 'to');

        const tfValue: TimeframeValue = {
          period: periodFilter.value,
          from: fromFilter ? moment(Number.parseInt(fromFilter.value)) : null,
          to: toFilter ? moment(Number.parseInt(toFilter.value)) : null,
        };

        this.form.get('period')?.setValue(tfValue, { emitEvent: false });
      } else {
        const control = this.form.get('period');
        if (control) {
          const currentValue = control.value as TimeframeValue | null;
          if (!currentValue || !currentValue.period) {
            control.setValue({ period: this.defaultPeriod(), from: null, to: null }, { emitEvent: false });
          }
        }
      }
    });
  }

  ngOnInit(): void {
    if (this.currentSelectedFilters().length === 0) {
      this.selectedFilters.emit([{ parentKey: 'period', value: this.defaultPeriod() }]);
    }

    this.form.valueChanges
      .pipe(distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)))
      .subscribe(formValues => {
        const selected: SelectedFilter[] = [];

        Object.keys(formValues).forEach(key => {
          const controlValue = formValues[key];

          if (controlValue == null) return;

          if (Array.isArray(controlValue)) {
            controlValue.forEach(val => {
              selected.push({ parentKey: key, value: val });
            });
          } else if (key === 'period' && typeof controlValue === 'object') {
            const tf = controlValue satisfies TimeframeValue;

            if (tf.period) {
              selected.push({ parentKey: 'period', value: tf.period });
            }

            if (tf.period === this.customPeriodKey) {
              if (tf.from) {
                selected.push({ parentKey: 'from', value: tf.from.valueOf().toString() });
              }
              if (tf.to) {
                selected.push({ parentKey: 'to', value: tf.to.valueOf().toString() });
              }
            }
          }
        });
        this.selectedFilters.emit(selected);
      });
  }

  applyCustomTimeframe() {
    const control = this.form.get('period');
    if (control?.value) {
      const currentValue = control.value as TimeframeValue;
      const updatedValue: TimeframeValue = {
        ...currentValue,
        period: this.customPeriodKey,
      };
      control.setValue(updatedValue);
    }
  }

  refreshFilters() {
    this.refresh.emit();
  }
}
