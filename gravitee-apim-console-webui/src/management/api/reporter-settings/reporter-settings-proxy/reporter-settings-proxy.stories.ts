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
import { ActivatedRoute } from '@angular/router';

import { ReporterSettingsProxyComponent } from './reporter-settings-proxy.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, fakeProxyApiV4 } from '../../../../entities/management-api-v2';

const api = fakeProxyApiV4();

export default {
  title: 'API / Logs / Settings / Proxy',
  component: ReporterSettingsProxyComponent,
  decorators: [
    moduleMetadata({
      imports: [ReporterSettingsProxyComponent],
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
      ],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 870px">
        <reporter-settings-proxy [api]="api"></reporter-settings-proxy>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = { api };
