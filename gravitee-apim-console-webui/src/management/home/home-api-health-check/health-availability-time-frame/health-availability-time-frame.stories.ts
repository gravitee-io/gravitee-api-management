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

import { HealthAvailabilityTimeFrameComponent, HealthAvailabilityTimeFrameOption } from './health-availability-time-frame.component';
import { HealthAvailabilityTimeFrameModule } from './health-availability-time-frame.module';

export default {
  title: 'Home / API Status / Health availability TimeFrame Component',
  component: HealthAvailabilityTimeFrameComponent,
  decorators: [
    moduleMetadata({
      imports: [MatCardModule, HealthAvailabilityTimeFrameModule],
    }),
  ],
  render: ({ option }) => {
    return {
      template: `
      <mat-card>
            <health-availability-time-frame [option]="option"></health-availability-time-frame>
      </mat-card>
      `,
      props: {
        option,
      },
    };
  },
} as Meta;

export const Simple: StoryObj = {
  args: {
    option: {
      timestamp: {
        start: 1679900320000,
        interval: 2000,
      },
      data: [100.0, 80.0, 90.0, 100.0, 0, 0, 0, 20.0, 30.0, 50.0, 60.0, 70.0, 0, 90.0, 100.0, 0, 0, 0, 20.0, 30.0, 50.0, 60.0, 70.0, 0],
    } as HealthAvailabilityTimeFrameOption,
  },
};

export const NoData: StoryObj = {
  args: {
    option: {
      timestamp: {
        start: 1679900320000,
        interval: 2000,
      },
      data: [],
    } as HealthAvailabilityTimeFrameOption,
  },
};
