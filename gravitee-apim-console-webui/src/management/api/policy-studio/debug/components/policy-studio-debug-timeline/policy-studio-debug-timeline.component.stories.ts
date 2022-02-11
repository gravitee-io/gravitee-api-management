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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata } from '@storybook/angular';
import { MatIconModule } from '@angular/material/icon';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { PolicyStudioDebugTimelineComponent } from './policy-studio-debug-timeline.component';

import {
  PolicyStudioDebugTimelineCardComponent,
  TimelineStep,
} from '../policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Timeline ',
  component: PolicyStudioDebugTimelineComponent,
  decorators: [
    moduleMetadata({
      declarations: [PolicyStudioDebugTimelineCardComponent],
      imports: [CommonModule, BrowserAnimationsModule, MatIconModule, GioIconsModule],
    }),
  ],
  argTypes: {
    timelineSteps: {
      control: {
        type: 'object',
      },
    },
  },
  args: {
    timelineSteps: null,
  },
  render: ({ timelineSteps }) => ({
    props: { timelineSteps },
    template: `
    <policy-studio-debug-timeline nbPoliciesRequest="3" nbPoliciesResponse="4">
      <policy-studio-debug-timeline-card *ngFor="let timelineStep of timelineSteps" [timelineStep]="timelineStep"></policy-studio-debug-timeline-card>
    </policy-studio-debug-timeline>
    `,
  }),
} as Meta;

export const Empty: Story = {};

export const ClientApp: Story = {
  args: {
    timelineSteps: [
      {
        mode: 'CLIENT_APP',
      },
      {
        mode: 'REQUEST_INPUT',
      },
      {
        mode: 'POLICY',
        executionTime: 32,
        flowName: 'Plan flow 1',
        policyName: 'IP Filtering Policy',
        icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAuNzEgMTEyLjgyIj48ZGVmcz48c3R5bGU+LmNscy0xe2ZpbGw6Izg2YzNkMDt9LmNscy0ye2ZpbGw6I2ZmZjt9LmNscy0ze2ZvbnQtc2l6ZToxMnB4O2ZpbGw6IzFkMWQxYjtmb250LWZhbWlseTpNeXJpYWRQcm8tUmVndWxhciwgTXlyaWFkIFBybzt9LmNscy00e2xldHRlci1zcGFjaW5nOi0wLjAzZW07fS5jbHMtNXtsZXR0ZXItc3BhY2luZzowZW07fS5jbHMtNntsZXR0ZXItc3BhY2luZzotMC4wMWVtO30uY2xzLTd7bGV0dGVyLXNwYWNpbmc6MGVtO30uY2xzLTh7bGV0dGVyLXNwYWNpbmc6MGVtO308L3N0eWxlPjwvZGVmcz48ZyBpZD0iQVBJIj48cGF0aCBjbGFzcz0iY2xzLTEiIGQ9Ik01MC4zNSwxMy4zN2E0Myw0MywwLDEsMCw0My4wNSw0M0E0Myw0MywwLDAsMCw1MC4zNSwxMy4zN1oiLz48cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Ik03MS43LDQwLjgxSDQ2LjU2YS44Ni44NiwwLDAsMSwwLTEuNzJINzEuN2EuODYuODYsMCwwLDEsMCwxLjcyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTM2LjU4LDQwLjgxSDI5YS44Ni44NiwwLDAsMSwwLTEuNzJoNy41N2EuODYuODYsMCwwLDEsMCwxLjcyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTcxLjcsNTcuMjZINjYuMzJhLjg2Ljg2LDAsMSwxLDAtMS43MUg3MS43YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNTYuMzQsNTcuMjZIMjlhLjg2Ljg2LDAsMSwxLDAtMS43MUg1Ni4zNGEuODYuODYsMCwxLDEsMCwxLjcxWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTcxLjcsNzMuNzJINTUuMzVhLjg2Ljg2LDAsMSwxLDAtMS43MUg3MS43YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNDUuMzYsNzMuNzJIMjlBLjg2Ljg2LDAsMSwxLDI5LDcySDQ1LjM2YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNDEuNTcsNDUuOEE1Ljg1LDUuODUsMCwxLDEsNDcuNDIsNDAsNS44Niw1Ljg2LDAsMCwxLDQxLjU3LDQ1LjhabTAtMTBBNC4xMyw0LjEzLDAsMSwwLDQ1LjcsNDAsNC4xMyw0LjEzLDAsMCwwLDQxLjU3LDM1LjgyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTYxLjMzLDYyLjI2YTUuODUsNS44NSwwLDEsMSw1Ljg1LTUuODZBNS44Niw1Ljg2LDAsMCwxLDYxLjMzLDYyLjI2Wm0wLTEwYTQuMTQsNC4xNCwwLDEsMCw0LjEzLDQuMTNBNC4xMyw0LjEzLDAsMCwwLDYxLjMzLDUyLjI3WiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTUwLjM2LDc4LjcxYTUuODUsNS44NSwwLDEsMSw1Ljg1LTUuODRBNS44Niw1Ljg2LDAsMCwxLDUwLjM2LDc4LjcxWm0wLTEwYTQuMTQsNC4xNCwwLDEsMCw0LjEzLDQuMTVBNC4xNCw0LjE0LDAsMCwwLDUwLjM2LDY4LjcyWiIvPjwvZz48ZyBpZD0iSE9TVElORyI+PHRleHQgY2xhc3M9ImNscy0zIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxNy45MyAtNC45OCkiPklQIDx0c3BhbiBjbGFzcz0iY2xzLTQiIHg9IjExLjgiIHk9IjAiPkY8L3RzcGFuPjx0c3BhbiBjbGFzcz0iY2xzLTUiIHg9IjE3LjMzIiB5PSIwIj5pbDwvdHNwYW4+PHRzcGFuIGNsYXNzPSJjbHMtNiIgeD0iMjIuOTciIHk9IjAiPnQ8L3RzcGFuPjx0c3BhbiBjbGFzcz0iY2xzLTciIHg9IjI2Ljg3IiB5PSIwIj5lPC90c3Bhbj48dHNwYW4gY2xhc3M9ImNscy04IiB4PSIzMi44OCIgeT0iMCI+cjwvdHNwYW4+PHRzcGFuIHg9IjM2Ljg1IiB5PSIwIj5pbmc8L3RzcGFuPjwvdGV4dD48L2c+PC9zdmc+',
      },
      {
        mode: 'REQUEST_OUTPUT',
      },
      {
        mode: 'BACKEND_TARGET',
      },
      {
        mode: 'RESPONSE_INPUT',
      },
      {
        mode: 'POLICY',
        executionTime: 32,
        flowName: 'Plan flow 1',
        policyName: 'IP Filtering Policy',
        icon: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDAuNzEgMTEyLjgyIj48ZGVmcz48c3R5bGU+LmNscy0xe2ZpbGw6Izg2YzNkMDt9LmNscy0ye2ZpbGw6I2ZmZjt9LmNscy0ze2ZvbnQtc2l6ZToxMnB4O2ZpbGw6IzFkMWQxYjtmb250LWZhbWlseTpNeXJpYWRQcm8tUmVndWxhciwgTXlyaWFkIFBybzt9LmNscy00e2xldHRlci1zcGFjaW5nOi0wLjAzZW07fS5jbHMtNXtsZXR0ZXItc3BhY2luZzowZW07fS5jbHMtNntsZXR0ZXItc3BhY2luZzotMC4wMWVtO30uY2xzLTd7bGV0dGVyLXNwYWNpbmc6MGVtO30uY2xzLTh7bGV0dGVyLXNwYWNpbmc6MGVtO308L3N0eWxlPjwvZGVmcz48ZyBpZD0iQVBJIj48cGF0aCBjbGFzcz0iY2xzLTEiIGQ9Ik01MC4zNSwxMy4zN2E0Myw0MywwLDEsMCw0My4wNSw0M0E0Myw0MywwLDAsMCw1MC4zNSwxMy4zN1oiLz48cGF0aCBjbGFzcz0iY2xzLTIiIGQ9Ik03MS43LDQwLjgxSDQ2LjU2YS44Ni44NiwwLDAsMSwwLTEuNzJINzEuN2EuODYuODYsMCwwLDEsMCwxLjcyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTM2LjU4LDQwLjgxSDI5YS44Ni44NiwwLDAsMSwwLTEuNzJoNy41N2EuODYuODYsMCwwLDEsMCwxLjcyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTcxLjcsNTcuMjZINjYuMzJhLjg2Ljg2LDAsMSwxLDAtMS43MUg3MS43YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNTYuMzQsNTcuMjZIMjlhLjg2Ljg2LDAsMSwxLDAtMS43MUg1Ni4zNGEuODYuODYsMCwxLDEsMCwxLjcxWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTcxLjcsNzMuNzJINTUuMzVhLjg2Ljg2LDAsMSwxLDAtMS43MUg3MS43YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNDUuMzYsNzMuNzJIMjlBLjg2Ljg2LDAsMSwxLDI5LDcySDQ1LjM2YS44Ni44NiwwLDEsMSwwLDEuNzFaIi8+PHBhdGggY2xhc3M9ImNscy0yIiBkPSJNNDEuNTcsNDUuOEE1Ljg1LDUuODUsMCwxLDEsNDcuNDIsNDAsNS44Niw1Ljg2LDAsMCwxLDQxLjU3LDQ1LjhabTAtMTBBNC4xMyw0LjEzLDAsMSwwLDQ1LjcsNDAsNC4xMyw0LjEzLDAsMCwwLDQxLjU3LDM1LjgyWiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTYxLjMzLDYyLjI2YTUuODUsNS44NSwwLDEsMSw1Ljg1LTUuODZBNS44Niw1Ljg2LDAsMCwxLDYxLjMzLDYyLjI2Wm0wLTEwYTQuMTQsNC4xNCwwLDEsMCw0LjEzLDQuMTNBNC4xMyw0LjEzLDAsMCwwLDYxLjMzLDUyLjI3WiIvPjxwYXRoIGNsYXNzPSJjbHMtMiIgZD0iTTUwLjM2LDc4LjcxYTUuODUsNS44NSwwLDEsMSw1Ljg1LTUuODRBNS44Niw1Ljg2LDAsMCwxLDUwLjM2LDc4LjcxWm0wLTEwYTQuMTQsNC4xNCwwLDEsMCw0LjEzLDQuMTVBNC4xNCw0LjE0LDAsMCwwLDUwLjM2LDY4LjcyWiIvPjwvZz48ZyBpZD0iSE9TVElORyI+PHRleHQgY2xhc3M9ImNscy0zIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxNy45MyAtNC45OCkiPklQIDx0c3BhbiBjbGFzcz0iY2xzLTQiIHg9IjExLjgiIHk9IjAiPkY8L3RzcGFuPjx0c3BhbiBjbGFzcz0iY2xzLTUiIHg9IjE3LjMzIiB5PSIwIj5pbDwvdHNwYW4+PHRzcGFuIGNsYXNzPSJjbHMtNiIgeD0iMjIuOTciIHk9IjAiPnQ8L3RzcGFuPjx0c3BhbiBjbGFzcz0iY2xzLTciIHg9IjI2Ljg3IiB5PSIwIj5lPC90c3Bhbj48dHNwYW4gY2xhc3M9ImNscy04IiB4PSIzMi44OCIgeT0iMCI+cjwvdHNwYW4+PHRzcGFuIHg9IjM2Ljg1IiB5PSIwIj5pbmc8L3RzcGFuPjwvdGV4dD48L2c+PC9zdmc+',
      },
      {
        mode: 'RESPONSE_OUTPUT',
      },
    ] as TimelineStep[],
  },
};
