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
import { of, NEVER } from 'rxjs';

import { ApiRuntimeLogsNativeSummaryComponent } from './api-runtime-logs-native-summary.component';

import { ApiNativeLogsV2Service } from '../../../../../../services-ngx/api-native-logs-v2.service';
import { NativeApiLogsSummary } from '../../../../../../entities/management-api-v2';

const NOW = Date.now();
const HOUR = 60 * 60 * 1000;

const populated: NativeApiLogsSummary = {
  countByConnectionStatus: { CONNECTED: 184, SESSION_ERROR: 32, CONNECTION_ERROR: 28, INTERNAL_ERROR: 4 },
};

const allZeros: NativeApiLogsSummary = {
  countByConnectionStatus: { CONNECTED: 0, SESSION_ERROR: 0, CONNECTION_ERROR: 0, INTERNAL_ERROR: 0 },
};

const partial: NativeApiLogsSummary = {
  countByConnectionStatus: { CONNECTED: 12 },
};

const stubLogsService = (response: NativeApiLogsSummary | null) =>
  ({
    searchSummary: () => (response == null ? NEVER : of(response)),
  }) as unknown as ApiNativeLogsV2Service;

const meta: Meta<ApiRuntimeLogsNativeSummaryComponent> = {
  title: 'API / Logs / Native / Summary',
  component: ApiRuntimeLogsNativeSummaryComponent,
  decorators: [moduleMetadata({ imports: [ApiRuntimeLogsNativeSummaryComponent] })],
  parameters: { layout: 'padded' },
  render: () => ({
    template: `
      <div style="min-width: 960px; padding: 24px; background-color: #f4f6fb;">
        <api-runtime-logs-native-summary [apiId]="apiId" [from]="from" [to]="to" />
      </div>
    `,
    props: { apiId: 'api-native-demo', from: NOW - HOUR, to: NOW },
  }),
};

export default meta;

export const Populated: StoryObj<ApiRuntimeLogsNativeSummaryComponent> = {
  decorators: [moduleMetadata({ providers: [{ provide: ApiNativeLogsV2Service, useValue: stubLogsService(populated) }] })],
};

export const AllZeros: StoryObj<ApiRuntimeLogsNativeSummaryComponent> = {
  decorators: [moduleMetadata({ providers: [{ provide: ApiNativeLogsV2Service, useValue: stubLogsService(allZeros) }] })],
};

export const OneNonZero: StoryObj<ApiRuntimeLogsNativeSummaryComponent> = {
  decorators: [moduleMetadata({ providers: [{ provide: ApiNativeLogsV2Service, useValue: stubLogsService(partial) }] })],
};

export const Loading: StoryObj<ApiRuntimeLogsNativeSummaryComponent> = {
  decorators: [moduleMetadata({ providers: [{ provide: ApiNativeLogsV2Service, useValue: stubLogsService(null) }] })],
};

export const NoTimeframe: StoryObj<ApiRuntimeLogsNativeSummaryComponent> = {
  decorators: [moduleMetadata({ providers: [{ provide: ApiNativeLogsV2Service, useValue: stubLogsService(populated) }] })],
  render: () => ({
    template: `
      <div style="min-width: 960px; padding: 24px; background-color: #f4f6fb;">
        <api-runtime-logs-native-summary [apiId]="apiId" [from]="null" [to]="null" />
      </div>
    `,
    props: { apiId: 'api-native-demo' },
  }),
};
