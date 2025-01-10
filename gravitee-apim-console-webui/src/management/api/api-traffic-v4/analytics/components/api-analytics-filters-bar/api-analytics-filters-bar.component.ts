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
import { Component, DestroyRef, OnDestroy, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatInputModule } from '@angular/material/input';
import { OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import moment, { Moment } from 'moment/moment';

import { FiltersApplied } from './api-analytics-filters-bar.configuration';

import { timeFrames, customTimeFrames, TimeRangeParams } from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'api-analytics-filters-bar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOption,
    MatSelect,
    MatInputModule,
    OwlDateTimeModule,
  ],
  templateUrl: './api-analytics-filters-bar.component.html',
  styleUrl: './api-analytics-filters-bar.component.scss',
})
export class ApiAnalyticsFiltersBarComponent implements OnInit, OnDestroy {
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  public form: FormGroup;
  public activeFilters: FiltersApplied;
  public minDate: Moment;
  public nowDate: Moment = moment().add(1, 'd');

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.initActiveFilters();
    this.initForm();
  }

  ngOnDestroy() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(null);
  }

  get periodFormValue() {
    return this.form.get('period') as FormControl;
  }

  private initForm() {
    this.form = this.formBuilder.group({
      period: this.activeFilters.period,
      from: null,
      to: null,
    });

    this.form
      .get('period')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((period) => {
        this.activeFilters = { ...this.activeFilters, period };
        if (period !== 'custom') {
          this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
        }
      });

    this.form
      .get('from')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((from) => {
        this.minDate = from;
      });
  }

  public applyCustomTimeframe() {
    const customTimeRange = {
      from: this.form.get('from').value.valueOf(),
      to: this.form.get('to').value.valueOf(),
    };
    this.apiAnalyticsV2Service.setTimeRangeFilter(customTimeRange);
  }

  public refresh() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }

  private getPeriodTimeRangeParams(): TimeRangeParams {
    return timeFrames.find((timeFrame) => timeFrame.id === this.activeFilters.period)?.timeFrameRangesParams();
  }

  private initActiveFilters() {
    const periodFromQueryParam = this.activatedRoute.snapshot.queryParams.period;
    const validPeriod = timeFrames.find((timeFrame) => timeFrame.id === periodFromQueryParam);

    if (validPeriod) {
      this.activeFilters = { period: periodFromQueryParam };
      this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
      return;
    }

    this.activeFilters = this.apiAnalyticsV2Service.defaultFilters;
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }
}
