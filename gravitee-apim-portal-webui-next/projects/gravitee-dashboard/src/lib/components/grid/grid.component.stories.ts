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
import { Widget } from '../widget/widget';

interface GridStoryArgs {
  items: Widget[];
}

// Common widget items to avoid duplication
const commonItems: Widget[] = [
  {
    id: 'widget-1',
    label: 'API Calls',
    type: 'pie',
    layout: { cols: 2, rows: 1, x: 0, y: 0 },
  },
  {
    id: 'widget-2',
    label: 'Database Queries',
    type: 'doughnut',
    layout: { cols: 1, rows: 1, x: 2, y: 0 },
  },
  {
    id: 'widget-3',
    label: 'Cache Hits',
    type: 'kpi',
    layout: { cols: 1, rows: 1, x: 3, y: 0 },
  },
  {
    id: 'widget-4',
    label: 'Error Rate',
    type: 'pie',
    layout: { cols: 1, rows: 3, x: 0, y: 1 },
  },
  {
    id: 'widget-5',
    label: 'Response Time',
    type: 'doughnut',
    layout: { cols: 3, rows: 3, x: 1, y: 1 },
  },
  {
    id: 'widget-6',
    label: 'User Activity',
    type: 'polarArea',
    layout: { cols: 2, rows: 2, x: 4, y: 1 },
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
} as Meta<GridStoryArgs>;

export const Default: StoryObj<GridStoryArgs> = {
  args: {
    items: commonItems,
  },
};
