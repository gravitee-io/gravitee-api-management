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
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'app-integration-configuration',
  templateUrl: './integration-configuration.component.html',
  styleUrls: ['./integration-configuration.component.scss'],
})
export class IntegrationConfigurationComponent {
  public configurationTabs: IntegrationNavigationItem[] = [
    {
      displayName: 'General',
      routerLink: '.',
      permissions: ['integration-definition-u', 'integration-definition-d'],
      routerLinkActiveOptions: { exact: true },
      disabled: false,
    },
    {
      displayName: 'User Permissions ',
      routerLink: 'members',
      permissions: ['integration-member-r'],
      routerLinkActiveOptions: { exact: true },
      disabled: false,
    },
  ];
  public allowedItems: IntegrationNavigationItem[] = [];

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.allowedItems = this.configurationTabs.filter((item: IntegrationNavigationItem) =>
      this.permissionService.hasAnyMatching(item.permissions),
    );
  }
}
