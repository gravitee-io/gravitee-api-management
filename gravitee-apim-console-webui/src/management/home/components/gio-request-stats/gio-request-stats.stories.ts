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

import { GioRequestStatsComponent } from './gio-request-stats.component';
import { GioRequestStatsModule } from './gio-request-stats.module';

import { AnalyticsStatsResponse } from '../../../../entities/analytics/analyticsResponse';

export default {
  title: 'Home / Components / Request stats',
  component: GioRequestStatsComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, GioRequestStatsModule],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Simple: StoryObj = {
  render: () => {
    const input: AnalyticsStatsResponse = {
      min: 0.02336,
      max: 23009.29032,
      avg: 8.4323,
      rps: 1.2012334,
      rpm: 72.074004,
      rph: 4324.44024,
      count: 332981092,
      sum: 4567115654.2,
    };

    return {
      template: `
      <mat-card style="width: 500px">
          <gio-request-stats [data]="source"></gio-request-stats>
      </mat-card>
      `,
      props: { source: input },
      styleUrls: [],
    };
  },
};
