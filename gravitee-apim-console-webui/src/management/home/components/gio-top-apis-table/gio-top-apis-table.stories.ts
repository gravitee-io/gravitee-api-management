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
import { action } from '@storybook/addon-actions';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { GioTopApisTableComponent } from './gio-top-apis-table.component';
import { GioTopApisTableModule } from './gio-top-apis-table.module';

import { UIRouterState } from '../../../../ajs-upgraded-providers';

export default {
  title: 'Home / Components / Top APIs table',
  component: GioTopApisTableComponent,
  decorators: [
    moduleMetadata({
      imports: [GioTopApisTableModule, BrowserAnimationsModule],
      providers: [{ provide: UIRouterState, useValue: { go: (...args) => action('Ajs state go')(args) } }],
    }),
  ],
  render: ({ data }) => ({
    props: { data },
  }),
} as Meta;

export const Default: Story = {};
Default.args = {
  data: {
    values: {
      '?': 2764281,
      '78416fd0-25ac-4234-816f-d025aca2345c': 351475,
      '62b7d292-8ee1-3913-8030-c883e01de8a0': 19,
      '5baa3ce2-5c8a-4a53-aa3c-e25c8a0a53aa': 17,
      '9cbd6331-fdc7-4362-bd63-31fdc71362ae': 2,
      'e78f07d4-d6d0-384e-8f8d-8cbe736074ad': 2,
      'b264ff24-9030-31ae-be7b-cd50e0a88920': 1,
    },
    metadata: {
      '78416fd0-25ac-4234-816f-d025aca2345c': {
        name: 'Snowcamp',
        version: '1',
        order: '1',
      },
      '5baa3ce2-5c8a-4a53-aa3c-e25c8a0a53aa': {
        name: 'Docs - APIM',
        version: '1.0',
        order: '3',
      },
      'e78f07d4-d6d0-384e-8f8d-8cbe736074ad': {
        name: 'API 1 with slow backend',
        version: '1',
        order: '5',
      },
      '62b7d292-8ee1-3913-8030-c883e01de8a0': {
        name: '4790',
        version: '1',
        order: '2',
      },
      '9cbd6331-fdc7-4362-bd63-31fdc71362ae': {
        name: 'test-bad-ssl',
        version: 'test-bad-ssl',
        order: '4',
      },
      'b264ff24-9030-31ae-be7b-cd50e0a88920': {
        name: 'API 2, call API 1 with slow backend',
        version: '1',
        order: '6',
      },
      '?': {
        name: 'Unknown API (not found)',
        unknown: 'true',
        order: '0',
      },
    },
  },
};
