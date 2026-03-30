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
import { MatOption, MatSelect } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import {
  ApiAnalyticsFilters,
  v4AnalyticsTimeFrames,
} from './api-analytics-filters-bar.configuration';

import { TimeRangeParams } from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'api-analytics-filters-bar',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatOption,
    MatSelect,
  ],
  templateUrl: './api-analytics-filters-bar.component.html',
  styleUrl: './api-analytics-filters-bar.component.scss',
})
export class ApiAnalyticsFiltersBarComponent implements OnInit, OnDestroy {
  protected readonly timeFrames = v4AnalyticsTimeFrames;
  public form: FormGroup;
  public activeFilters: ApiAnalyticsFilters;

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
    });

    this.form
      .get('period')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((period) => {
        this.activeFilters = { ...this.activeFilters, period };
        this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
      });
  }

  public refresh() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }

  private getPeriodTimeRangeParams(): TimeRangeParams {
    return (
      v4AnalyticsTimeFrames.find((timeFrame) => timeFrame.id === this.activeFilters.period)?.timeFrameRangesParams() ??
      v4AnalyticsTimeFrames.find((timeFrame) => timeFrame.id === this.apiAnalyticsV2Service.defaultFilters.period)
        .timeFrameRangesParams()
    );
  }

  private initActiveFilters() {
    const { period } = this.activatedRoute.snapshot.queryParams;
    const validPeriod = v4AnalyticsTimeFrames.find((timeFrame) => timeFrame.id === period);

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
