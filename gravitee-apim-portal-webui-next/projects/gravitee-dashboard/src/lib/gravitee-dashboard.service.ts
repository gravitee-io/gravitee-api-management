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
import { Injectable } from '@angular/core';

import { Widget } from './components/widget/widget';

@Injectable({
  providedIn: 'root',
})
export class GraviteeDashboardService {
  constructor() {}

  public getWidgets(): Widget[] {
    return [
      {
        id: '1',
        label: 'Requests',
        type: 'kpi',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 1,
        },
      },
      {
        id: '2',
        label: 'Error Rate',
        type: 'kpi',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 2,
        },
      },
      {
        id: '3',
        label: 'Average Latency',
        type: 'kpi',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 3,
        },
      },
      {
        id: 'kpi',
        label: 'Subscriptions',
        type: 'doughnut',
        layout: {
          cols: 1,
          rows: 1,
          y: 0,
          x: 4,
        },
      },
      {
        id: '5',
        label: 'HTTP Statuses',
        type: 'doughnut',
        filter: 'status-code',
        layout: {
          cols: 1,
          rows: 2,
          y: 1,
          x: 1,
        },
      },
      {
        id: '6',
        label: 'Response Time',
        type: 'pie',
        layout: {
          cols: 3,
          rows: 2,
          y: 1,
          x: 2,
        },
      },
      {
        id: '7',
        label: 'Response Statuses',
        type: 'doughnut',
        filter: 'status-over-time',
        layout: {
          cols: 3,
          rows: 2,
          y: 3,
          x: 1,
        },
      },
      {
        id: '8',
        label: 'Consumption by Application',
        type: 'polarArea',
        layout: {
          cols: 1,
          rows: 2,
          y: 3,
          x: 4,
        },
      },
      {
        id: '9',
        label: 'Top Application',
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
        label: 'Top API',
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
}
