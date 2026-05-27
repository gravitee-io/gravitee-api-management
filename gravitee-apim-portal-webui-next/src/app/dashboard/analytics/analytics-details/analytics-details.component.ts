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
import { Component, computed, effect, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { Dashboard, GraviteeDashboardComponent, timeFrameRangesParams, TimeRange } from '@gravitee/gravitee-dashboard';

import { BannerComponent } from '../../../../components/banner/banner.component';
import { LoaderComponent } from '../../../../components/loader/loader.component';
import { AnalyticsDashboardService } from '../../../../services/analytics-dashboard.service';
import { BreadcrumbService } from '../../../../services/breadcrumb.service';
import { ConfigService } from '../../../../services/config.service';
import { analyticsListBreadcrumb } from '../analytics-breadcrumbs';

@Component({
  selector: 'app-analytics-details',
  imports: [GraviteeDashboardComponent, LoaderComponent, BannerComponent],
  templateUrl: './analytics-details.component.html',
  styleUrl: './analytics-details.component.scss',
})
export default class AnalyticsDetailsComponent {
  private readonly configService = inject(ConfigService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly analyticsDashboardService = inject(AnalyticsDashboardService);

  readonly dashboardId = input.required<string>();

  readonly baseURL = this.configService.baseURL;

  readonly dashboardResource = rxResource<Dashboard | undefined, string>({
    params: () => this.dashboardId(),
    stream: ({ params }) => this.analyticsDashboardService.getById(params),
  });
  readonly dashboard = computed(() => (this.dashboardResource.error() ? undefined : this.dashboardResource.value()));
  readonly dashboardName = computed(() => this.dashboard()?.name ?? '');
  readonly timeRange: TimeRange = (() => {
    const params = timeFrameRangesParams('5m');
    return { from: new Date(params.from).toISOString(), to: new Date(params.to).toISOString() };
  })();
  readonly interval = timeFrameRangesParams('5m').interval;

  constructor() {
    effect(() => {
      const id = this.dashboardId();
      const name = this.dashboardName();
      this.breadcrumbService.set([analyticsListBreadcrumb(true), { id: `analytics-${id}`, label: name || id }]);
    });
  }
}
