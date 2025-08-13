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
import { Component, DestroyRef, effect, input, OnInit, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOptionModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatRadioButton } from '@angular/material/radio';

import { customTimeFrames, DATE_TIME_FORMATS, timeFrames } from '../../utils/timeFrameRanges';

interface GioTimeframeWidgetForm {
  period: FormControl<string>;
  from: FormControl<Moment | null>;
  to: FormControl<Moment | null>;
}

export interface ApiAnalyticsProxyFilters {
  period: string;
  from?: number | null;
  to?: number | null;
}

@Component({
  selector: 'gio-timeframe-widget',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOptionModule,
    MatSelectModule,
    MatInputModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    MatRadioButton,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
  templateUrl: './gio-timeframe-widget.component.html',
  styleUrl: './gio-timeframe-widget.component.scss',
})
export class GioTimeframeWidgetComponent implements OnInit {
  activeFilters = input.required<ApiAnalyticsProxyFilters>();
  filtersChange = output<ApiAnalyticsProxyFilters>();
  refresh = output<void>();

  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];

  form: FormGroup<GioTimeframeWidgetForm> = this.formBuilder.group(
    {
      period: [''],
      from: [null],
      to: [null],
    },
    { validators: this.dateRangeValidator },
  );
  minDate: Moment;
  nowDate: Moment = moment().add(1, 'd');
  customPeriod: string = 'custom';

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
  ) {
    effect(() => {
      const filters = this.activeFilters();
      this.updateFormFromFilters(filters);
    });
  }

  ngOnInit() {
    this.form.controls.period.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((period) => {
      const currentFilters = this.activeFilters();
      const updatedFilters = { ...currentFilters, period };
      if (period !== this.customPeriod) {
        this.filtersChange.emit(updatedFilters);
      }
    });

    this.form.controls.from.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((from) => {
      this.minDate = from;
      this.form.updateValueAndValidity();
    });

    this.form.controls.to.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.form.updateValueAndValidity();
    });
  }

  applyCustomTimeframe() {
    const from = this.form.controls.from.value.valueOf();
    const to = this.form.controls.to.value.valueOf();

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

  private dateRangeValidator(group: FormGroup): { [key: string]: any } | null {
    const from = group.get('from')?.value;
    const to = group.get('to')?.value;

    if (from && to && from.isAfter(to)) {
      return { dateRange: true };
    }

    return null;
  }

  private updateFormFromFilters(filters: ApiAnalyticsProxyFilters) {
    if (this.form) {
      this.form.patchValue({
        period: filters.period,
        from: filters.from ? moment(filters.from) : null,
        to: filters.to ? moment(filters.to) : null,
      });
    }
  }
}
