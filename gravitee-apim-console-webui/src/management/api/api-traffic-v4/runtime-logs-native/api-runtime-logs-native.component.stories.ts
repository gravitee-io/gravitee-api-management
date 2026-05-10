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
import { action } from 'storybook/actions';

import { ApiRuntimeLogsNativeComponent } from './api-runtime-logs-native.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { ApiNativeLogsV2Service } from '../../../../services-ngx/api-native-logs-v2.service';
import { ApiV4, fakeApiV4, fakeNativeApiLog, fakeNativeApiLogsResponse, fakePlanV4 } from '../../../../entities/management-api-v2';
import { fakeApplication } from '../../../../entities/application/Application.fixture';
import { PagedResult } from '../../../../entities/pagedResult';

const APP_1 = fakeApplication({ id: 'app-1', name: 'Order Service' });
const APP_2 = fakeApplication({ id: 'app-2', name: 'Payment Service' });

const PLAN_1 = fakePlanV4({ id: 'plan-1', name: 'Free' });
const PLAN_2 = fakePlanV4({ id: 'plan-2', name: 'Premium' });

const apiMock: ApiV4 = fakeApiV4({
  id: 'api-native-demo',
  name: 'Acme Native Kafka',
  type: 'NATIVE',
  analytics: { enabled: true, reporterMetricsEnabled: true },
});

const apiDisabledReportingMock: ApiV4 = fakeApiV4({
  id: 'api-native-demo',
  name: 'Acme Native Kafka',
  type: 'NATIVE',
  analytics: { enabled: true, reporterMetricsEnabled: false },
});

const populatedLogs = fakeNativeApiLogsResponse({
  data: [
    fakeNativeApiLog({
      requestId: 'req-1',
      timestamp: '2026-05-10T10:30:15.000Z',
      applicationId: 'app-1',
      planId: 'plan-1',
      clientIdentifier: 'kafka-client-orders-prod',
      connectionStatus: 'CONNECTED',
      connectionDurationMs: 4200,
    }),
    fakeNativeApiLog({
      requestId: 'req-2',
      timestamp: '2026-05-10T10:31:42.000Z',
      applicationId: 'app-2',
      planId: 'plan-2',
      clientIdentifier: 'kafka-client-payments',
      connectionStatus: 'CONNECTION_ERROR',
      connectionDurationMs: 134,
    }),
    fakeNativeApiLog({
      requestId: 'req-3',
      timestamp: '2026-05-10T10:33:01.000Z',
      applicationId: 'app-1',
      planId: 'plan-2',
      clientIdentifier: 'kafka-client-orders-staging',
      connectionStatus: 'SESSION_ERROR',
      connectionDurationMs: 982347,
    }),
  ],
  pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 3, totalCount: 23 },
});

const emptyLogs = fakeNativeApiLogsResponse({
  data: [],
  pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 0, totalCount: 0 },
});

const apiServiceMock = (api: ApiV4) =>
  ({
    get: () => of(api),
  }) as unknown as ApiV2Service;

const planServiceMock = {
  list: () =>
    of({
      data: [PLAN_1, PLAN_2],
      pagination: { page: 1, perPage: 9999, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
    }),
} as unknown as ApiPlanV2Service;

const applicationServiceMock = {
  list: () => {
    const result = new PagedResult<typeof APP_1>();
    result.data = [APP_1, APP_2];
    result.page = { current: 1, size: 25, per_page: 25, total_pages: 1, total_elements: 2 };
    return of(result);
  },
  findByIds: () => {
    const result = new PagedResult<typeof APP_1>();
    result.data = [APP_1, APP_2];
    result.page = { current: 1, size: 2, per_page: 2, total_pages: 1, total_elements: 2 };
    return of(result);
  },
} as unknown as ApplicationService;

const logsServiceMock = (response: typeof populatedLogs) =>
  ({
    searchConnectionLogs: () => of(response),
  }) as unknown as ApiNativeLogsV2Service;

const activatedRouteMock = {
  snapshot: {
    params: { apiId: 'api-native-demo' },
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

const buildProviders = (api: ApiV4, response: typeof populatedLogs) => [
  { provide: ApiV2Service, useValue: apiServiceMock(api) },
  { provide: ApiPlanV2Service, useValue: planServiceMock },
  { provide: ApplicationService, useValue: applicationServiceMock },
  { provide: ApiNativeLogsV2Service, useValue: logsServiceMock(response) },
  { provide: ActivatedRoute, useValue: activatedRouteMock },
  { provide: Router, useValue: routerMock },
];

const meta: Meta<ApiRuntimeLogsNativeComponent> = {
  title: 'API / Logs / Native / Page',
  parameters: { layout: 'fullscreen' },
  render: () => ({
    template: `
      <div style="min-height: 900px; padding: 24px; background-color: #f4f6fb;">
        <api-runtime-logs-native></api-runtime-logs-native>
      </div>
    `,
  }),
};

export default meta;

export const WithRows: StoryObj<ApiRuntimeLogsNativeComponent> = {
  decorators: [moduleMetadata({ imports: [ApiRuntimeLogsNativeComponent], providers: buildProviders(apiMock, populatedLogs) })],
};

export const Empty: StoryObj<ApiRuntimeLogsNativeComponent> = {
  decorators: [moduleMetadata({ imports: [ApiRuntimeLogsNativeComponent], providers: buildProviders(apiMock, emptyLogs) })],
};

export const ReportingDisabled: StoryObj<ApiRuntimeLogsNativeComponent> = {
  decorators: [
    moduleMetadata({ imports: [ApiRuntimeLogsNativeComponent], providers: buildProviders(apiDisabledReportingMock, populatedLogs) }),
  ],
};
