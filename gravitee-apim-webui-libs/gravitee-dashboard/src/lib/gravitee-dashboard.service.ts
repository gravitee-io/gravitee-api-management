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

import { GlobalRequest } from './components/widget/model/request/request';
import { FacetsResponse } from './components/widget/model/response/facets-response';
import { MeasuresResponse } from './components/widget/model/response/measures-response';
import { TimeSeriesResponse } from './components/widget/model/response/time-series-response';
import { RequestType, Widget } from './components/widget/model/widget/widget.model';

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
        description: 'Requests count',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 0,
        },
        request: {
          type: 'measures',
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
        description: 'Percentage of responses in error',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 1,
        },
        request: {
          type: 'measures',
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
        description: 'Average latency of the Gateway',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 2,
        },
        request: {
          type: 'measures',
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
        description: 'Average response time of the Gateway',
        type: 'stats',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 3,
        },
        request: {
          type: 'measures',
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
        title: 'HTTP Statuses',
        description: 'Number of HTTP requests per HTTP Status',
        type: 'doughnut',
        layout: {
          cols: 1,
          rows: 2,
          y: 1,
          x: 0,
        },
        request: {
          type: 'facets',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['COUNT'],
            },
          ],
        },
      },
      {
        id: '6',
        title: 'Response Time',
        description: 'Average response time of the Endpoint and Gateway',
        type: 'line',
        layout: {
          cols: 3,
          rows: 2,
          y: 1,
          x: 1,
        },
        request: {
          type: 'time-series',
          by: [],
          metrics: [
            {
              name: 'HTTP_ENDPOINT_RESPONSE_TIME',
              measures: ['AVG'],
            },
            {
              name: 'HTTP_GATEWAY_RESPONSE_TIME',
              measures: ['AVG'],
            },
          ],
        },
      },
      {
        id: '7',
        title: 'Response Statuses',
        description: 'Number of response statuses over time',
        type: 'bar',
        layout: {
          cols: 3,
          rows: 2,
          y: 3,
          x: 0,
        },
        request: {
          type: 'time-series',
          by: ['HTTP_STATUS_CODE_GROUP'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['COUNT'],
            },
          ],
        },
      },
      {
        id: '8',
        title: 'Top 5 Applications',
        description: 'Top 5 applications by number of HTTP requests',
        type: 'pie',
        layout: {
          cols: 1,
          rows: 2,
          y: 3,
          x: 3,
        },
        request: {
          type: 'facets',
          limit: 5,
          by: ['APPLICATION'],
          metrics: [
            {
              name: 'HTTP_REQUESTS',
              measures: ['COUNT'],
            },
          ],
        },
      },
    ];
  }

  public getMetrics(
    basePath: string,
    endpoint: RequestType,
    request: GlobalRequest<RequestType>,
  ): Observable<MeasuresResponse | FacetsResponse | TimeSeriesResponse> {
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
}
