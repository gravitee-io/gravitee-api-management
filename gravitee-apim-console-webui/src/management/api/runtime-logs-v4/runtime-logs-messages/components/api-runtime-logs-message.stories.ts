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
import { MatCardModule } from '@angular/material/card';

import { ApiRuntimeLogsMessageModule } from './api-runtime-logs-message.module';

import { ApiRuntimeLogsMessagesComponent } from '../api-runtime-logs-messages.component';
import { fakeMessageLog } from '../../../../../entities/management-api-v2/log/messageLog.fixture';

const messageLog = fakeMessageLog({ connectorId: 'kafka' });
const connectorIcon = 'gio:kafka';

export default {
  title: 'API / Logs / Messages / Item',
  component: ApiRuntimeLogsMessagesComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsMessageModule, MatCardModule],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `
      <mat-card style="width: 800px">
        <api-runtime-logs-message [messageLog]="messageLog" [connectorIcon]="connectorIcon"></api-runtime-logs-message>
      </mat-card>
    `,
    props: args,
  }),
} as Meta;

export const Default: Story = {};
Default.args = {
  messageLog,
  connectorIcon,
};
