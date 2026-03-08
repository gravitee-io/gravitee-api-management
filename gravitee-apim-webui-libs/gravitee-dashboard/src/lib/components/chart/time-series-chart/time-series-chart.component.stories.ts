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

import { TimeSeriesChartComponent } from './time-series-chart.component';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';
import { TimeSeriesType } from '../../widget/model/widget/widget.model';

interface TimeSeriesChartStoryArgs {
  storyId?: string;
  type: TimeSeriesType;
  dataPoints: {
    timestamp: string;
    value: number;
  }[];
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Time Series Chart',
  component: TimeSeriesChartComponent,
  decorators: [
    moduleMetadata({
      imports: [TimeSeriesChartComponent],
    }),
    applicationConfig({
      providers: [provideCharts(withDefaultRegisterables())],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A unified time-series chart component that supports both line and bar display modes.',
      },
    },
  },
  argTypes: {
    storyId: { table: { disable: true } },
    type: {
      control: { type: 'select' },
      options: ['time-series-line', 'time-series-bar'],
      description: 'Chart display type',
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
      measures: [{ name: 'COUNT' as const, value: point.value }],
    }));

    const timeSeriesData: TimeSeriesResponse = {
      interval: '1h',
      metrics: [{ name: 'HTTP_REQUESTS', buckets: metricBuckets }],
      buckets: metricBuckets.map(({ key, name, timestamp }) => ({ key, name, timestamp })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-time-series-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: { type: args.type, timeSeriesData },
    };
  },
} satisfies Meta<TimeSeriesChartStoryArgs>;

const HOURLY_DATA = Array.from({ length: 24 }, (_, i) => ({
  timestamp: new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 60 * 60 * 1000).toISOString(),
  value: Math.floor(100 + Math.sin(i / 3) * 80 + Math.random() * 40),
}));

export const DefaultLine: StoryObj<TimeSeriesChartStoryArgs> = {
  args: { storyId: 'default-line', type: 'time-series-line', dataPoints: HOURLY_DATA },
};

export const DefaultBar: StoryObj<TimeSeriesChartStoryArgs> = {
  args: { storyId: 'default-bar', type: 'time-series-bar', dataPoints: HOURLY_DATA },
};

export const SparseDataLine: StoryObj<TimeSeriesChartStoryArgs> = {
  args: {
    storyId: 'sparse-data-line',
    type: 'time-series-line',
    dataPoints: [
      { timestamp: '2025-01-01T00:00:00Z', value: 120 },
      { timestamp: '2025-01-01T06:00:00Z', value: 156 },
      { timestamp: '2025-01-01T12:00:00Z', value: 298 },
      { timestamp: '2025-01-01T18:00:00Z', value: 467 },
      { timestamp: '2025-01-02T00:00:00Z', value: 357 },
    ],
  },
};

export const SparseDataBar: StoryObj<TimeSeriesChartStoryArgs> = {
  args: {
    storyId: 'sparse-data-bar',
    type: 'time-series-bar',
    dataPoints: [
      { timestamp: '2025-01-01T00:00:00Z', value: 120 },
      { timestamp: '2025-01-01T06:00:00Z', value: 156 },
      { timestamp: '2025-01-01T12:00:00Z', value: 298 },
      { timestamp: '2025-01-01T18:00:00Z', value: 467 },
      { timestamp: '2025-01-02T00:00:00Z', value: 357 },
    ],
  },
};

export const StackedBar: StoryObj<TimeSeriesChartStoryArgs> = {
  args: { storyId: 'stacked-bar', type: 'time-series-bar', dataPoints: [] },
  render: args => {
    const timestamps = Array.from({ length: 40 }, (_, i) =>
      new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 30 * 60 * 1000).toISOString(),
    );

    const getRandomValue = (min: number, max: number) => Math.floor(Math.random() * (max - min + 1)) + min;

    const makeBuckets = (ts: string[]) =>
      ts.map(t => ({
        key: t,
        name: t,
        timestamp: new Date(t),
      }));

    const timeSeriesData: TimeSeriesResponse = {
      interval: '30m',
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          buckets: timestamps.map(t => ({
            key: t,
            name: t,
            timestamp: new Date(t),
            measures: [{ name: 'COUNT' as const, value: getRandomValue(200, 500) }],
          })),
        },
        {
          name: 'HTTP_ERRORS',
          buckets: timestamps.map(t => ({
            key: t,
            name: t,
            timestamp: new Date(t),
            measures: [{ name: 'COUNT' as const, value: getRandomValue(10, 50) }],
          })),
        },
        {
          name: 'MESSAGES',
          buckets: timestamps.map(t => ({
            key: t,
            name: t,
            timestamp: new Date(t),
            measures: [{ name: 'COUNT' as const, value: getRandomValue(5, 30) }],
          })),
        },
      ],
      buckets: makeBuckets(timestamps),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-time-series-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: { type: args.type, timeSeriesData },
    };
  },
};

export const ColorPaletteDemo: StoryObj<TimeSeriesChartStoryArgs> = {
  args: { type: 'time-series-line', dataPoints: [] },
  render: args => {
    const timestamps = Array.from({ length: 10 }, (_, i) =>
      new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 60 * 60 * 1000).toISOString(),
    );

    const createMetric = (name: string, baseValue: number) => ({
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      name: name as any,
      buckets: timestamps.map(ts => ({
        key: ts,
        name: ts,
        timestamp: new Date(ts),
        measures: [{ name: 'COUNT' as const, value: baseValue + Math.random() * 50 }],
      })),
    });

    const timeSeriesData: TimeSeriesResponse = {
      interval: '1h',
      metrics: [
        createMetric('01_BLUE', 100),
        createMetric('02_GREEN', 150),
        createMetric('03_YELLOW', 200),
        createMetric('04_ORANGE', 250),
        createMetric('05_RED', 300),
        createMetric('06_PURPLE', 350),
        createMetric('07_GREY', 400),
        createMetric('08_PINK', 450),
        createMetric('09_BROWN', 500),
        createMetric('10_GREEN_LEMON', 550),
      ],
      buckets: timestamps.map(ts => ({ key: ts, name: ts, timestamp: new Date(ts) })),
    };

    return {
      template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-time-series-chart [type]="type" [data]="timeSeriesData" />
        </div>
      `,
      props: { type: args.type, timeSeriesData },
    };
  },
};
