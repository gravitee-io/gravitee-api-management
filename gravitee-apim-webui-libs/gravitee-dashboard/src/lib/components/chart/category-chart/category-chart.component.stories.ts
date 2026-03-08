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

import { CategoryChartComponent } from './category-chart.component';
import { FacetsResponse } from '../../widget/model/response/facets-response';
import { CategoryType } from '../../widget/model/widget/widget.model';

interface CategoryChartStoryArgs {
  type: CategoryType;
  facetsData: FacetsResponse;
}

export default {
  title: 'Gravitee Dashboard/Components/Chart/Category Chart',
  component: CategoryChartComponent,
  decorators: [
    moduleMetadata({
      imports: [CategoryChartComponent],
    }),
    applicationConfig({
      providers: [provideCharts(withDefaultRegisterables())],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A category bar chart component that displays facets data as vertical or horizontal bars.',
      },
    },
  },
  argTypes: {
    type: {
      control: { type: 'select' },
      options: ['vertical-bar', 'horizontal-bar'],
      description: 'Bar orientation',
    },
  },
} satisfies Meta<CategoryChartStoryArgs>;

const MCP_METHOD_DATA: FacetsResponse = {
  metrics: [
    {
      name: 'HTTP_REQUESTS',
      buckets: [
        { key: 'tools/call', name: 'tools/call', measures: [{ name: 'COUNT', value: 17 }] },
        { key: 'tools/list', name: 'tools/list', measures: [{ name: 'COUNT', value: 14 }] },
        { key: 'resources/list', name: 'resources/list', measures: [{ name: 'COUNT', value: 10 }] },
        { key: 'prompts/list', name: 'prompts/list', measures: [{ name: 'COUNT', value: 8 }] },
        { key: 'resources/call', name: 'resources/call', measures: [{ name: 'COUNT', value: 6 }] },
      ],
    },
  ],
};

export const VerticalBar: StoryObj<CategoryChartStoryArgs> = {
  args: { type: 'vertical-bar', facetsData: MCP_METHOD_DATA },
  render: args => ({
    template: `
      <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
        <gd-category-chart [type]="type" [data]="facetsData" />
      </div>
    `,
    props: { type: args.type, facetsData: args.facetsData },
  }),
};

export const HorizontalBar: StoryObj<CategoryChartStoryArgs> = {
  args: { type: 'horizontal-bar', facetsData: MCP_METHOD_DATA },
  render: args => ({
    template: `
      <div style="height: 100vh; width: 100vw; position: absolute; top: 0; left: 0;">
        <gd-category-chart [type]="type" [data]="facetsData" />
      </div>
    `,
    props: { type: args.type, facetsData: args.facetsData },
  }),
};
