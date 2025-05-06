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
import { CommonModule, TitleCasePipe } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { MatIconModule } from '@angular/material/icon';
import { MatTreeModule } from '@angular/material/tree';
import { MatButtonModule } from '@angular/material/button';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';

import '@gravitee/ui-components/wc/gv-icon';

import { DebugModeResponseComponent } from './debug-mode-response.component';

import { DebugResponse } from '../../models/DebugResponse';
import { fakeDebugResponse } from '../../models/DebugResponse.fixture';
import { DebugModeTimelineCardComponent } from '../debug-mode-timeline-card/debug-mode-timeline-card.component';
import { DebugModeTimelineComponent } from '../debug-mode-timeline/debug-mode-timeline.component';
import { DebugModeTimelineLegendComponent } from '../debug-mode-timeline-legend/debug-mode-timeline-legend.component';
import { DebugModeInspectorTableComponent } from '../debug-mode-inspector/debug-mode-inspector-table/debug-mode-inspector-table.component';
import { DebugModeInspectorBodyComponent } from '../debug-mode-inspector/debug-mode-inspector-body/debug-mode-inspector-body.component';
import { DebugModeInspectorTextComponent } from '../debug-mode-inspector/debug-mode-inspector-text/debug-mode-inspector-text.component';
import { DebugModeInspectorComponent } from '../debug-mode-inspector/debug-mode-inspector.component';
import { DebugModeTimelineOverviewComponent } from '../debug-mode-timeline-overview/debug-mode-timeline-overview.component';
import { DebugModeTimelineHoverComponent } from '../debug-mode-timeline-hover/debug-mode-timeline-hover.directive';
import { fakePolicyListItem } from '../../../../../entities/policy';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Response',
  component: DebugModeResponseComponent,
  decorators: [
    moduleMetadata({
      declarations: [
        DebugModeInspectorComponent,
        DebugModeInspectorBodyComponent,
        DebugModeInspectorTableComponent,
        DebugModeInspectorTextComponent,
        DebugModeTimelineCardComponent,
        DebugModeTimelineComponent,
        DebugModeTimelineOverviewComponent,
        DebugModeTimelineLegendComponent,
        DebugModeTimelineHoverComponent,
      ],
      imports: [CommonModule, BrowserAnimationsModule, MatButtonModule, MatIconModule, MatTreeModule, GioIconsModule, MatTooltipModule],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [TitleCasePipe],
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
            <debug-mode-response [debugResponse]="debugResponse" [listPolicies]="listPolicies"></debug-mode-response>
        </div>
    `,
  }),
} as Meta;

export const Default: StoryObj = {};

export const Loading: StoryObj = {
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

export const ResponseSuccess: StoryObj = {
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

export const ResponseErrorFullEmpty: StoryObj = {
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
