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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { GioApiEventsTableComponent } from './gio-api-events-table.component';
import { GioApiEventsTableModule } from './gio-api-events-table.module';

import { EventService } from '../../../../services-ngx/event.service';

export const SEARCH_RESPONSE = {
  content: [
    {
      id: 'eba738ec-2fc7-40a7-a738-ec2fc7f0a760',
      type: 'PUBLISH_API',
      properties: {
        api_id: 'fcf65967-8f3a-4f4c-b659-678f3a6f4c12',
        deployment_number: '3',
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        origin: '0.0.0.0',
        api_name: 'LPI Kafka',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693404843037,
      updated_at: 1693404843037,
    },
    {
      id: '7eda6159-f8cf-45d2-9a61-59f8cf25d24d',
      type: 'PUBLISH_API',
      properties: {
        api_id: 'fcf65967-8f3a-4f4c-b659-678f3a6f4c12',
        deployment_number: '2',
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        origin: '0.0.0.0',
        api_name: 'LPI Kafka',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693403898443,
      updated_at: 1693403898443,
    },
    {
      id: 'ffed7f9a-0a4f-477c-ad7f-9a0a4f477c5b',
      type: 'PUBLISH_API',
      properties: {
        api_id: 'fcf65967-8f3a-4f4c-b659-678f3a6f4c12',
        deployment_number: '1',
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        origin: '0.0.0.0',
        api_name: 'LPI Kafka',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693403586978,
      updated_at: 1693403586978,
    },
    {
      id: '7e7377c1-22ee-43be-b377-c122eed3be9f',
      type: 'START_API',
      properties: {
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        api_id: 'd7716a35-1e53-4d42-b16a-351e53ed42c1',
        origin: '0.0.0.0',
        api_name: 'LPI test v2',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693398269882,
      updated_at: 1693398269882,
    },
    {
      id: '55cbc4a4-3ec3-4e4e-8bc4-a43ec39e4e06',
      type: 'PUBLISH_API',
      properties: {
        api_id: 'd7716a35-1e53-4d42-b16a-351e53ed42c1',
        deployment_number: '1',
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        origin: '0.0.0.0',
        api_name: 'LPI test v2',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693398268756,
      updated_at: 1693398268756,
    },
    {
      id: 'c35bf90b-15b7-452c-9bf9-0b15b7852ccf',
      type: 'PUBLISH_API',
      properties: {
        api_id: 'ce38956e-572b-4f12-b895-6e572b4f121e',
        deployment_number: '1',
        user: 'e639ac53-89df-4946-b9ac-5389dfa94693',
        origin: '0.0.0.0',
        api_name: 'SSE &#43; Kafka',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693398122715,
      updated_at: 1693398122715,
    },
    {
      id: 'f5bd4258-a686-4971-bd42-58a6861971db',
      type: 'STOP_API',
      properties: {
        user: 'cfeaf0f1-9833-458b-aaf0-f19833e58b98',
        api_id: '001bb281-5f62-4a34-9bb2-815f621a34b3',
        origin: '0.0.0.0',
        api_name: 'LPI test',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693304040250,
      updated_at: 1693304040250,
    },
    {
      id: '62789c43-b7a7-481f-b89c-43b7a7a81ffb',
      type: 'START_API',
      properties: {
        user: 'cfeaf0f1-9833-458b-aaf0-f19833e58b98',
        api_id: '001bb281-5f62-4a34-9bb2-815f621a34b3',
        origin: '0.0.0.0',
        api_name: 'LPI test',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693304035131,
      updated_at: 1693304035131,
    },
    {
      id: '01a4fc53-f4c7-4b50-a4fc-53f4c78b5074',
      type: 'STOP_API',
      properties: {
        user: 'cfeaf0f1-9833-458b-aaf0-f19833e58b98',
        api_id: '001bb281-5f62-4a34-9bb2-815f621a34b3',
        origin: '0.0.0.0',
        api_name: 'LPI test',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693302134385,
      updated_at: 1693302134385,
    },
    {
      id: 'a141eb60-24d1-456d-81eb-6024d1356d30',
      type: 'START_API',
      properties: {
        user: 'cfeaf0f1-9833-458b-aaf0-f19833e58b98',
        api_id: '001bb281-5f62-4a34-9bb2-815f621a34b3',
        origin: '0.0.0.0',
        api_name: 'LPI test',
        api_version: '1',
      },
      user: {},
      environments: ['DEFAULT'],
      created_at: 1693302124186,
      updated_at: 1693302124186,
    },
  ],
  pageNumber: 1,
  pageElements: 10,
  totalElements: 84,
};
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
              return of(SEARCH_RESPONSE).pipe(delay(1000));
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

export const Default: Story = {};
Default.args = {
  timeRangeParams: {
    id: '1M',
    from: 1691565599784,
    to: 1694157599784,
    interval: 86400000,
  },
};
