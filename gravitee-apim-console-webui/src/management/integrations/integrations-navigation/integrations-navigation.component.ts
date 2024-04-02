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
import { Component } from '@angular/core';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { IntegrationNavigationItem } from '../integrations.model';

@Component({
  selector: 'app-integrations-navigation',
  templateUrl: './integrations-navigation.component.html',
  styleUrls: ['./integrations-navigation.component.scss'],
})
export class IntegrationsNavigationComponent {
  public items: IntegrationNavigationItem[] = [
    {
      routerLink: `.`,
      displayName: 'Overview',
      permissions: [],
      icon: 'info',
    },
    // toDo: uncomment when pages are ready
    // {
    //   routerLink: `./agent`,
    //   displayName: 'Agent',
    //   permissions: [],
    //   icon: 'server-connection',
    // },
    // {
    //   routerLink: `./settings`,
    //   displayName: 'Settings',
    //   permissions: [],
    //   icon: 'settings',
    // }
  ];

  constructor(public integrationsService: IntegrationsService) {}
}
