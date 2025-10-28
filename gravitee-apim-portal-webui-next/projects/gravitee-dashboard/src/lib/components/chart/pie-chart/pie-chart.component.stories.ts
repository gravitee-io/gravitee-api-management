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

import { PieChartComponent, PieType } from './pie-chart.component';

interface PieChartStoryArgs {
  storyId?: string;
  type: PieType;
  data: {
    labels: string[];
    datasets: Array<{
      data: number[];
    }>;
  };
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Pie Chart',
  component: PieChartComponent,
  decorators: [
    moduleMetadata({
      imports: [PieChartComponent],
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
        component: 'A pie chart component built with Chart.js that displays data in a circular chart format.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    type: {
      control: { type: 'select' },
      options: ['pie', 'doughnut', 'polarArea'],
      description: 'Type of pie chart to display',
    },
    data: {
      description: 'Chart data containing labels and datasets',
    },
  },
  render: args => ({
    template: `
        <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
          <gd-pie-chart [type]="type" [data]="data" />
        </div>
    `,
    props: {
      type: args.type,
      data: args.data,
    },
  }),
} satisfies Meta<PieChartStoryArgs>;

export const Default: StoryObj<PieChartStoryArgs> = {
  args: {
    storyId: 'default',
    type: 'pie' as PieType,
    data: {
      labels: ['North America', 'Europe', 'Asia Pacific', 'South America', 'Africa', 'Middle East', 'Oceania'],
      datasets: [
        {
          data: [35, 28, 20, 8, 5, 3, 1],
        },
      ],
    },
  },
};
