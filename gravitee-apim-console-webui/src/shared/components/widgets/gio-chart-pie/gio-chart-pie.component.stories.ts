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
import { CommonModule } from '@angular/common';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatCardModule } from '@angular/material/card';

import { GioChartPieComponent } from './gio-chart-pie.component';
import { GioChartPieComponentModule } from './gio-chart-pie.component.module';

export default {
  title: 'Shared / Pie Chart Component',
  component: GioChartPieComponent,
  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserAnimationsModule, MatCardModule, GioChartPieComponentModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Simple: Story = {
  render: () => {
    return {
      template: `
      <mat-card>
            <gio-chart-pie></gio-chart-pie>
      </mat-card>
      `,
      props: {},
      styles: [],
    };
  },
};
