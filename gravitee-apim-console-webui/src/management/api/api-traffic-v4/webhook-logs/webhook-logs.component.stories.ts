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
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { Component, Input } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';

import { WebhookLogsComponent } from './webhook-logs.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { fakeApiV4 } from '../../../../entities/management-api-v2/api/api.fixture';
import { MenuItemHeader } from '../../api-navigation/MenuGroupItem';

@Component({
  selector: 'api-navigation-header',
  template: `
    <div style="padding: 16px 24px; background: white; border-bottom: 1px solid #e0e0e0; margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2 style="margin: 0; font-size: 20px; font-weight: 500;">{{ menuItemHeader?.title }}</h2>
        <div><ng-content select="[actionButtons]"></ng-content></div>
      </div>
    </div>
  `,
  standalone: true,
})
class MockApiNavigationHeaderComponent {
  @Input() menuItemHeader?: MenuItemHeader;
}

const mockApiId = 'test-api-id';

const mockApi = fakeApiV4({
  id: mockApiId,
  name: 'Webhook Test API',
  analytics: {
    enabled: true,
    logging: {
      mode: {
        endpoint: true,
        entrypoint: true,
      },
      phase: {
        request: true,
        response: true,
      },
      content: {
        messagePayload: true,
        messageHeaders: true,
        messageMetadata: true,
        headers: true,
        payload: true,
      },
    },
  },
});

export default {
  title: 'API / Logs / Webhooks / Full View',
  component: WebhookLogsComponent,
  decorators: [
    moduleMetadata({
      imports: [WebhookLogsComponent, MockApiNavigationHeaderComponent],
      providers: [
        {
          provide: ApiV2Service,
          useValue: { get: () => of(mockApi) },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: mockApiId },
              queryParams: {},
            },
          },
        },
        {
          provide: MatDialog,
          useValue: { open: () => ({}) },
        },
      ],
    }),
    applicationConfig({
      providers: [provideAnimations(), provideRouter([])],
    }),
  ],
  render: (args) => ({
    template: `
      <div style="width: 100%; background: #fafafa; min-height: 100vh;">
        <webhook-logs></webhook-logs>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {};
Default.parameters = {
  docs: {
    description: {
      story:
        'Shows the complete webhook logs view with 5 sample webhook deliveries including both successful (200) and failed (500) requests. The table displays timestamp, status, callback URL, application, and duration for each webhook.',
    },
  },
};

export const WithFilters: StoryObj = {
  decorators: [
    moduleMetadata({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              params: { apiId: mockApiId },
              queryParams: {
                search: 'finance',
                statuses: '500',
                applicationIds: 'app-2',
                timeframe: '-1h',
              },
            },
          },
        },
      ],
    }),
  ],
  parameters: {
    docs: {
      description: {
        story:
          'Demonstrates the filtering capabilities with pre-applied filters: searching for "finance", filtering by 500 status code, specific application (app-2), and a 1-hour timeframe. This should show only the Acme Finance Service webhook with the 500 error.',
      },
    },
  },
};
