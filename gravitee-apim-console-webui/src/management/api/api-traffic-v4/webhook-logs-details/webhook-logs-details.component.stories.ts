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
import { Meta, moduleMetadata, StoryObj, applicationConfig } from '@storybook/angular';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { importProvidersFrom } from '@angular/core';
import { GioMonacoEditorModule } from '@gravitee/ui-particles-angular';

import { WebhookLogsDetailsComponent } from './webhook-logs-details.component';

const createActivatedRoute = (requestId: string) =>
  ({
    snapshot: {
      params: { requestId },
    },
  }) as unknown as ActivatedRoute;

export default {
  title: 'API / Traffic / Webhook Logs / Details',
  component: WebhookLogsDetailsComponent,
  decorators: [
    applicationConfig({
      providers: [importProvidersFrom(GioMonacoEditorModule.forRoot({ theme: 'vs', baseUrl: '.' }))],
    }),
    moduleMetadata({
      imports: [WebhookLogsDetailsComponent, BrowserAnimationsModule, RouterTestingModule],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 1200px; margin: 24px; padding: 24px; background-color: #f4f6fb; min-height: 100vh;">
        <webhook-logs-details></webhook-logs-details>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Status200: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: createActivatedRoute('req-2'),
        },
      ],
    }),
  ],
};

export const Status500: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: createActivatedRoute('req-1'),
        },
      ],
    }),
  ],
};

export const Status0: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: createActivatedRoute('req-3'),
        },
      ],
    }),
  ],
};
