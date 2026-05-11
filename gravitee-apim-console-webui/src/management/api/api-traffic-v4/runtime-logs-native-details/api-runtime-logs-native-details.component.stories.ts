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
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { ApiRuntimeLogsNativeDetailsComponent } from './api-runtime-logs-native-details.component';

import { ApiNativeLogsV2Service } from '../../../../services-ngx/api-native-logs-v2.service';
import { fakeNativeApiLog, NativeApiLog } from '../../../../entities/management-api-v2';

const activatedRoute = {
  snapshot: {
    params: { apiId: 'api-native-demo', requestId: 'req-demo' },
    queryParams: { from: '1000', to: '2000', period: '1h' },
  },
};

const stubService = (log: NativeApiLog | null | HttpErrorResponse) =>
  ({
    getConnectionLog: () =>
      log instanceof HttpErrorResponse || log == null
        ? throwError(() => log ?? new HttpErrorResponse({ status: 404, statusText: 'Not Found' }))
        : of(log),
  }) as unknown as ApiNativeLogsV2Service;

const meta: Meta<ApiRuntimeLogsNativeDetailsComponent> = {
  title: 'API / Logs / Native / Details',
  component: ApiRuntimeLogsNativeDetailsComponent,
  decorators: [moduleMetadata({ imports: [ApiRuntimeLogsNativeDetailsComponent] })],
  parameters: { layout: 'padded' },
  render: () => ({
    template: `
      <div style="min-width: 960px; padding: 24px; background-color: #f4f6fb;">
        <api-runtime-logs-native-details />
      </div>
    `,
  }),
};

export default meta;

export const Connected: StoryObj<ApiRuntimeLogsNativeDetailsComponent> = {
  decorators: [
    moduleMetadata({
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        {
          provide: ApiNativeLogsV2Service,
          useValue: stubService(fakeNativeApiLog({ connectionStatus: 'CONNECTED', clientId: 'kafka-consumer-1', brokerId: '0' })),
        },
      ],
    }),
  ],
};

export const ConnectionError: StoryObj<ApiRuntimeLogsNativeDetailsComponent> = {
  decorators: [
    moduleMetadata({
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        {
          provide: ApiNativeLogsV2Service,
          useValue: stubService(
            fakeNativeApiLog({
              connectionStatus: 'CONNECTION_ERROR',
              errorKey: 'AUTH_FAILED',
              errorMessage: 'SASL handshake failed: invalid credentials',
            }),
          ),
        },
      ],
    }),
  ],
};

export const NotFound: StoryObj<ApiRuntimeLogsNativeDetailsComponent> = {
  decorators: [
    moduleMetadata({
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: ApiNativeLogsV2Service, useValue: stubService(null) },
      ],
    }),
  ],
};

export const LoadFailed: StoryObj<ApiRuntimeLogsNativeDetailsComponent> = {
  decorators: [
    moduleMetadata({
      providers: [
        { provide: ActivatedRoute, useValue: activatedRoute },
        {
          provide: ApiNativeLogsV2Service,
          useValue: stubService(new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' })),
        },
      ],
    }),
  ],
};
