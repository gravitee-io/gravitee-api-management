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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { StatsComponent } from './stats.component';
import { MeasureName } from '../../widget/model/request/enum/measure-name';
import { MeasuresResponse } from '../../widget/model/response/measures-response';

interface StatsStoryArgs {
  storyId?: string;
  measures: Array<{
    name: MeasureName;
    value: number;
  }>;
}

export default {
  title: 'Gravitee Dashboard/Components/Text/Stats',
  component: StatsComponent,
  decorators: [
    moduleMetadata({
      imports: [StatsComponent],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A stats component that displays data in text format.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    measures: {
      control: { type: 'object' },
      description: 'Array of measures with name and value',
    },
  },
  render: args => {
    const metricsData: MeasuresResponse = {
      metrics: [
        {
          name: 'HTTP_REQUESTS',
          measures: args.measures,
        },
      ],
    };

    return {
      template: `
        <div>
          <gd-stats [data]="metricsData"></gd-stats>
        </div>
      `,
      props: {
        metricsData,
      },
    };
  },
} satisfies Meta<StatsStoryArgs>;

export const Default: StoryObj<StatsStoryArgs> = {
  args: {
    storyId: 'default',
    measures: [
      { name: 'COUNT', value: 1234 },
      { name: 'AVG', value: 45.6 },
      { name: 'MAX', value: 1200 },
    ],
  },
};
