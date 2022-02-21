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
import { MatTreeModule } from '@angular/material/tree';
import { MatButtonModule } from '@angular/material/button';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { MatTooltipModule } from '@angular/material/tooltip';

import '@gravitee/ui-components/wc/gv-icon';

import { PolicyStudioDebugResponseComponent } from './policy-studio-debug-response.component';

import { DebugResponse } from '../../models/DebugResponse';
import { fakeDebugResponse } from '../../models/DebugResponse.fixture';
import { PolicyStudioDebugTimelineCardComponent } from '../policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { PolicyStudioDebugTimelineComponent } from '../policy-studio-debug-timeline/policy-studio-debug-timeline.component';
import { fakePolicyListItem } from '../../../../../../entities/policy';
import { PolicyStudioDebugTimelineLegendComponent } from '../policy-studio-debug-timeline-legend/policy-studio-debug-timeline-legend.component';
import { PolicyStudioDebugInspectorTableComponent } from '../policy-studio-debug-inspector/policy-studio-debug-inspector-table/policy-studio-debug-inspector-table.component';
import { PolicyStudioDebugInspectorBodyComponent } from '../policy-studio-debug-inspector/policy-studio-debug-inspector-body/policy-studio-debug-inspector-body.component';
import { PolicyStudioDebugInspectorTextComponent } from '../policy-studio-debug-inspector/policy-studio-debug-inspector-text/policy-studio-debug-inspector-text.component';
import { PolicyStudioDebugInspectorComponent } from '../policy-studio-debug-inspector/policy-studio-debug-inspector.component';
import { PolicyStudioDebugTimelineOverviewComponent } from '../policy-studio-debug-timeline-overview/policy-studio-debug-timeline-overview.component';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Response',
  component: PolicyStudioDebugResponseComponent,
  decorators: [
    moduleMetadata({
      declarations: [
        PolicyStudioDebugInspectorComponent,
        PolicyStudioDebugInspectorBodyComponent,
        PolicyStudioDebugInspectorTableComponent,
        PolicyStudioDebugInspectorTextComponent,
        PolicyStudioDebugTimelineCardComponent,
        PolicyStudioDebugTimelineComponent,
        PolicyStudioDebugTimelineOverviewComponent,
        PolicyStudioDebugTimelineLegendComponent,
      ],
      imports: [CommonModule, BrowserAnimationsModule, MatButtonModule, MatIconModule, MatTreeModule, GioIconsModule, MatTooltipModule],
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
  parameters: {
    layout: 'fullscreen',
  },
  render: ({ debugResponse, listPolicies }) => ({
    props: { debugResponse, listPolicies },
    template: `
        <div style="height: 100vh">
            <policy-studio-debug-response [debugResponse]="debugResponse" [listPolicies]="listPolicies"></policy-studio-debug-response>
        </div>
    `,
  }),
} as Meta;

export const Default: Story = {};

export const Loading: Story = {
  args: {
    debugResponse: {
      isLoading: true,
      request: {},
      response: {},
      responsePolicyDebugSteps: [],
      backendResponse: {},
      requestPolicyDebugSteps: [],
      preprocessorStep: {},
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
      responsePolicyDebugSteps: [],
      backendResponse: {},
      requestPolicyDebugSteps: [],
      preprocessorStep: {},
    } as DebugResponse,
  },
};
