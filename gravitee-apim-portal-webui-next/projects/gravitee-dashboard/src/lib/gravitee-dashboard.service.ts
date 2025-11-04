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

import { FacetName } from './components/widget/model/request/enum/facet-name';
import { FilterName } from './components/widget/model/request/enum/filter-name';
import { MeasureName } from './components/widget/model/request/enum/measure-name';
import { MetricName } from './components/widget/model/request/enum/metric-name';
import { GlobalRequest } from './components/widget/model/request/request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { TimeSeriesResponse } from './components/widget/model/response/time-series-response';
import { RequestType, Widget } from './components/widget/model/widget/widget';

@Injectable({
  providedIn: 'root',
})
export class GraviteeDashboardService {
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
          x: 1,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          filters: [
            {
              name: FilterName.API,
              operator: 'EQ',
              value: '8528bd43-c264-4719-8d9e-ad8afb34ce71',
            },
          ],
          metrics: [
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: [MeasureName.P90, MeasureName.P95, MeasureName.P99],
              filters: [
                {
                  name: FilterName.TENANT,
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
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
          x: 2,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          filters: [
            {
              name: FilterName.API,
              operator: 'EQ',
              value: '8528bd43-c264-4719-8d9e-ad8afb34ce71',
            },
          ],
          metrics: [
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: [MeasureName.P90, MeasureName.P95, MeasureName.P99],
              filters: [
                {
                  name: FilterName.TENANT,
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
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
          x: 3,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          filters: [
            {
              name: FilterName.API,
              operator: 'EQ',
              value: '8528bd43-c264-4719-8d9e-ad8afb34ce71',
            },
          ],
          metrics: [
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: [MeasureName.P90, MeasureName.P95, MeasureName.P99],
              filters: [
                {
                  name: FilterName.TENANT,
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '4',
        title: 'Subscriptions',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 4,
        },
        request: {
          type: 'measures',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          filters: [
            {
              name: FilterName.API,
              operator: 'EQ',
              value: '8528bd43-c264-4719-8d9e-ad8afb34ce71',
            },
          ],
          metrics: [
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: [MeasureName.P90, MeasureName.P95, MeasureName.P99],
              filters: [
                {
                  name: FilterName.TENANT,
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '5',
        title: 'HTTP Statuses',
        type: 'doughnut',
        layout: {
          cols: 1,
          rows: 2,
          y: 1,
          x: 1,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: [FacetName.API, FacetName.APPLICATION],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: [MeasureName.RPS],
              filters: [
                {
                  name: FilterName.TENANT,
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
        title: 'Response Time',
        type: 'pie',
        layout: {
          cols: 3,
          rows: 2,
          y: 1,
          x: 2,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: [FacetName.API, FacetName.APPLICATION],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: [MeasureName.RPS],
              filters: [
                {
                  name: FilterName.TENANT,
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
        title: 'Response Statuses',
        type: 'doughnut',
        layout: {
          cols: 3,
          rows: 2,
          y: 3,
          x: 1,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: [FacetName.API, FacetName.APPLICATION],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: [MeasureName.RPS],
              filters: [
                {
                  name: FilterName.TENANT,
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
        title: 'Consumption by Application',
        type: 'polarArea',
        layout: {
          cols: 1,
          rows: 2,
          y: 3,
          x: 4,
        },
        request: {
          type: 'facets',
          timeRange: {
            from: '2025-01-01T00:00:00Z',
            to: '2025-01-31T23:59:59Z',
          },
          by: [FacetName.API, FacetName.APPLICATION],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: [MeasureName.RPS],
              filters: [
                {
                  name: FilterName.TENANT,
                  operator: 'EQ',
                  value: 'europe',
                },
              ],
            },
          ],
        },
      },
      {
        id: '9',
        title: 'Top Application',
        type: 'top',
        layout: {
          cols: 1,
          rows: 3,
          y: 1,
          x: 5,
        },
      },
      {
        id: '10',
        title: 'Top API',
        type: 'top',
        layout: {
          cols: 1,
          rows: 3,
          y: 2,
          x: 0,
        },
      },
    ];
  }

  public getMetrics(basePath: string, endpoint: RequestType, request: GlobalRequest<RequestType>) {
    if (endpoint === 'measures') {
      return this.http.post<MeasuresResponse>(`${basePath}/${endpoint}`, request);
    }
    if (endpoint === 'facets') {
      return this.http.post<FacetsResponse>(`${basePath}/${endpoint}`, request);
    }
    if (endpoint === 'time-series') {
      return this.http.post<TimeSeriesResponse>(`${basePath}/${endpoint}`, request);
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
        return of(this.getMeasures());
      case 'facets':
        return of(this.getFacets());
      default:
        throw new Error(`Endpoint ${endpoint} not supported`);
    }
  }

  private getMeasures(): MeasuresResponse {
    return { type: 'measures', metrics: [{ name: MetricName.HTTP_REQUESTS, measures: [{ name: MeasureName.AVG, value: 133 }] }] };
  }

  private getFacets(): FacetsResponse {
    return {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_REQUESTS,
          buckets: [
            {
              key: '8528bd43-c264-4719-8d9e-ad8afb34ce71',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 1234,
                },
              ],
            },
            {
              key: '8528bd43-c264-4719-8d9e-ad8afb34ce71-test',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 989,
                },
              ],
            },
            {
              key: '8528bd43-c264-4719-8d9e-ad8afb34ce71-test2',
              measures: [
                {
                  name: MeasureName.COUNT,
                  value: 590,
                },
              ],
            },
          ],
        },
      ],
    };
  }
}
