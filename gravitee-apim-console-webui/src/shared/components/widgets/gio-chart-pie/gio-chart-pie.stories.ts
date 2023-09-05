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
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { MatCardModule } from '@angular/material/card';

import { GioChartPieComponent } from './gio-chart-pie.component';
import { GioChartPieModule } from './gio-chart-pie.module';

export default {
  title: 'Shared / Pie Chart Component',
  component: GioChartPieComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, GioChartPieModule],
    }),
  ],
  render: ({ input, inputDescription, title }) => {
    return {
      template: `
      <mat-card style="width: 500px">
            <gio-chart-pie [input]="input" [inputDescription]="inputDescription" [title]="title"></gio-chart-pie>
      </mat-card>
      `,
      props: {
        input,
        inputDescription,
        title,
      },
    };
  },
} as Meta;

export const Simple: Story = {
  args: {
    input: [
      {
        color: '#bbb',
        label: '1xx',
        value: 0,
      },
      {
        color: '#30ab61',
        label: '2xx',
        value: 27567,
      },
      {
        color: '#365bd3',
        label: '3xx',
        value: 0,
      },
      {
        color: '#ff9f40',
        label: '4xx',
        value: 1000,
      },
      {
        color: '#cf3942',
        label: '5xx',
        value: 300,
      },
    ],
    inputDescription: 'Nb hits',
  },
};

export const NoData: Story = {
  args: {
    input: [],
    inputDescription: 'Nb hits',
  },
};
