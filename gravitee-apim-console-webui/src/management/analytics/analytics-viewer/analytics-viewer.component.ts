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
import { GridComponent, WidgetComponent } from '@gravitee/gravitee-dashboard';

import { Component } from '@angular/core';
import { Widget } from '@gravitee/gravitee-dashboard/lib/components/widget/widget';

@Component({
  selector: 'analytics-viewer',
  imports: [GridComponent, WidgetComponent],
  templateUrl: './analytics-viewer.component.html',
  styleUrl: './analytics-viewer.component.scss',
})
export class AnalyticsViewerComponent {
  widgets: Widget[];

  constructor() {
    this.widgets = this.getWidgets();
  }

  public getWidgets(): Widget[] {
    return [
      {
        id: 'min-response-time-01',
        label: 'Min Response Time',
        type: 'metric',
        layout: {
          cols: 1,
          rows: 3,
          y: 0,
          x: 2,
        },
      },
      {
        id: 'avg-response-time-02',
        label: 'Avg Response Time',
        type: 'metric',
        layout: {
          cols: 1,
          rows: 3,
          y: 0,
          x: 3,
        },
      },
      {
        id: 'requests-per-second-03',
        label: 'Requests Per Second',
        type: 'metric',
        layout: {
          cols: 1,
          rows: 3,
          y: 0,
          x: 4,
        },
      },
      {
        id: 'http-status-repartition-04',
        label: 'HTTP Status Repartition',
        type: 'chart',
        filter: 'status-code',
        layout: {
          cols: 4,
          rows: 4,
          y: 1,
          x: 0,
        },
      },
      {
        id: 'top-applications-05',
        label: 'Top Applications',
        type: 'table',
        layout: {
          cols: 2,
          rows: 4,
          y: 1,
          x: 4,
        },
      },
      {
        id: 'response-status-over-time-06',
        label: 'Response Status Over Time',
        type: 'chart',
        filter: 'status-over-time',
        layout: {
          cols: 4,
          rows: 4,
          y: 2,
          x: 0,
        },
      },
      {
        id: 'top-api-plans-07',
        label: 'Top API Plans',
        type: 'table',
        layout: {
          cols: 2,
          rows: 4,
          y: 2,
          x: 4,
        },
      },
      {
        id: 'empty-widget-08',
        label: 'Custom Widget',
        type: 'custom',
        layout: {
          cols: 3,
          rows: 4,
          y: 3,
          x: 0,
        },
      },
      {
        id: 'empty-widget-09',
        label: 'Custom Widget',
        type: 'custom',
        layout: {
          cols: 3,
          rows: 4,
          y: 3,
          x: 3,
        },
      },
    ];
  }
}
