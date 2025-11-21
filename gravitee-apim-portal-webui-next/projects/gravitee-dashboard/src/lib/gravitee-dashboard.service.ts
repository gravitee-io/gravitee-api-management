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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { GlobalRequest } from './components/widget/model/request/request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { TimeSeriesResponse } from './components/widget/model/response/time-series-response';
import { RequestType, Widget } from './components/widget/model/widget/widget';

@Injectable({
  providedIn: 'root',
})
export class GraviteeDashboardService {
  analyticsPath = 'analytics';
  constructor(private readonly http: HttpClient) {}

  public getWidgets(): Widget[] {
    return [
      {
        id: '1',
        title: 'Requests',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 0,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['COUNT'],
            },
          ],
        },
      },
      {
        id: '2',
        title: 'Error Rate',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 1,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          metrics: [
            {
              name: 'HTTP_ERRORS',
              measures: ['PERCENTAGE'],
            },
          ],
        },
      },
      {
        id: '3',
        title: 'Average Latency',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 2,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          metrics: [
            {
              name: 'HTTP_GATEWAY_LATENCY',
              measures: ['AVG'],
            },
          ],
        },
      },
      {
        id: '4',
        title: 'Average Response Time',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 3,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          metrics: [
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: ['AVG'],
            },
          ],
        },
      },
      {
        id: '5',
        title: 'HTTP Statuses (mock)',
        type: 'doughnut',
        layout: {
          cols: 1,
          rows: 2,
          y: 1,
          x: 0,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          by: ['API', 'APPLICATION'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['RPS'],
              filters: [
                {
                  name: 'TENANT',
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '6',
        title: 'Response Time (mock)',
        type: 'line',
        layout: {
          cols: 3,
          rows: 2,
          y: 1,
          x: 1,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          by: ['API', 'APPLICATION'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['RPS'],
              filters: [
                {
                  name: 'TENANT',
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '7',
        title: 'Response Statues (mock)',
        type: 'bar',
        layout: {
          cols: 3,
          rows: 2,
          y: 3,
          x: 0,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          by: ['API', 'APPLICATION'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['RPS'],
              filters: [
                {
                  name: 'TENANT',
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '8',
        title: 'Requests Split by Application (mock)',
        type: 'polarArea',
        layout: {
          cols: 1,
          rows: 2,
          y: 3,
          x: 3,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-11-07T06:50:30Z',
            to: '2025-12-07T11:35:30Z',
          },
          by: ['API', 'APPLICATION'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['RPS'],
              filters: [
                {
                  name: 'TENANT',
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
    ];
  }

  public getMetrics(basePath: string, endpoint: RequestType, request: GlobalRequest<RequestType>) {
    if (endpoint === 'measures') {
      return this.http.post<MeasuresResponse>(`${basePath}/${this.analyticsPath}/${endpoint}`, request);
    }
    if (endpoint === 'facets') {
      return this.http.post<FacetsResponse>(`${basePath}/${this.analyticsPath}/${endpoint}`, request);
    }
    if (endpoint === 'time-series') {
      return this.http.post<TimeSeriesResponse>(`${basePath}/${this.analyticsPath}/${endpoint}`, request);
    }

    throw new Error(`Endpoint ${endpoint} not supported`);
  }

  public getMetricsMock(
    basePath: string,
    endpoint: string,
    _request: GlobalRequest<RequestType>,
  ): Observable<MeasuresResponse | TimeSeriesResponse | FacetsResponse> {
    switch (endpoint) {
      case 'measures':
        return this.http.post<MeasuresResponse>(`${basePath}/${this.analyticsPath}/${endpoint}`, _request);
      case 'facets':
        return of(this.getFacets());
      default:
        throw new Error(`Endpoint ${endpoint} not supported`);
    }
  }

  private getFacets(): FacetsResponse {
    return {
      type: 'facets',
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          buckets: [
            {
              key: '1XX',
              measures: [
                {
                  name: 'COUNT',
                  value: 253,
                },
              ],
            },
            {
              key: '2XX',
              measures: [
                {
                  name: 'COUNT',
                  value: 1234,
                },
              ],
            },
            {
              key: '3XX',
              measures: [
                {
                  name: 'COUNT',
                  value: 120,
                },
              ],
            },
            {
              key: '4XX',
              measures: [
                {
                  name: 'COUNT',
                  value: 590,
                },
              ],
            },
            {
              key: '5XX',
              measures: [
                {
                  name: 'COUNT',
                  value: 250,
                },
              ],
            },
          ],
        },
      ],
    };
  }
}
