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
// eslint-disable-next-line import/no-unresolved
import 'chartjs-adapter-date-fns';

import { LineChartComponent, LineType } from './line-chart.component';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';

interface LineChartStoryArgs {
  storyId?: string;
  type: LineType;
  dataPoints: {
    timestamp: string;
    value: number;
  }[];
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Line Chart',
  component: LineChartComponent,
  decorators: [
    moduleMetadata({
      imports: [LineChartComponent],
    }),
    applicationConfig({
      providers: [
        // Register Chart.js controllers for line charts
        provideCharts(withDefaultRegisterables()),
      ],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A line chart component built with Chart.js that displays time series data in a line chart format.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    type: {
      control: { type: 'select' },
      options: ['line'],
      description: 'Type of line chart to display',
    },
    dataPoints: {
      control: { type: 'object' },
      description: 'Array of data points with timestamp and value',
    },
  },
  render: args => {
    const timeSeriesData: TimeSeriesResponse = {
      interval: '1h',
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          buckets: args.dataPoints.map(point => ({
            key: point.timestamp,
            name: point.timestamp,
            timestamp: new Date(point.timestamp),
            measures: [
              {
                name: 'COUNT',
                value: point.value,
              },
            ],
          })),
        },
      ],
      buckets: args.dataPoints.map(point => ({
        key: point.timestamp,
        name: point.timestamp,
        timestamp: new Date(point.timestamp),
        measures: [
          {
            name: 'COUNT',
            value: point.value,
          },
        ],
      })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-line-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: {
        type: args.type,
        timeSeriesData,
      },
    };
  },
} satisfies Meta<LineChartStoryArgs>;

export const Default: StoryObj<LineChartStoryArgs> = {
  args: {
    storyId: 'default',
    type: 'line' as LineType,
    dataPoints: [
      { timestamp: '2025-01-01T00:00:00Z', value: 120 },
      { timestamp: '2025-01-01T01:00:00Z', value: 145 },
      { timestamp: '2025-01-01T02:00:00Z', value: 98 },
      { timestamp: '2025-01-01T03:00:00Z', value: 167 },
      { timestamp: '2025-01-01T04:00:00Z', value: 134 },
      { timestamp: '2025-01-01T05:00:00Z', value: 189 },
      { timestamp: '2025-01-01T06:00:00Z', value: 156 },
      { timestamp: '2025-01-01T07:00:00Z', value: 201 },
      { timestamp: '2025-01-01T08:00:00Z', value: 234 },
      { timestamp: '2025-01-01T09:00:00Z', value: 267 },
      { timestamp: '2025-01-01T10:00:00Z', value: 289 },
      { timestamp: '2025-01-01T11:00:00Z', value: 312 },
      { timestamp: '2025-01-01T12:00:00Z', value: 298 },
      { timestamp: '2025-01-01T13:00:00Z', value: 345 },
      { timestamp: '2025-01-01T14:00:00Z', value: 378 },
      { timestamp: '2025-01-01T15:00:00Z', value: 401 },
      { timestamp: '2025-01-01T16:00:00Z', value: 423 },
      { timestamp: '2025-01-01T17:00:00Z', value: 445 },
      { timestamp: '2025-01-01T18:00:00Z', value: 467 },
      { timestamp: '2025-01-01T19:00:00Z', value: 489 },
      { timestamp: '2025-01-01T20:00:00Z', value: 456 },
      { timestamp: '2025-01-01T21:00:00Z', value: 423 },
      { timestamp: '2025-01-01T22:00:00Z', value: 390 },
      { timestamp: '2025-01-01T23:00:00Z', value: 357 },
    ],
  },
};

export const SparseData: StoryObj<LineChartStoryArgs> = {
  args: {
    storyId: 'sparse-data',
    type: 'line' as LineType,
    dataPoints: [
      { timestamp: '2025-01-01T00:00:00Z', value: 120 },
      { timestamp: '2025-01-01T06:00:00Z', value: 156 },
      { timestamp: '2025-01-01T12:00:00Z', value: 298 },
      { timestamp: '2025-01-01T18:00:00Z', value: 467 },
      { timestamp: '2025-01-02T00:00:00Z', value: 357 },
    ],
  },
};

export const HighVolume: StoryObj<LineChartStoryArgs> = {
  args: {
    storyId: 'high-volume',
    type: 'line' as LineType,
    dataPoints: Array.from({ length: 48 }, (_, i) => ({
      timestamp: new Date(Date.now() - (47 - i) * 60 * 60 * 1000).toISOString(),
      value: Math.floor(Math.random() * 1000) + 500,
    })),
  },
};

export const ProductionResponseTime: StoryObj<LineChartStoryArgs> = {
  args: {
    storyId: 'production-response-time',
    type: 'line' as LineType,
    dataPoints: [],
  },
  render: args => {
    const timeSeriesData: TimeSeriesResponse = {
      interval: '5h36m',
      metrics: [
        {
          name: 'HTTP_ENDPOINT_RESPONSE_TIME',
          buckets: [
            {
              key: '2026-01-13T14:24:00Z',
              name: '2026-01-13T14:24:00Z',
              timestamp: new Date('2026-01-13T14:24:00Z'),
              measures: [{ name: 'AVG', value: 0 }],
            },
            {
              key: '2026-01-13T20:00:00Z',
              name: '2026-01-13T20:00:00Z',
              timestamp: new Date('2026-01-13T20:00:00Z'),
              measures: [{ name: 'AVG', value: 2.6 }],
            },
            {
              key: '2026-01-14T01:36:00Z',
              name: '2026-01-14T01:36:00Z',
              timestamp: new Date('2026-01-14T01:36:00Z'),
              measures: [{ name: 'AVG', value: 0 }],
            },
            {
              key: '2026-01-15T05:36:00Z',
              name: '2026-01-15T05:36:00Z',
              timestamp: new Date('2026-01-15T05:36:00Z'),
              measures: [{ name: 'AVG', value: 38.555557 }],
            },
            {
              key: '2026-01-15T11:12:00Z',
              name: '2026-01-15T11:12:00Z',
              timestamp: new Date('2026-01-15T11:12:00Z'),
              measures: [{ name: 'AVG', value: 156.875 }],
            },
            {
              key: '2026-01-16T04:00:00Z',
              name: '2026-01-16T04:00:00Z',
              timestamp: new Date('2026-01-16T04:00:00Z'),
              measures: [{ name: 'AVG', value: 15.933333 }],
            },
            {
              key: '2026-01-16T09:36:00Z',
              name: '2026-01-16T09:36:00Z',
              timestamp: new Date('2026-01-16T09:36:00Z'),
              measures: [{ name: 'AVG', value: 236.8 }],
            },
            {
              key: '2026-01-16T15:12:00Z',
              name: '2026-01-16T15:12:00Z',
              timestamp: new Date('2026-01-16T15:12:00Z'),
              measures: [{ name: 'AVG', value: 208.3125 }],
            },
            {
              key: '2026-01-17T08:00:00Z',
              name: '2026-01-17T08:00:00Z',
              timestamp: new Date('2026-01-17T08:00:00Z'),
              measures: [{ name: 'AVG', value: 158.0 }],
            },
            {
              key: '2026-01-19T04:48:00Z',
              name: '2026-01-19T04:48:00Z',
              timestamp: new Date('2026-01-19T04:48:00Z'),
              measures: [{ name: 'AVG', value: 250.76923 }],
            },
            {
              key: '2026-01-19T10:24:00Z',
              name: '2026-01-19T10:24:00Z',
              timestamp: new Date('2026-01-19T10:24:00Z'),
              measures: [{ name: 'AVG', value: 28.54 }],
            },
            {
              key: '2026-01-19T16:00:00Z',
              name: '2026-01-19T16:00:00Z',
              timestamp: new Date('2026-01-19T16:00:00Z'),
              measures: [{ name: 'AVG', value: 268.66666 }],
            },
            {
              key: '2026-01-19T21:36:00Z',
              name: '2026-01-19T21:36:00Z',
              timestamp: new Date('2026-01-19T21:36:00Z'),
              measures: [{ name: 'AVG', value: 26.5 }],
            },
            {
              key: '2026-01-20T08:48:00Z',
              name: '2026-01-20T08:48:00Z',
              timestamp: new Date('2026-01-20T08:48:00Z'),
              measures: [{ name: 'AVG', value: 47.24616 }],
            },
            {
              key: '2026-01-20T14:24:00Z',
              name: '2026-01-20T14:24:00Z',
              timestamp: new Date('2026-01-20T14:24:00Z'),
              measures: [{ name: 'AVG', value: 19.258064 }],
            },
          ],
        },
        {
          name: 'HTTP_GATEWAY_RESPONSE_TIME',
          buckets: [
            {
              key: '2026-01-13T14:24:00Z',
              name: '2026-01-13T14:24:00Z',
              timestamp: new Date('2026-01-13T14:24:00Z'),
              measures: [{ name: 'AVG', value: 0 }],
            },
            {
              key: '2026-01-13T20:00:00Z',
              name: '2026-01-13T20:00:00Z',
              timestamp: new Date('2026-01-13T20:00:00Z'),
              measures: [{ name: 'AVG', value: 3.0 }],
            },
            {
              key: '2026-01-14T01:36:00Z',
              name: '2026-01-14T01:36:00Z',
              timestamp: new Date('2026-01-14T01:36:00Z'),
              measures: [{ name: 'AVG', value: 0 }],
            },
            {
              key: '2026-01-14T12:48:00Z',
              name: '2026-01-14T12:48:00Z',
              timestamp: new Date('2026-01-14T12:48:00Z'),
              measures: [{ name: 'AVG', value: 8.0 }],
            },
            {
              key: '2026-01-15T05:36:00Z',
              name: '2026-01-15T05:36:00Z',
              timestamp: new Date('2026-01-15T05:36:00Z'),
              measures: [{ name: 'AVG', value: 39.88889 }],
            },
            {
              key: '2026-01-15T11:12:00Z',
              name: '2026-01-15T11:12:00Z',
              timestamp: new Date('2026-01-15T11:12:00Z'),
              measures: [{ name: 'AVG', value: 170.0 }],
            },
            {
              key: '2026-01-16T04:00:00Z',
              name: '2026-01-16T04:00:00Z',
              timestamp: new Date('2026-01-16T04:00:00Z'),
              measures: [{ name: 'AVG', value: 1103.4 }],
            },
            {
              key: '2026-01-16T09:36:00Z',
              name: '2026-01-16T09:36:00Z',
              timestamp: new Date('2026-01-16T09:36:00Z'),
              measures: [{ name: 'AVG', value: 237.55 }],
            },
            {
              key: '2026-01-16T15:12:00Z',
              name: '2026-01-16T15:12:00Z',
              timestamp: new Date('2026-01-16T15:12:00Z'),
              measures: [{ name: 'AVG', value: 413.0 }],
            },
            {
              key: '2026-01-17T08:00:00Z',
              name: '2026-01-17T08:00:00Z',
              timestamp: new Date('2026-01-17T08:00:00Z'),
              measures: [{ name: 'AVG', value: 160.28572 }],
            },
            {
              key: '2026-01-17T13:36:00Z',
              name: '2026-01-17T13:36:00Z',
              timestamp: new Date('2026-01-17T13:36:00Z'),
              measures: [{ name: 'AVG', value: 955.0 }],
            },
            {
              key: '2026-01-19T04:48:00Z',
              name: '2026-01-19T04:48:00Z',
              timestamp: new Date('2026-01-19T04:48:00Z'),
              measures: [{ name: 'AVG', value: 253.46153 }],
            },
            {
              key: '2026-01-19T10:24:00Z',
              name: '2026-01-19T10:24:00Z',
              timestamp: new Date('2026-01-19T10:24:00Z'),
              measures: [{ name: 'AVG', value: 117.26 }],
            },
            {
              key: '2026-01-19T16:00:00Z',
              name: '2026-01-19T16:00:00Z',
              timestamp: new Date('2026-01-19T16:00:00Z'),
              measures: [{ name: 'AVG', value: 269.66666 }],
            },
            {
              key: '2026-01-19T21:36:00Z',
              name: '2026-01-19T21:36:00Z',
              timestamp: new Date('2026-01-19T21:36:00Z'),
              measures: [{ name: 'AVG', value: 27.375 }],
            },
            {
              key: '2026-01-20T08:48:00Z',
              name: '2026-01-20T08:48:00Z',
              timestamp: new Date('2026-01-20T08:48:00Z'),
              measures: [{ name: 'AVG', value: 47.861164 }],
            },
            {
              key: '2026-01-20T14:24:00Z',
              name: '2026-01-20T14:24:00Z',
              timestamp: new Date('2026-01-20T14:24:00Z'),
              measures: [{ name: 'AVG', value: 19.774193 }],
            },
          ],
        },
      ],
      buckets: [
        { key: '2026-01-13T14:24:00Z', name: '2026-01-13T14:24:00Z', timestamp: new Date('2026-01-13T14:24:00Z') },
        { key: '2026-01-13T20:00:00Z', name: '2026-01-13T20:00:00Z', timestamp: new Date('2026-01-13T20:00:00Z') },
        { key: '2026-01-14T01:36:00Z', name: '2026-01-14T01:36:00Z', timestamp: new Date('2026-01-14T01:36:00Z') },
        { key: '2026-01-15T05:36:00Z', name: '2026-01-15T05:36:00Z', timestamp: new Date('2026-01-15T05:36:00Z') },
        { key: '2026-01-15T11:12:00Z', name: '2026-01-15T11:12:00Z', timestamp: new Date('2026-01-15T11:12:00Z') },
        { key: '2026-01-16T04:00:00Z', name: '2026-01-16T04:00:00Z', timestamp: new Date('2026-01-16T04:00:00Z') },
        { key: '2026-01-16T09:36:00Z', name: '2026-01-16T09:36:00Z', timestamp: new Date('2026-01-16T09:36:00Z') },
        { key: '2026-01-16T15:12:00Z', name: '2026-01-16T15:12:00Z', timestamp: new Date('2026-01-16T15:12:00Z') },
        { key: '2026-01-17T08:00:00Z', name: '2026-01-17T08:00:00Z', timestamp: new Date('2026-01-17T08:00:00Z') },
        { key: '2026-01-19T04:48:00Z', name: '2026-01-19T04:48:00Z', timestamp: new Date('2026-01-19T04:48:00Z') },
        { key: '2026-01-19T10:24:00Z', name: '2026-01-19T10:24:00Z', timestamp: new Date('2026-01-19T10:24:00Z') },
        { key: '2026-01-19T16:00:00Z', name: '2026-01-19T16:00:00Z', timestamp: new Date('2026-01-19T16:00:00Z') },
        { key: '2026-01-19T21:36:00Z', name: '2026-01-19T21:36:00Z', timestamp: new Date('2026-01-19T21:36:00Z') },
        { key: '2026-01-20T08:48:00Z', name: '2026-01-20T08:48:00Z', timestamp: new Date('2026-01-20T08:48:00Z') },
        { key: '2026-01-20T14:24:00Z', name: '2026-01-20T14:24:00Z', timestamp: new Date('2026-01-20T14:24:00Z') },
      ],
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-line-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: {
        type: args.type,
        timeSeriesData,
      },
    };
  },
};
