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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { of } from 'rxjs';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { action } from '@storybook/addon-actions';
import { MatDialog } from '@angular/material/dialog';

import { WebhookLogsComponent } from './webhook-logs.component';

import { ApiV4 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

const apiMock: ApiV4 = {
  id: 'api-webhook-demo',
  name: 'Acme Webhook Traffic',
  analytics: {
    enabled: true,
    logging: {
      mode: {
        entrypoint: true,
        endpoint: true,
      },
    },
  },
} as ApiV4;

const apiServiceMock: Partial<ApiV2Service> = {
  get: () => of(apiMock),
};

class MatDialogMock {
  open() {
    action('open-settings-dialog')();
    return {
      afterClosed: () => of({ saved: false }),
    };
  }
}

const activatedRouteMock = {
  snapshot: {
    params: { apiId: 'api-webhook-demo' },
    queryParams: {},
    queryParamMap: convertToParamMap({}),
  },
};

const routerMock: Partial<Router> = {
  navigate: (...args: Parameters<Router['navigate']>) => {
    action('router.navigate')(args);
    return Promise.resolve(true);
  },
};

const meta: Meta<WebhookLogsComponent> = {
  title: 'API / Logs / Webhooks / Page',
  decorators: [
    moduleMetadata({
      imports: [WebhookLogsComponent],
      providers: [
        { provide: ApiV2Service, useValue: apiServiceMock },
        { provide: MatDialog, useClass: MatDialogMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: Router, useValue: routerMock },
      ],
    }),
  ],
  parameters: {
    layout: 'fullscreen',
  },
  render: () => ({
    template: `
      <div style="min-height: 800px; padding: 24px; background-color: #f4f6fb;">
        <webhook-logs></webhook-logs>
      </div>
    `,
  }),
};

export default meta;

export const Default: StoryObj<WebhookLogsComponent> = {};
