/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';

import {
  BasicTimeframe,
  Dashboard,
  GraviteeDashboardComponent,
  TimeframeSelectorComponent,
  TimeframeValue,
  timeFrameRangesParams,
  timeFrames,
  TimeRange,
} from '@gravitee/gravitee-dashboard';

import { BannerComponent } from '../../../../components/banner/banner.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { AnalyticsDashboardService } from '../../../../services/analytics-dashboard.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { ConfigService } from '../../../../services/config.service';
import { analyticsListBreadcrumb } from '../analytics-breadcrumbs';

const DEFAULT_PERIOD: BasicTimeframe = '5m';

@Component({
  selector: 'app-analytics-details',
  imports: [GraviteeDashboardComponent, LoaderComponent, BannerComponent, TimeframeSelectorComponent, ReactiveFormsModule],
  templateUrl: './analytics-details.component.html',
  styleUrl: './analytics-details.component.scss',
  providers: [{ provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } }],
})
export default class AnalyticsDetailsComponent {
  private readonly configService = inject(ConfigService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly analyticsDashboardService = inject(AnalyticsDashboardService);

  readonly dashboardId = input.required<string>();

  readonly baseURL = this.configService.baseURL;
  protected readonly defaultPeriod = DEFAULT_PERIOD;
  protected readonly timeFrames = timeFrames;
  protected readonly periodControl = new FormControl<TimeframeValue>(
    { period: DEFAULT_PERIOD, from: null, to: null },
    { nonNullable: true },
  );
  private readonly refreshTokenSignal = signal(0);
  readonly refreshToken = this.refreshTokenSignal.asReadonly();

  private readonly period = toSignal(this.periodControl.valueChanges, { initialValue: this.periodControl.value });

  readonly dashboardResource = rxResource<Dashboard | undefined, string>({
    params: () => this.dashboardId(),
    stream: ({ params }) => this.analyticsDashboardService.getById(params),
  });
  readonly dashboard = computed(() => (this.dashboardResource.error() ? undefined : this.dashboardResource.value()));
  readonly dashboardName = computed(() => this.dashboard()?.name ?? '');

  private readonly timeframeParams = computed(() => {
    const id = this.period().period;
    const known = timeFrames.some(timeFrame => timeFrame.id === id);
    return timeFrameRangesParams(known ? (id as BasicTimeframe) : DEFAULT_PERIOD);
  });
  readonly timeRange = computed<TimeRange>(() => {
    const params = this.timeframeParams();
    return { from: new Date(params.from).toISOString(), to: new Date(params.to).toISOString() };
  });
  readonly interval = computed(() => this.timeframeParams().interval);

  constructor() {
    effect(() => {
      const id = this.dashboardId();
      const name = this.dashboardName();
      this.breadcrumbService.set([analyticsListBreadcrumb(true), { id: `analytics-${id}`, label: name || id }]);
    });
  }

  refresh(): void {
    this.refreshTokenSignal.update(token => token + 1);
  }
}
