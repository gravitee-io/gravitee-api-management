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

import { ApiRuntimeLogsProxyComponent } from './api-runtime-logs-proxy.component';

import { ApiAnalyticsV2Service } from '../../../../../../services-ngx/api-analytics-v2.service';
import { fakeApiMetricResponse } from '../../../../../../entities/management-api-v2/analytics/apiMetricsDetailResponse.fixture';
import { ApiLogsV2Service } from '../../../../../../services-ngx/api-logs-v2.service';
import { fakeConnectionLogDetail } from '../../../../../../entities/management-api-v2';

export default {
  title: 'API / Logs / Details / Proxy Connection Log',
  component: ApiRuntimeLogsProxyComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsProxyComponent],
      providers: [
        {
          provide: ApiAnalyticsV2Service,
          useValue: {
            getApiMetricsDetail: (apiId: string, requestId: string) => of(fakeApiMetricResponse({ apiId, requestId })),
          },
        },
        {
          provide: ApiLogsV2Service,
          useValue: {
            searchConnectionLogDetail: (apiId: string, requestId: string) => of(fakeConnectionLogDetail({ apiId, requestId })),
          },
        },
      ],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    styles: [
      `
      .container {
        display: flex;
        flex-direction: column;
        flex: 1 1 auto;
        width: 100%;
      }

      api-runtime-logs-proxy {
        width: 1400px;
        margin: 0;
        padding: 0 50px;
      }
    `,
    ],
    template: `
      <div class="container">
        <api-runtime-logs-proxy></api-runtime-logs-proxy>,
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
