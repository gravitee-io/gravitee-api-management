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

import { Component, OnDestroy, OnInit } from '@angular/core';

import { IntegrationsService } from '../../../services-ngx/integrations.service';
import { AgentStatus, IntegrationNavigationItem } from '../integrations.model';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'app-integrations-navigation',
  templateUrl: './integrations-navigation.component.html',
  styleUrls: ['./integrations-navigation.component.scss'],
})
export class IntegrationsNavigationComponent implements OnInit, OnDestroy {
  protected readonly AgentStatus = AgentStatus;
  public items: IntegrationNavigationItem[] = [
    {
      routerLink: `.`,
      displayName: 'Overview',
      permissions: ['integration-definition-r'],
      icon: 'info',
      routerLinkActiveOptions: { exact: true },
    },
    {
      routerLink: `agent`,
      displayName: 'Agent',
      permissions: ['integration-definition-r'],
      icon: 'server-connection',
      routerLinkActiveOptions: { exact: true },
    },
    {
      routerLink: `configuration`,
      displayName: 'Configuration',
      permissions: ['integration-definition-r'],
      icon: 'settings',
      routerLinkActiveOptions: { exact: false },
    },
  ];

  public allowedItems: IntegrationNavigationItem[] = [];

  constructor(
    public readonly integrationsService: IntegrationsService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnDestroy() {
    this.integrationsService.resetCurrentIntegration();
  }

  ngOnInit(): void {
    this.allowedItems = this.items.filter((item: IntegrationNavigationItem) => this.permissionService.hasAnyMatching(item.permissions));
  }
}
