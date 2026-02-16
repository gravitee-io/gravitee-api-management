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

import { ApiRuntimeLogsQuickFiltersComponent } from './api-runtime-logs-quick-filters.component';
import { ApiRuntimeLogsQuickFiltersModule } from './api-runtime-logs-quick-filters.module';

import { QuickFiltersStoreService } from '../../services';
import { ApplicationService } from '../../../../../../services-ngx/application.service';
import { fakeApplication } from '../../../../../../entities/application/Application.fixture';
import { fakePagedResult, fakePlanV4 } from '../../../../../../entities/management-api-v2';
import { ApiPlanV2Service } from '../../../../../../services-ngx/api-plan-v2.service';

const applications = [
  fakeApplication({ id: '1', name: 'app 1', owner: { displayName: 'owner 1' } }),
  fakeApplication({ id: '2', name: 'app 2', owner: { displayName: 'owner 2' } }),
  fakeApplication({ id: '3', name: 'app 3', owner: { displayName: 'owner 3' } }),
];

const plans = [fakePlanV4({ id: '1', name: 'plan 1' }), fakePlanV4({ id: '2', name: 'plan 2' }), fakePlanV4({ id: '3', name: 'plan 3' })];

export default {
  title: 'API / Logs / Connections / Quick filters',
  component: ApiRuntimeLogsQuickFiltersComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiRuntimeLogsQuickFiltersModule, BrowserAnimationsModule],
      providers: [
        {
          provide: ApplicationService,
          useValue: { list: () => of(fakePagedResult(applications)), findById: (id: string) => applications.find(app => app.id === id) },
        },
        {
          provide: ApiPlanV2Service,
          useValue: { list: () => of(fakePagedResult(plans)) },
        },
        QuickFiltersStoreService,
      ],
    }),
  ],
  argTypes: {},
  render: args => ({
    template: `
      <div style="width: 1000px">
        <h4>Toggle loading value in the controls panel to enable or disable the form</h4>
        <api-runtime-logs-quick-filters [loading]="loading" [plans]="plans" [initialValues]="initialValues">
        </api-runtime-logs-quick-filters>
      </div>
    `,
    props: args,
  }),
} as Meta;

export const Default: StoryObj = {};
Default.args = {
  initialValues: { applications: undefined, plans: undefined },
  plans,
  loading: true,
};
