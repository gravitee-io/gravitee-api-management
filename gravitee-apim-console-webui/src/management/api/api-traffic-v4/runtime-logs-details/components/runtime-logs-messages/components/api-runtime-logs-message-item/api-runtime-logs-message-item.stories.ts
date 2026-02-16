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

import { ApiRuntimeLogsMessageItemModule } from './api-runtime-logs-message-item.module';
import { ApiRuntimeLogsMessageItemComponent } from './api-runtime-logs-message-item.component';

import { fakeAggregatedMessageLog, fakeMessage } from '../../../../../../../../entities/management-api-v2';

export default {
  title: 'API / Logs / Details/ Messages item',
  component: ApiRuntimeLogsMessageItemComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsMessageItemModule],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 800px">
        <api-runtime-logs-message-item [messageLog]="messageLog" [entrypointConnectorIcon]="entrypointConnectorIcon" [endpointConnectorIcon]="endpointConnectorIcon"></api-runtime-logs-message-item>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const WithEntrypointAndEndpoint: StoryObj = {};
WithEntrypointAndEndpoint.args = {
  messageLog: fakeAggregatedMessageLog({
    entrypoint: fakeMessage({ connectorId: 'http-get' }),
    endpoint: fakeMessage({ connectorId: 'kafka' }),
  }),
  entrypointConnectorIcon: 'gio:http-get',
  endpointConnectorIcon: 'gio:kafka',
};

export const WithEntrypointOnly: StoryObj = {};
WithEntrypointOnly.args = {
  messageLog: fakeAggregatedMessageLog({
    entrypoint: fakeMessage({ connectorId: 'http-get' }),
    endpoint: undefined,
  }),
  entrypointConnectorIcon: 'gio:http-get',
};

export const WithEndpointOnly: StoryObj = {};
WithEndpointOnly.args = {
  messageLog: fakeAggregatedMessageLog({
    endpoint: fakeMessage({ connectorId: 'kafka' }),
    entrypoint: undefined,
  }),
  endpointConnectorIcon: 'gio:kafka',
};

export const WithNoContent: StoryObj = {};
WithNoContent.args = {
  messageLog: fakeAggregatedMessageLog({
    entrypoint: fakeMessage({ connectorId: 'http-get', payload: undefined }),
    endpoint: fakeMessage({ connectorId: 'kafka', payload: undefined }),
  }),
  entrypointConnectorIcon: 'gio:http-get',
  endpointConnectorIcon: 'gio:kafka',
};

export const WithNoHeaders: StoryObj = {};
WithNoHeaders.args = {
  messageLog: fakeAggregatedMessageLog({
    entrypoint: fakeMessage({ connectorId: 'http-get', headers: undefined }),
    endpoint: fakeMessage({ connectorId: 'kafka', headers: undefined }),
  }),
  entrypointConnectorIcon: 'gio:http-get',
  endpointConnectorIcon: 'gio:kafka',
};

export const WithNoMetadata: StoryObj = {};
WithNoMetadata.args = {
  messageLog: fakeAggregatedMessageLog({
    entrypoint: fakeMessage({ connectorId: 'http-get', metadata: undefined }),
    endpoint: fakeMessage({ connectorId: 'kafka', metadata: undefined }),
  }),
  entrypointConnectorIcon: 'gio:http-get',
  endpointConnectorIcon: 'gio:kafka',
};
