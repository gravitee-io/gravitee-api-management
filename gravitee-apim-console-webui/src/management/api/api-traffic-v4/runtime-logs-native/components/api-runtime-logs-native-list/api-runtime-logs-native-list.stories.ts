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

import { ApiRuntimeLogsNativeListComponent } from './api-runtime-logs-native-list.component';

import { fakeNativeApiLog, fakePlanV4 } from '../../../../../../entities/management-api-v2';
import { fakeApplication } from '../../../../../../entities/application/Application.fixture';

const APPS = [fakeApplication({ id: 'app-1', name: 'Order Service' }), fakeApplication({ id: 'app-2', name: 'Payment Service' })];

const PLANS = [fakePlanV4({ id: 'plan-1', name: 'Free' }), fakePlanV4({ id: 'plan-2', name: 'Premium' })];

const logs = [
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
  fakeNativeApiLog({
    requestId: 'req-4',
    timestamp: '2026-05-10T10:35:21.000Z',
    applicationId: 'app-2',
    planId: 'plan-1',
    clientIdentifier: 'kafka-client-batch-job',
    connectionStatus: 'INTERNAL_ERROR',
    connectionDurationMs: 27,
  }),
];

export default {
  title: 'API / Logs / Native / List',
  component: ApiRuntimeLogsNativeListComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsNativeListComponent],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 1200px">
        <api-runtime-logs-native-list
          [logs]="logs"
          [pagination]="pagination"
          [applications]="applications"
          [plans]="plans"
        ></api-runtime-logs-native-list>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Empty: StoryObj = {};
Empty.args = {
  pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 0, totalCount: 0 },
  logs: [],
  applications: APPS,
  plans: PLANS,
};

export const FirstPage: StoryObj = {};
FirstPage.args = {
  pagination: { page: 1, perPage: 10, pageCount: 3, pageItemsCount: 4, totalCount: 24 },
  logs,
  applications: APPS,
  plans: PLANS,
};

export const UnresolvedNames: StoryObj = {};
UnresolvedNames.args = {
  pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 4, totalCount: 4 },
  logs,
  applications: [],
  plans: [],
};
