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
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import '@gravitee/ui-components/wc/gv-icon';

import { PolicyStudioDebugResponseComponent } from './policy-studio-debug-response.component';

import { DebugResponse } from '../../models/DebugResponse';
import { fakeDebugResponse } from '../../models/DebugResponse.fixture';
import { PolicyStudioDebugTimelineCardComponent } from '../policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { PolicyStudioDebugTimelineComponent } from '../policy-studio-debug-timeline/policy-studio-debug-timeline.component';
import { fakePolicyListItem } from '../../../../../../entities/policy';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Response',
  component: PolicyStudioDebugResponseComponent,
  decorators: [
    moduleMetadata({
      declarations: [PolicyStudioDebugTimelineCardComponent, PolicyStudioDebugTimelineComponent],
      imports: [CommonModule, BrowserAnimationsModule, MatIconModule, GioIconsModule],
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
  render: ({ debugResponse, listPolicies }) => ({
    props: { debugResponse, listPolicies },
    template: '<policy-studio-debug-response [debugResponse]="debugResponse" [listPolicies]="listPolicies"></policy-studio-debug-response>',
  }),
} as Meta;

export const Default: Story = {};

export const Loading: Story = {
  args: {
    debugResponse: {
      isLoading: true,
      request: {},
      response: {},
      responseDebugSteps: [],
      backendResponse: {},
      requestDebugSteps: [],
      initialAttributes: {},
    } as DebugResponse,
  },
};

export const ResponseSuccess: Story = {
  args: {
    debugResponse: fakeDebugResponse({
      isLoading: false,
      request: {
        body: '',
        headers: {
          'Content-Type': ['application/json'],
        },
        method: 'GET',
        path: '/api/v1/',
      },
      response: {
        statusCode: 200,
        body: 'Ok',
        headers: {
          'Content-Type': ['text/plain'],
        },
        method: 'GET',
        path: '/api/v1/',
      },
    }),
    listPolicies: [
      fakePolicyListItem({ id: 'key-less', name: 'Key less' }),
      fakePolicyListItem({ id: 'policy-assign-attributes', name: 'Assign attributes' }),
      fakePolicyListItem({ id: 'policy-override-request-method', name: 'Override request attributes' }),
      fakePolicyListItem({ id: 'transform-headers', name: 'Transform headers' }),
      fakePolicyListItem({ id: 'policy-assign-content', name: 'Assign content' }),
    ],
  },
};

export const ResponseErrorFullEmpty: Story = {
  args: {
    debugResponse: {
      isLoading: false,
      request: {},
      response: {},
      responseDebugSteps: [],
      backendResponse: {},
      requestDebugSteps: [],
      initialAttributes: {},
    } as DebugResponse,
  },
};
