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
import { Component, inject } from '@angular/core';
import { GioCardEmptyStateModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { BehaviorSubject, combineLatest, Observable, of, switchMap } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { map, startWith } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { flatten, get, toNumber } from 'lodash';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import {
  AnalyticsRequestStats,
  ApiAnalyticsRequestStatsComponent,
} from '../components/api-analytics-requests-stats/api-analytics-request-stats.component';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../../../util/apiFilter.operator';
import { AnalyticsRequestsCount } from '../../../../../entities/management-api-v2/analytics/analyticsRequestsCount';
import { ApiAnalyticsV2Service } from '../../../../../services-ngx/api-analytics-v2.service';
import { AnalyticsAverageConnectionDuration } from '../../../../../entities/management-api-v2/analytics/analyticsAverageConnectionDuration';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';
import { IconService } from '../../../../../services-ngx/icon.service';
import { ApiAnalyticsFiltersBarComponent } from '../components/api-analytics-filters-bar/api-analytics-filters-bar.component';
import { AnalyticsAverageMessagesPerRequest } from '../../../../../entities/management-api-v2/analytics/analyticsAverageMessagesPerRequest';
import {
  ApiAnalyticsResponseStatusRanges,
  ApiAnalyticsResponseStatusRangesComponent,
} from '../../../../../shared/components/api-analytics-response-status-ranges/api-analytics-response-status-ranges.component';
import { AnalyticsResponseStatusRanges } from '../../../../../entities/management-api-v2/analytics/analyticsResponseStatusRanges';

type ApiAnalyticsVM = {
  isLoading: boolean;
  isAnalyticsEnabled?: boolean;
  globalRequestStats?: AnalyticsRequestStats;
  globalResponseStatusRanges?: ApiAnalyticsResponseStatusRanges;
  entrypoints?: {
    id: string;
    name: string;
    icon: string;
    requestStats?: AnalyticsRequestStats;
    isNotConfigured?: boolean;
    responseStatusRanges?: ApiAnalyticsResponseStatusRanges;
  }[];
};

@Component({
  selector: 'api-analytics-message',
  standalone: true,
  imports: [
    CommonModule,
    MatButton,
    MatCardModule,
    GioLoaderModule,
    GioCardEmptyStateModule,
    ApiAnalyticsRequestStatsComponent,
    MatIcon,
    ApiAnalyticsFiltersBarComponent,
    MatTooltip,
    ApiAnalyticsResponseStatusRangesComponent,
  ],
  templateUrl: './api-analytics-message.component.html',
  styleUrl: './api-analytics-message.component.scss',
})
export class ApiAnalyticsMessageComponent {
  private readonly apiService = inject(ApiV2Service);
  private readonly apiAnalyticsService = inject(ApiAnalyticsV2Service);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly connectorPluginsV2Service = inject(ConnectorPluginsV2Service);
  private readonly iconService = inject(IconService);

  private getRequestsCount$: Observable<Partial<AnalyticsRequestsCount> & { isLoading: boolean }> = this.apiAnalyticsService
    .getRequestsCount(this.activatedRoute.snapshot.params.apiId)
    .pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getAverageConnectionDuration$: Observable<Partial<AnalyticsAverageConnectionDuration> & { isLoading: boolean }> =
    this.apiAnalyticsService.getAverageConnectionDuration(this.activatedRoute.snapshot.params.apiId).pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getAverageMessagesPerRequest$: Observable<Partial<AnalyticsAverageMessagesPerRequest> & { isLoading: boolean }> =
    this.apiAnalyticsService.getAverageMessagesPerRequest(this.activatedRoute.snapshot.params.apiId).pipe(
      map((requestsCount) => ({ isLoading: false, ...requestsCount })),
      startWith({ isLoading: true }),
    );

  private getResponseStatusRanges$: Observable<Partial<AnalyticsResponseStatusRanges> & { isLoading: boolean }> = this.apiAnalyticsService
    .getResponseStatusRanges(this.activatedRoute.snapshot.params.apiId)
    .pipe(
      map((responseStatusRanges) => ({ isLoading: false, ...responseStatusRanges })),
      startWith({ isLoading: true }),
    );

  filters$ = new BehaviorSubject<void>(undefined);

  apiAnalyticsVM$: Observable<ApiAnalyticsVM> = combineLatest([
    this.apiService.getLastApiFetch(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV4Filter()),
    this.connectorPluginsV2Service.listAsyncEntrypointPlugins(),
    this.filters$,
  ]).pipe(
    switchMap(([api, availableEntrypoints]) => {
      if (api.analytics.enabled) {
        const apiEntrypointsId = flatten(api.listeners.map((l) => l.entrypoints)).map((e) => e.type);
        const allEntrypoints: ApiAnalyticsVM['entrypoints'] = availableEntrypoints.map((e) => ({
          id: e.id,
          name: e.name,
          icon: this.iconService.registerSvg(e.id, e.icon),
        }));

        return this.analyticsData$(allEntrypoints, apiEntrypointsId).pipe(
          map((analyticsData) => ({ isAnalyticsEnabled: true, ...analyticsData })),
        );
      }
      return of({ isAnalyticsEnabled: false });
    }),
    map((analyticsData) => ({ isLoading: false, ...analyticsData })),
    startWith({ isLoading: true }),
  );

  private analyticsData$(
    allEntrypoints: ApiAnalyticsVM['entrypoints'],
    apiEntrypointsId: string[],
  ): Observable<Pick<ApiAnalyticsVM, 'globalRequestStats' | 'entrypoints'>> {
    return combineLatest([
      this.getRequestsCount$,
      this.getAverageConnectionDuration$,
      this.getAverageMessagesPerRequest$,
      this.getResponseStatusRanges$,
    ]).pipe(
      map(([requestsCount, averageConnectionDuration, averageMessagesPerRequest, responseStatusRanges]) => {
        // Entrypoints that are configured in the API
        const apiEntrypoints = allEntrypoints.filter((entrypoint) => apiEntrypointsId.includes(entrypoint.id));

        // Entrypoints that are not configured in the API
        const notApiConfiguredEntrypoints = allEntrypoints
          .filter((entrypoint) => !apiEntrypointsId.includes(entrypoint.id))
          .filter((entrypoint) =>
            [
              ...Object.keys(!requestsCount.isLoading ? requestsCount.countsByEntrypoint ?? {} : {}),
              ...Object.keys(!averageConnectionDuration.isLoading ? averageConnectionDuration.averagesByEntrypoint ?? {} : {}),
              ...Object.keys(!averageMessagesPerRequest.isLoading ? averageMessagesPerRequest.averagesByEntrypoint ?? {} : {}),
            ].includes(entrypoint.id),
          )
          .map((entrypoint) => ({ ...entrypoint, isNotConfigured: true }));

        return {
          entrypoints: [...apiEntrypoints, ...notApiConfiguredEntrypoints].map((entrypoint) => {
            return {
              ...entrypoint,
              requestStats: [
                {
                  label: 'Total Requests',
                  value: get(requestsCount, `countsByEntrypoint.${entrypoint.id}`),
                  isLoading: requestsCount.isLoading,
                },
                {
                  label: 'Average Messages Per Request',
                  value: get(averageMessagesPerRequest, `averagesByEntrypoint.${entrypoint.id}`),
                  isLoading: averageMessagesPerRequest.isLoading,
                },
                {
                  label: 'Average Connection Duration',
                  unitLabel: 'ms',
                  value: get(averageConnectionDuration, `averagesByEntrypoint.${entrypoint.id}`),
                  isLoading: averageConnectionDuration.isLoading,
                },
              ],
              responseStatusRanges: {
                isLoading: responseStatusRanges.isLoading,
                data: Object.entries(get(responseStatusRanges, `rangesByEntrypoint.${entrypoint.id}`, {})).map(([label, value]) => ({
                  label,
                  value: toNumber(value),
                })),
              },
            };
          }),
          globalRequestStats: [
            {
              label: 'Total Requests',
              value: requestsCount.total,
              isLoading: requestsCount.isLoading,
            },
            {
              label: 'Average Messages Per Request',
              value: averageMessagesPerRequest.average,
              isLoading: averageMessagesPerRequest.isLoading,
            },
            {
              label: 'Average Connection Duration',
              unitLabel: 'ms',
              value: averageConnectionDuration.average,
              isLoading: averageConnectionDuration.isLoading,
            },
          ],
          globalResponseStatusRanges: {
            isLoading: responseStatusRanges.isLoading,
            data: Object.entries(responseStatusRanges.ranges ?? {}).map(([label, value]) => ({ label, value: toNumber(value) })),
          },
        };
      }),
    );
  }
}
