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
import { of } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ApiRuntimeLogsMessageSettingsModule } from './api-runtime-logs-message-settings.module';
import { ApiRuntimeLogsMessageSettingsComponent } from './api-runtime-logs-message-settings.component';

import { ApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';
import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { CurrentUserService } from '../../../../../services-ngx/current-user.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { User } from '../../../../../entities/user';

const currentUser = new User();
currentUser.userPermissions = ['api-definition-u'];

const api = fakeApiV4({
  analytics: {
    enabled: true,
    logging: {
      mode: { entrypoint: false, endpoint: false },
      phase: { request: false, response: false },
      content: { messagePayload: false, messageHeaders: false, messageMetadata: false, headers: false },
      condition: null,
      messageCondition: null,
    },
    sampling: {
      type: 'COUNT',
      value: '42',
    },
  },
});

export default {
  title: 'API / Logs / Settings / Message',
  component: ApiRuntimeLogsMessageSettingsComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsMessageSettingsModule, BrowserAnimationsModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: api.id } },
        {
          provide: ApiV2Service,
          useValue: {
            get: (_: string) => {
              return of(api);
            },
            update: (_: ApiV4) => {
              return of(api);
            },
          },
        },
        {
          provide: CurrentUserService,
          useValue: { currentUser },
        },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => true,
          },
        },
      ],
    }),
  ],
  argTypes: {},
  render: (args) => ({
    template: `
      <div style="width: 870px">
        <api-runtime-logs-message-settings [api]="api"></api-runtime-logs-message-settings>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: Story = {};
Default.args = {
  api,
};
