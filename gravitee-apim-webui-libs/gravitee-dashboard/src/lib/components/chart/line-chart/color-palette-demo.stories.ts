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

import { LineChartComponent, LineType } from './line-chart.component';
import { TimeSeriesResponse } from '../../widget/model/response/time-series-response';

interface ColorPaletteStoryArgs {
  type: LineType;
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Color Palette Demo',
  component: LineChartComponent,
  decorators: [
    moduleMetadata({
      imports: [LineChartComponent],
    }),
    applicationConfig({
      providers: [provideCharts(withDefaultRegisterables())],
    }),
  ],
} satisfies Meta<ColorPaletteStoryArgs>;

export const TwelveDatasets: StoryObj<ColorPaletteStoryArgs> = {
  args: {
    type: 'line' as LineType,
  },
  render: args => {
    const timestamps = Array.from({ length: 10 }, (_, i) => {
      return new Date(new Date('2025-01-01T00:00:00Z').getTime() + i * 1 * 60 * 60 * 1000).toISOString();
    });

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
        createMetric('11_CYCLES_TO_BLUE', 600),
        createMetric('12_CYCLES_TO_GREEN', 650),
      ],
      buckets: timestamps.map(ts => ({ key: ts, name: ts, timestamp: new Date(ts) })),
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
