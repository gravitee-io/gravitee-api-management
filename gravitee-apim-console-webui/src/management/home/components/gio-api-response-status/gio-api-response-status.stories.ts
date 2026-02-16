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
import { Meta, moduleMetadata, componentWrapperDecorator, StoryObj } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { GioApiResponseStatusComponent } from './gio-api-response-status.component';
import { GioApiResponseStatusModule } from './gio-api-response-status.module';

export default {
  title: 'Home / Components / API response status',
  component: GioApiResponseStatusComponent,
  decorators: [
    moduleMetadata({
      imports: [GioApiResponseStatusModule, BrowserAnimationsModule],
    }),
    componentWrapperDecorator(story => `<div style="height:400px;width: 400px">${story}</div>`),
  ],
  render: ({ data }) => ({
    props: { data },
    template: `<gio-api-response-status [data]="data"></gio-api-response-status>`,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {
  data: {
    values: {
      '200.0-300.0': 2,
      '300.0-400.0': 3,
      '400.0-500.0': 4,
      '500.0-600.0': 5,
      '100.0-200.0': 1,
    },
    metadata: {
      '300.0-400.0': {
        name: '300.0-400.0',
        order: '2',
      },
      '100.0-200.0': {
        name: '100.0-200.0',
        order: '0',
      },
      '200.0-300.0': {
        name: '200.0-300.0',
        order: '1',
      },
      '400.0-500.0': {
        name: '400.0-500.0',
        order: '3',
      },
      '500.0-600.0': {
        name: '500.0-600.0',
        order: '4',
      },
    },
  },
};
