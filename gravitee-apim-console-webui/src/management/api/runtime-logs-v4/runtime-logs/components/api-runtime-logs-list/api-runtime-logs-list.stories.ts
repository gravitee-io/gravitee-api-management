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
import { Meta, moduleMetadata } from '@storybook/angular';
import { Story } from '@storybook/angular/dist/ts3.9/client/preview/types-7-0';

import { ApiRuntimeLogsListComponent } from './api-runtime-logs-list.component';
import { ApiRuntimeLogsListModule } from './api-runtime-logs-list.module';

import { fakeConnectionLog } from '../../../../../../entities/management-api-v2/log/connectionLog.fixture';

const logs = [fakeConnectionLog(), fakeConnectionLog(), fakeConnectionLog()];

export default {
  title: 'API / Logs / Runtime logs list',
  component: ApiRuntimeLogsListComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsListModule],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `
      <div style="width: 800px">
        <api-runtime-logs-list [logs]="logs" [pagination]="pagination"></api-runtime-logs-list>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const FirstPage: Story = {};
FirstPage.args = {
  pagination: { page: 1, perPage: 10, pageCount: 5, pageItemsCount: 3, totalCount: 30 },
  logs,
};
