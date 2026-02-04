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
import { applicationConfig, moduleMetadata, StoryObj } from '@storybook/angular';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { Widget } from './model/widget/widget.model';
import { WidgetComponent, WidgetTitleComponent, WidgetBodyComponent } from './widget.component';
import { PieChartComponent } from '../chart/pie-chart/pie-chart.component';

interface WidgetStoryArgs {
  storyId: string;
  widgetTitle: string;
  widgetBody: string;
  isDraggable: boolean;
  item: Widget;
}

export default {
  title: 'Gravitee Dashboard/Components/Widget',
  component: WidgetComponent,
  decorators: [
    moduleMetadata({
      imports: [WidgetComponent, WidgetTitleComponent, WidgetBodyComponent, PieChartComponent],
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
          'A widget component that displays data in a container with title and body sections. Supports different widget types including charts, Plain text and KPIs.',
      },
    },
  },
  argTypes: {
    storyId: {
      table: { disable: true },
    },
    item: {
      description: 'Widget configuration object',
      if: { arg: 'storyId', neq: 'default' },
    },
    widgetTitle: {
      control: { type: 'text' },
      description: 'Custom title for the widget',
      defaultValue: 'Custom Widget Title',
    },
    widgetBody: {
      control: { type: 'text' },
      description: 'Custom body content for the widget',
      defaultValue: 'This is custom body content that can be edited by the user.',
    },
    isDraggable: {
      control: { type: 'boolean' },
      description: 'Custom body content for the widget',
      defaultValue: 'This is custom body content that can be edited by the user.',
    },
  },
};

export const Default: StoryObj<WidgetStoryArgs> = {
  args: {
    storyId: 'default',
    widgetTitle: 'Custom Widget Title',
    widgetBody: 'This is custom body content that can be edited by the user.',
  },
  render: args => {
    const typedArgs = args;
    return {
      template: `
            <gd-widget [isDraggable]="isDraggable">
              <gd-widget-title>{{ widgetTitle }}
              </gd-widget-title>
              <gd-widget-body >
                  {{ widgetBody }}
              </gd-widget-body>
            </gd-widget>
      `,
      props: {
        widgetTitle: typedArgs.widgetTitle,
        widgetBody: typedArgs.widgetBody,
        isDraggable: typedArgs.isDraggable,
      },
    };
  },
};

export const WidgetWithPieChart: StoryObj<WidgetStoryArgs> = {
  args: {
    widgetTitle: '{{item.label}}',
    widgetBody: '<gd-pie-chart type="pie" />',
    item: {
      id: 'widget-custom',
      title: 'Custom Analytics',
      type: 'pie',
      layout: { cols: 3, rows: 3, x: 0, y: 0 },
    },
  },
  render: args => {
    const typedArgs = args;
    return {
      template: `
            <gd-widget [isDraggable]="isDraggable">
              <gd-widget-title>{{item.label}}</gd-widget-title>
              <gd-widget-body>
                <gd-pie-chart type="pie" />
              </gd-widget-body>
            </gd-widget>
      `,
      props: {
        item: args.item,
        widgetTitle: typedArgs.widgetTitle,
        widgetBody: typedArgs.widgetBody,
        isDraggable: typedArgs.isDraggable,
      },
    };
  },
};
