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
import '@gravitee/ui-components/wc/gv-code';
import '@gravitee/ui-components/wc/gv-icon';

import { PolicyStudioDebugTimelineCardComponent, TimelineStep } from './policy-studio-debug-timeline-card.component';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Timeline card',
  component: PolicyStudioDebugTimelineCardComponent,
  decorators: [
    moduleMetadata({
      imports: [CommonModule, BrowserAnimationsModule, MatIconModule, GioIconsModule],
    }),
  ],
  argTypes: {
    timelineStep: {
      control: {
        type: 'object',
      },
    },
  },
  args: {
    timelineStep: null,
  },
  render: ({ timelineStep }) => ({
    props: { timelineStep },
    template: '<policy-studio-debug-timeline-card [timelineStep]="timelineStep"></policy-studio-debug-timeline-card>',
  }),
} as Meta;

export const Empty: Story = {};

export const ClientApp: Story = {
  args: {
    timelineStep: {
      mode: 'CLIENT_APP',
    } as TimelineStep,
  },
};

export const BackendTarget: Story = {
  args: {
    timelineStep: {
      mode: 'BACKEND_TARGET',
    } as TimelineStep,
  },
};
