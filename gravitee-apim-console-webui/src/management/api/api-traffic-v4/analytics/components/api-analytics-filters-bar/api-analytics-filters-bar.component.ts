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
import { Component, DestroyRef, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatOption } from '@angular/material/autocomplete';
import { MatSelect } from '@angular/material/select';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { FiltersApplied } from './api-analytics-filters-bar.configuration';

import { timeFrames, TimeRangeParams } from '../../../../../../shared/utils/timeFrameRanges';
import { ApiAnalyticsV2Service, DefaultFilters } from '../../../../../../services-ngx/api-analytics-v2.service';

@Component({
  selector: 'api-analytics-filters-bar',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    GioIconsModule,
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatOption,
    MatSelect,
  ],
  templateUrl: './api-analytics-filters-bar.component.html',
  styleUrl: './api-analytics-filters-bar.component.scss',
})
export class ApiAnalyticsFiltersBarComponent implements OnInit {
  private readonly defaultFilters = this.getDefaultFilters();
  protected readonly timeFrames = timeFrames;
  public formGroup: FormGroup;
  public activeFilters: FiltersApplied = this.defaultFilters;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
    private readonly apiAnalyticsV2Service: ApiAnalyticsV2Service,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit() {
    this.initForm();
  }

  private initForm() {
    this.formGroup = this.formBuilder.group(this.defaultFilters);
    this.formGroup.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      this.activeFilters = value;
      this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
    });
  }

  public refresh() {
    this.apiAnalyticsV2Service.setTimeRangeFilter(this.getPeriodTimeRangeParams());
  }

  private getPeriodTimeRangeParams(): TimeRangeParams {
    return timeFrames.find((timeFrame) => timeFrame.id === this.activeFilters.period)?.timeFrameRangesParams();
  }

  private getDefaultFilters(): DefaultFilters {
    const periodFromQueryParam = this.activatedRoute.snapshot.queryParams.period;
    const validPeriod = timeFrames.find((timeFrame) => timeFrame.id === periodFromQueryParam);

    if (periodFromQueryParam && validPeriod) {
      return { period: periodFromQueryParam };
    }

    this.router.navigate(['../analytics'], {
      relativeTo: this.activatedRoute,
    });
    return { period: '1d' };
  }
}
