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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import moment, { Moment } from 'moment/moment';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';

import { ApiAnalyticsMessageFilters } from './api-analytics-message-filters-bar.configuration';

import {
  timeFrames,
  customTimeFrames,
  TimeRangeParams,
  DATE_TIME_FORMATS,
  calculateCustomInterval,
} from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'api-analytics-message-filters-bar',
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
    OwlMomentDateTimeModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
  templateUrl: './api-analytics-message-filters-bar.component.html',
  styleUrl: './api-analytics-message-filters-bar.component.scss',
})
export class ApiAnalyticsMessageFiltersBarComponent implements OnInit, OnDestroy {
  protected readonly timeFrames = [...timeFrames, ...customTimeFrames];
  public form: FormGroup;
  public activeFilters: ApiAnalyticsMessageFilters;
  public minDate: Moment;
  public nowDate: Moment = moment().add(1, 'd');
  public customPeriod: string = 'custom';
  public periodFormValue$: Observable<string>;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    this.initActiveFilters();
    this.initForm();

    if (
      this.form.get('period').value === this.customPeriod &&
      this.form.valid &&
      this.form.get('from').value &&
      this.form.get('to').value
    ) {
      this.applyCustomTimeframe();
    }

    this.periodFormValue$ = this.form.get('period').valueChanges;
  }

  ngOnDestroy() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(null);
  }

  private initForm() {
    this.form = this.formBuilder.group({
      period: this.activeFilters.period,
      from: this.activeFilters.from ? moment(this.activeFilters.from) : null,
      to: this.activeFilters.to ? moment(this.activeFilters.to) : null,
    });

    this.form
      .get('period')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(period => {
        this.activeFilters = { ...this.activeFilters, period };
        if (period !== this.customPeriod) {
          this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
        }
      });

    this.form
      .get('from')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(from => {
        this.minDate = from;
      });
  }

  public applyCustomTimeframe() {
    const from = this.form.get('from').value.valueOf();
    const to = this.form.get('to').value.valueOf();

    const customTimeRange = {
      from,
      to,
      interval: calculateCustomInterval(from, to),
    };
    this.apiAnalyticsV2Service.setTimeRangeFilter(customTimeRange);
  }

  public refresh() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }

  private getPeriodTimeRangeParams(): TimeRangeParams {
    return timeFrames.find(timeFrame => timeFrame.id === this.activeFilters.period)?.timeFrameRangesParams();
  }

  private initActiveFilters() {
    const { period, from, to } = this.activatedRoute.snapshot.queryParams;
    const validPeriod = timeFrames.find(timeFrame => timeFrame.id === period);

    if (period === this.customPeriod) {
      this.activeFilters = { period, from: +from, to: +to };
      this.router.navigate([], {
        queryParams: {
          from: null,
          to: null,
          period: null,
        },
        queryParamsHandling: 'merge',
      });
      return;
    }

    if (validPeriod) {
      this.activeFilters = { period, from: null, to: null };
      this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());

      this.router.navigate([], {
        queryParams: {
          period: null,
        },
        queryParamsHandling: 'merge',
      });
      return;
    }

    this.activeFilters = this.apiAnalyticsV2Service.defaultFilters;
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }
}
