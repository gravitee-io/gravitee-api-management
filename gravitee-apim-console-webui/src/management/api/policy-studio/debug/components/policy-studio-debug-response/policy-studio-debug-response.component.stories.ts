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
import { CommonModule } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import '@gravitee/ui-components/wc/gv-code';
import '@gravitee/ui-components/wc/gv-icon';

import { PolicyStudioDebugResponseComponent } from './policy-studio-debug-response.component';

import { DebugResponse } from '../../models/DebugResponse';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Response',
  component: PolicyStudioDebugResponseComponent,
  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserAnimationsModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }),
  ],
  argTypes: {
    debugResponse: {
      control: {
        type: 'object',
      },
    },
  },
  args: {
    defaultValue: null,
  },
  render: ({ debugResponse }) => ({
    props: { debugResponse },
    template: '<policy-studio-debug-response [debugResponse]="debugResponse"></policy-studio-debug-response>',
  }),
} as Meta;

export const Default: Story = {};

export const Loading: Story = {
  args: {
    debugResponse: {
      isLoading: true,
    } as DebugResponse,
  },
};

export const ResponseSuccess: Story = {
  args: {
    debugResponse: {
      isLoading: false,
      request: {
        body: '',
        headers: [{ name: 'Content-Type', value: 'application/json' }],
        method: 'GET',
        path: '/api/v1/',
      },
      response: {
        statusCode: 200,
        body: 'Ok',
        headers: {
          'Content-Type': 'text/plain',
        },
        method: 'GET',
        path: '/api/v1/',
      },
    } as DebugResponse,
  },
};

export const ResponseError: Story = {
  args: {
    debugResponse: {
      isLoading: false,
      request: {
        body: '',
        headers: [{ name: 'Content-Type', value: 'application/json' }],
        method: 'GET',
        path: '/api/v1/',
      },
      response: {
        statusCode: 404,
        method: 'GET',
        path: '/api/v1/',
      },
    } as DebugResponse,
  },
};
