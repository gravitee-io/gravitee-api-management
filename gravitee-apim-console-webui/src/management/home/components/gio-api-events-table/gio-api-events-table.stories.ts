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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { GioApiEventsTableComponent } from './gio-api-events-table.component';
import { GioApiEventsTableModule } from './gio-api-events-table.module';
import { MOCK_SEARCH_RESPONSE } from './gio-api-events-table.util';

import { EventService } from '../../../../services-ngx/event.service';

export default {
  title: 'Home / Components / API events table',
  component: GioApiEventsTableComponent,
  decorators: [
    moduleMetadata({
      imports: [GioApiEventsTableModule, BrowserAnimationsModule],
      providers: [
        {
          provide: EventService,
          useValue: {
            search: () => {
              return of(MOCK_SEARCH_RESPONSE).pipe(delay(1000));
            },
          },
        },
      ],
    }),
  ],
  render: ({ timeRangeParams }) => ({
    props: { timeRangeParams },
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {
  timeRangeParams: {
    id: '1M',
    from: 1691565599784,
    to: 1694157599784,
    interval: 86400000,
  },
};
