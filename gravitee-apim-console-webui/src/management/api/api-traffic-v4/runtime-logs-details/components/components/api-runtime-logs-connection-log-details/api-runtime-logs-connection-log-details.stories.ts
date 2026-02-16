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

import { ApiRuntimeLogsConnectionLogDetailsModule } from './api-runtime-logs-connection-log-details.module';
import { ApiRuntimeLogsConnectionLogDetailsComponent } from './api-runtime-logs-connection-log-details.component';

import { fakeConnectionLogDetail } from '../../../../../../../entities/management-api-v2';

export default {
  title: 'API / Logs / Details / Connection Log',
  component: ApiRuntimeLogsConnectionLogDetailsComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsConnectionLogDetailsModule],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 800px">
      <api-runtime-logs-connection-log-details
          [connectionLogDetail]="connectionLogDetail"
        ></api-runtime-logs-connection-log-details>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {
  connectionLogDetail: fakeConnectionLogDetail(),
};
