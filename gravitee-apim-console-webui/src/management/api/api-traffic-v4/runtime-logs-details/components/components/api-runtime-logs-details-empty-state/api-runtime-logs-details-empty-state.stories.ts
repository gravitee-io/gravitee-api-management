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

import { ApiRuntimeLogsDetailsEmptyStateModule } from './api-runtime-logs-details-empty-state.module';
import { ApiRuntimeLogsDetailsEmptyStateComponent } from './api-runtime-logs-details-empty-state.component';

export default {
  title: 'API / Logs / Details / Empty',
  component: ApiRuntimeLogsDetailsEmptyStateComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsDetailsEmptyStateModule],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 800px">
        <api-runtime-logs-details-empty-state></api-runtime-logs-details-empty-state>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {};
