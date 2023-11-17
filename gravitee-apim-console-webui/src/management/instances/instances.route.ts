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
import { Ng2StateDeclaration } from '@uirouter/angular';

import { InstanceListComponent } from './instance-list/instance-list.component';
import { InstanceDetailsComponent } from './instance-details/instance-details.component';
import { InstanceDetailsEnvironmentComponent } from './instance-details/instance-details-environment/instance-details-environment.component';
import { InstanceDetailsMonitoringComponent } from './instance-details/instance-details-monitoring/instance-details-monitoring.component';

export const states: Ng2StateDeclaration[] = [
  {
    name: `management.instances-list`,
    url: '/instances',
    component: InstanceListComponent,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-instance-r'],
      },
      docs: {
        page: 'management-gateways',
      },
    },
  },
  {
    name: `management.instance`,
    url: '/instances/:instanceId',
    component: InstanceDetailsComponent,
    data: {
      useAngularMaterial: true,
      perms: {
        only: ['environment-instance-r'],
      },
    },
  },
  {
    name: `management.instance.environment`,
    url: '/environment',
    component: InstanceDetailsEnvironmentComponent,
    data: {
      docs: {
        page: 'management-gateway-environment',
      },
      useAngularMaterial: true,
    },
  },
  {
    name: `management.instance.monitoring`,
    url: '/monitoring',
    component: InstanceDetailsMonitoringComponent,
    data: {
      docs: {
        page: 'management-gateway-monitoring',
      },
      useAngularMaterial: true,
    },
  },
];
