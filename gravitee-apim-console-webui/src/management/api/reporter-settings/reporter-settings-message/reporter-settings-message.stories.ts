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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ReporterSettingsMessageComponent } from './reporter-settings-message.component';

import { ApiV4, fakeApiV4 } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GioPermissionService, GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { PortalConfigurationService } from '../../../../services-ngx/portal-configuration.service';

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
  component: ReporterSettingsMessageComponent,
  decorators: [
    moduleMetadata({
      imports: [ReporterSettingsMessageComponent, BrowserAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: api.id } } } },
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
          provide: PortalConfigurationService,
          useValue: {
            get: () => {
              return of({
                logging: {
                  messageSampling: {
                    probabilistic: {
                      limit: 0.52,
                      default: 0.33322,
                    },
                    count: {
                      limit: 40,
                      default: 666,
                    },
                    temporal: {
                      limit: 'PT10S',
                      default: 'PT10S',
                    },
                  },
                },
              });
            },
          },
        },
        {
          provide: GioTestingPermissionProvider,
          useValue: ['api-definition-u'],
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
  render: args => ({
    template: `
      <div style="width: 870px">
        <reporter-settings-message [api]="api"></reporter-settings-message>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = { api };
