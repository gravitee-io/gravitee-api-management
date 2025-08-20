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

import { GioChartBarComponent } from './gio-chart-bar.component';

export default {
  title: 'Shared / Bar Chart Component',
  component: GioChartBarComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, GioChartBarComponent],
    }),
  ],
  render: ({ data, options }) => {
    return {
      template: `
      <mat-card style="height: 400px; width: 1024px">
          <gio-chart-bar [data]="data" [options]="options"></gio-chart-bar>
      </mat-card>
      `,
      props: {
        data,
        options,
      },
    };
  },
} as Meta;

export const NoData: StoryObj = {
  args: {
    data: [],
    options: {},
  },
};

export const Simple: StoryObj = {
  args: {
    data: [
      {
        name: 'Sales',
        values: [120, 200, 150, 80, 70, 110, 130],
        color: '#7B61FF',
      },
      {
        name: 'Revenue',
        values: [90, 120, 100, 60, 50, 80, 90],
        color: '#00D4AA',
      },
    ],
    options: {
      categories: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    },
  },
};

export const Stacked: StoryObj = {
  args: {
    data: [
      {
        name: 'Desktop',
        values: [120, 200, 150, 80, 70, 110, 130],
        color: '#7B61FF',
      },
      {
        name: 'Mobile',
        values: [90, 120, 100, 60, 50, 80, 90],
        color: '#00D4AA',
      },
      {
        name: 'Tablet',
        values: [30, 50, 40, 20, 15, 25, 35],
        color: '#FF6B6B',
      },
    ],
    options: {
      categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
      stacked: true,
    },
  },
};
