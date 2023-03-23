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

import { GioRequestStatsComponent } from './gio-request-stats.component';
import { GioRequestStatsModule } from './gio-request-stats.module';

export default {
  title: 'Home / Widgets / Request stats',
  component: GioRequestStatsComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, GioRequestStatsModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Simple: Story = {
  render: () => {
    const input = {
      min: 0.02336,
      max: 23009.29032,
      avg: 8.4323,
      rps: 1.2012334,
      total: 332981092,
    };

    return {
      template: `
      <mat-card style="width: 500px">
          <gio-request-stats [source]="source"></gio-request-stats>
      </mat-card>
      `,
      props: { source: input },
      styles: [],
    };
  },
};
