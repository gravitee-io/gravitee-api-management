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
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButton } from '@angular/material/button';

import { timeFrames, TimeRangeParams } from '../../../../../shared/utils/timeFrameRanges';
import { ApiHealthV2Service } from '../../../../../services-ngx/api-health-v2.service';

export interface ActiveFilter {
  timeframe: string;
}

@Component({
  selector: 'api-health-check-dashboard-v4-filters',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatSelectModule, MatButton],
  templateUrl: './api-health-check-dashboard-v4-filters.component.html',
  styleUrl: './api-health-check-dashboard-v4-filters.component.scss',
})
export class ApiHealthCheckDashboardV4FiltersComponent implements OnInit {
  protected readonly timeFrames = timeFrames;
  private defaultFilter = this.apiHealthCheckDashboardV4Service.defaultFilter;
  private activeFilter: ActiveFilter = this.defaultFilter;
  public form: FormGroup;

  constructor(
    private readonly destroyRef: DestroyRef,
    private readonly formBuilder: FormBuilder,
    private readonly apiHealthCheckDashboardV4Service: ApiHealthV2Service,
  ) {}

  ngOnInit() {
    this.initForm();
  }

  initForm() {
    this.form = this.formBuilder.group(this.defaultFilter);
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((value) => {
      this.activeFilter = value;
      this.apiHealthCheckDashboardV4Service.setActiveFilter(this.mapToTimeFrameRangesParams());
    });
  }

  refresh() {
    this.apiHealthCheckDashboardV4Service.setActiveFilter(this.mapToTimeFrameRangesParams());
  }

  mapToTimeFrameRangesParams(): TimeRangeParams {
    return timeFrames.find((timeFrame) => timeFrame.id === this.activeFilter.timeframe)?.timeFrameRangesParams();
  }
}
