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
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { GridComponent } from './grid.component';
import { MeasureName } from '../widget/model/request/enum/measure-name';
import { MetricName } from '../widget/model/request/enum/metric-name';
import { FacetsResponse } from '../widget/model/response/facets-response';
import { MeasuresResponse } from '../widget/model/response/measures-response';
import { Widget } from '../widget/model/widget/widget';

interface GridStoryArgs {
  items: Widget[];
}

// Common widget items to avoid duplication
const commonItems: Widget[] = [
  {
    id: 'widget-1',
    title: 'API Calls',
    type: 'pie',
    layout: { cols: 2, rows: 1, x: 0, y: 0 },
    response: {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_REQUESTS,
          buckets: [
            { key: 'North America', measures: [{ name: MeasureName.COUNT, value: 35 }] },
            { key: 'Europe', measures: [{ name: MeasureName.COUNT, value: 28 }] },
            { key: 'Asia Pacific', measures: [{ name: MeasureName.COUNT, value: 20 }] },
            { key: 'South America', measures: [{ name: MeasureName.COUNT, value: 8 }] },
            { key: 'Africa', measures: [{ name: MeasureName.COUNT, value: 5 }] },
            { key: 'Middle East', measures: [{ name: MeasureName.COUNT, value: 3 }] },
            { key: 'Oceania', measures: [{ name: MeasureName.COUNT, value: 1 }] },
          ],
        },
      ],
    } satisfies FacetsResponse,
  },
  {
    id: 'widget-2',
    title: 'Database Queries',
    type: 'doughnut',
    layout: { cols: 1, rows: 1, x: 2, y: 0 },
    response: {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_ERRORS,
          buckets: [
            { key: 'SELECT', measures: [{ name: MeasureName.COUNT, value: 45 }] },
            { key: 'INSERT', measures: [{ name: MeasureName.COUNT, value: 20 }] },
            { key: 'UPDATE', measures: [{ name: MeasureName.COUNT, value: 15 }] },
            { key: 'DELETE', measures: [{ name: MeasureName.COUNT, value: 5 }] },
          ],
        },
      ],
    } satisfies FacetsResponse,
  },
  {
    id: 'widget-3',
    title: 'Cache Hits',
    type: 'stats',
    layout: { cols: 1, rows: 1, x: 3, y: 0 },
    response: {
      type: 'measures',
      metrics: [
        {
          name: MetricName.HTTP_GATEWAY_LATENCY,
          measures: [
            { name: MeasureName.COUNT, value: 1234 },
            { name: MeasureName.AVG, value: 45.6 },
            { name: MeasureName.MAX, value: 1200 },
          ],
        },
      ],
    } satisfies MeasuresResponse,
  },
  {
    id: 'widget-4',
    title: 'Error Rate',
    type: 'pie',
    layout: { cols: 1, rows: 3, x: 0, y: 1 },
    response: {
      type: 'facets',
      metrics: [
        {
          name: MetricName.MESSAGE_ERRORS,
          buckets: [
            { key: '4xx', measures: [{ name: MeasureName.COUNT, value: 12 }] },
            { key: '5xx', measures: [{ name: MeasureName.COUNT, value: 3 }] },
            { key: 'Success', measures: [{ name: MeasureName.COUNT, value: 985 }] },
          ],
        },
      ],
    } satisfies FacetsResponse,
  },
  {
    id: 'widget-5',
    title: 'Response Time',
    type: 'doughnut',
    layout: { cols: 3, rows: 3, x: 1, y: 1 },
    response: {
      type: 'facets',
      metrics: [
        {
          name: MetricName.HTTP_ENDPOINT_RESPONSE_TIME,
          buckets: [
            { key: '< 100ms', measures: [{ name: MeasureName.COUNT, value: 600 }] },
            { key: '100-500ms', measures: [{ name: MeasureName.COUNT, value: 300 }] },
            { key: '500ms-1s', measures: [{ name: MeasureName.COUNT, value: 80 }] },
            { key: '> 1s', measures: [{ name: MeasureName.COUNT, value: 20 }] },
          ],
        },
      ],
    } satisfies FacetsResponse,
  },
  {
    id: 'widget-6',
    title: 'User Activity',
    type: 'polarArea',
    layout: { cols: 2, rows: 2, x: 4, y: 1 },
    response: {
      type: 'facets',
      metrics: [
        {
          name: MetricName.APIS,
          buckets: [
            { key: 'Active', measures: [{ name: MeasureName.COUNT, value: 150 }] },
            { key: 'Inactive', measures: [{ name: MeasureName.COUNT, value: 50 }] },
            { key: 'New', measures: [{ name: MeasureName.COUNT, value: 30 }] },
            { key: 'Returning', measures: [{ name: MeasureName.COUNT, value: 120 }] },
          ],
        },
      ],
    } satisfies FacetsResponse,
  },
];

export default {
  title: 'Gravitee Dashboard/Components/Grid',
  component: GridComponent,
  decorators: [
    moduleMetadata({
      imports: [GridComponent],
    }),
    applicationConfig({
      providers: [
        // Register Chart.js controllers for pie charts
        provideCharts(withDefaultRegisterables()),
      ],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component:
          'A grid component built with Angular Gridster2 that displays widgets in a draggable and resizable layout. The grid options can be configured to customize the grid behavior. More info: https://tiberiuzuld.github.io/angular-gridster2/api',
      },
    },
  },
  argTypes: {
    items: {
      description: 'Array of widgets to display in the grid',
    },
  },
  render: args => ({
    template: `
          <gd-grid [items]="items" />
    `,
    props: {
      items: args.items,
    },
  }),
} satisfies Meta<GridStoryArgs>;

export const Default: StoryObj<GridStoryArgs> = {
  args: {
    items: commonItems,
  },
};
