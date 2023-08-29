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

import { ApiRuntimeLogsListRowComponent } from './api-runtime-logs-list-row.component';
import { ApiRuntimeLogsListRowModule } from './api-runtime-logs-list-row.module';

import { ConnectionLog } from '../../../../../../entities/management-api-v2';
import { fakeConnectionLog } from '../../../../../../entities/management-api-v2/log/connectionLog.fixture';

const log: ConnectionLog = fakeConnectionLog();

export default {
  title: 'API / Logs / Runtime logs list row',
  component: ApiRuntimeLogsListRowComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsListRowModule],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `
      <div style="width: 870px">
        <api-runtime-logs-list-row [log]="log" [index]="index"></api-runtime-logs-list-row>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const First: Story = {};
First.args = { log, index: 0 };

export const NthElement: Story = {};
NthElement.args = { log, index: 1 };
