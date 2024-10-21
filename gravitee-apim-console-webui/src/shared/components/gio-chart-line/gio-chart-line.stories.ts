/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { MatCardModule } from '@angular/material/card';

import { GioChartLineComponent } from './gio-chart-line.component';
import { GioChartLineModule } from './gio-chart-line.module';

export default {
  title: 'Shared / Line Chart Component',
  component: GioChartLineComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, GioChartLineModule],
    }),
  ],
  render: ({ data, options }) => {
    return {
      template: `
      <mat-card style="height: 400px; width: 1024px">
          <gio-chart-line [data]="data" [options]="options"></gio-chart-line>
      </mat-card>
      `,
      props: {
        data,
        options,
      },
    };
  },
} as Meta;

export const Simple: StoryObj = {
  args: {
    data: [
      {
        name: '200',
        values: [
          16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 13, 16, 10, 0, 0,
        ],
      },
    ],
    options: {
      pointStart: 1728992010000,
      pointInterval: 30000,
    },
  },
};

export const NoData: StoryObj = {
  args: {
    data: [],
    options: {},
  },
};
