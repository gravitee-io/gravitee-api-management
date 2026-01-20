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

import { BarChartComponent, BarType } from './bar-chart.component';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';

interface BarChartStoryArgs {
  storyId?: string;
  type: BarType;
  dataPoints: {
    timestamp: string;
    value: number;
  }[];
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Bar Chart',
  component: BarChartComponent,
  decorators: [
    moduleMetadata({
      imports: [BarChartComponent],
    }),
    applicationConfig({
      providers: [
        // Register Chart.js controllers for bar charts
        provideCharts(withDefaultRegisterables()),
      ],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A bar chart component built with Chart.js that displays time series data in a bar chart format.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    type: {
      control: { type: 'select' },
      options: ['bar'],
      description: 'Type of bar chart to display',
    },
    dataPoints: {
      control: { type: 'object' },
      description: 'Array of data points with timestamp and value',
    },
  },
  render: args => {
    const metricBuckets = args.dataPoints.map(point => ({
      key: point.timestamp,
      name: point.timestamp,
      timestamp: new Date(point.timestamp),
      measures: [
        {
          name: 'COUNT' as const,
          value: point.value,
        },
      ],
    }));

    const timeSeriesData: TimeSeriesResponse = {
      interval: '1h',
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          buckets: metricBuckets,
        },
      ],
      buckets: metricBuckets.map(({ key, name, timestamp }) => ({ key, name, timestamp })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-bar-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: {
        type: args.type,
        timeSeriesData,
      },
    };
  },
} satisfies Meta<BarChartStoryArgs>;

export const Default: StoryObj<BarChartStoryArgs> = {
  args: {
    storyId: 'default',
    type: 'bar',
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
    ],
  },
};

export const SparseData: StoryObj<BarChartStoryArgs> = {
  args: {
    storyId: 'sparse-data',
    type: 'bar',
    dataPoints: [
      { timestamp: '2025-01-01T00:00:00Z', value: 120 },
      { timestamp: '2025-01-01T06:00:00Z', value: 156 },
      { timestamp: '2025-01-01T12:00:00Z', value: 298 },
      { timestamp: '2025-01-01T18:00:00Z', value: 467 },
      { timestamp: '2025-01-02T00:00:00Z', value: 357 },
    ],
  },
};

export const HighVolume: StoryObj<BarChartStoryArgs> = {
  args: {
    storyId: 'high-volume',
    type: 'bar',
    dataPoints: Array.from({ length: 24 }, (_, i) => ({
      timestamp: new Date(new Date('2025-01-10T23:00:00Z').getTime() - (23 - i) * 60 * 60 * 1000).toISOString(),
      value: Math.floor(Math.random() * 1000) + 500,
    })),
  },
};

export const Stacked: StoryObj<BarChartStoryArgs> = {
  args: {
    storyId: 'stacked',
    type: 'bar',
    dataPoints: [],
  },
  render: args => {
    const timestamps = Array.from({ length: 40 }, (_, i) => {
      return new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 30 * 60 * 1000).toISOString();
    });

    const getRandomValue = (min: number, max: number) => Math.floor(Math.random() * (max - min + 1)) + min;

    const successBuckets = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: getRandomValue(200, 500) }],
    }));

    const errorBuckets = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: getRandomValue(10, 50) }],
    }));

    const otherBuckets = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: getRandomValue(5, 30) }],
    }));

    const timeSeriesData: TimeSeriesResponse = {
      interval: '30m',
      metrics: [
        { name: 'HTTP_REQUESTS', buckets: successBuckets },
        { name: 'HTTP_ERRORS', buckets: errorBuckets },
        { name: 'MESSAGES', buckets: otherBuckets },
      ],
      buckets: timestamps.map(ts => ({ key: ts, name: ts, timestamp: new Date(ts) })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-bar-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: {
        type: args.type,
        timeSeriesData,
      },
    };
  },
};

export const StatusDistribution: StoryObj<BarChartStoryArgs> = {
  args: {
    storyId: 'status-distribution',
    type: 'bar',
    dataPoints: [],
  },
  render: args => {
    const timestamps = Array.from({ length: 10 }, (_, i) => {
      return new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 1 * 60 * 60 * 1000).toISOString();
    });

    const getRandomValue = (min: number, max: number) => Math.floor(Math.random() * (max - min + 1)) + min;

    const maybeZero = (value: number, chance = 0.2) => (Math.random() < chance ? 0 : value);

    const buckets1xx = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: maybeZero(getRandomValue(5, 20)) }],
    }));
    const buckets2xx = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: getRandomValue(100, 300) }],
    }));

    const buckets3xx = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: maybeZero(getRandomValue(10, 50)) }],
    }));
    const buckets4xx = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: maybeZero(getRandomValue(20, 100)) }],
    }));

    const buckets5xx = timestamps.map(ts => ({
      key: ts,
      name: ts,
      timestamp: new Date(ts),
      measures: [{ name: 'COUNT' as const, value: maybeZero(getRandomValue(0, 30)) }],
    }));

    const timeSeriesData: TimeSeriesResponse = {
      interval: '1h',
      metrics: [
        { name: 'APIS', buckets: buckets1xx },
        { name: 'HTTP_REQUESTS', buckets: buckets2xx },
        { name: 'MESSAGES', buckets: buckets3xx },
        { name: 'HTTP_ERRORS', buckets: buckets4xx },
        { name: 'MESSAGE_ERRORS', buckets: buckets5xx },
      ],
      buckets: timestamps.map(ts => ({ key: ts, name: ts, timestamp: new Date(ts) })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-bar-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: {
        type: args.type,
        timeSeriesData,
      },
    };
  },
};
