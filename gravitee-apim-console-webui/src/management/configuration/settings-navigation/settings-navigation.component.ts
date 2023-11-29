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
import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { castArray } from 'lodash';

import { UIRouterState } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

interface MenuItem {
  // @deprecated
  targetRoute?: string;
  // @deprecated
  baseRoute?: string | string[];
  routerLink?: string;
  displayName: string;
  permissions?: string[];
}

interface GroupItem {
  title: string;
  items: MenuItem[];
}

@Component({
  selector: 'settings-navigation',
  template: require('./settings-navigation.component.html'),
  styles: [require('./settings-navigation.component.scss')],
})
export class SettingsNavigationComponent implements OnInit {
  public groupItems: GroupItem[] = [];
  public hasBreadcrumb = false;
  private unsubscribe$ = new Subject();

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly permissionService: GioPermissionService,
    private readonly gioMenuService: GioMenuService,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    this.groupItems = [
      {
        title: 'Portal',
        items: [
          {
            displayName: 'Analytics',
            routerLink: './analytics',
            permissions: ['environment-dashboard-r'],
          },
          {
            displayName: 'API Portal Information',
            routerLink: './api-portal-header',
            permissions: ['environment-api_header-r'],
          },
          {
            displayName: 'API Quality',
            routerLink: './api-quality-rules',
            permissions: ['environment-quality_rule-r'],
          },
          {
            displayName: 'Authentication',
            targetRoute: 'management.settings.environment.identityproviders',
            baseRoute: 'management.settings.environment.identityproviders',
            permissions: ['organization-identity_provider-r', 'environment-identity_provider_activation-r'],
          },
          {
            displayName: 'Categories',
            targetRoute: 'management.settings.categories.list',
            baseRoute: 'management.settings.categories',
            permissions: ['environment-category-r'],
          },
          {
            displayName: 'Client Registration',
            targetRoute: 'management.settings.clientregistrationproviders.list',
            baseRoute: 'management.settings.clientregistrationproviders',
            permissions: ['environment-client_registration_provider-r'],
          },
          {
            displayName: 'Documentation',
            targetRoute: 'management.settings.documentation.list',
            baseRoute: 'management.settings.documentation',
            permissions: ['environment-documentation-c', 'environment-documentation-u', 'environment-documentation-d'],
          },
          {
            displayName: 'Metadata',
            targetRoute: 'management.settings.metadata',
            baseRoute: 'management.settings.metadata',
            permissions: ['environment-metadata-r'],
          },
          {
            displayName: 'Settings',
            targetRoute: 'management.settings.portal',
            baseRoute: 'management.settings.portal',
            permissions: ['environment-settings-r'],
          },
          {
            displayName: 'Theme',
            targetRoute: 'management.settings.theme',
            baseRoute: 'management.settings.theme',
            permissions: ['environment-theme-r'],
          },
          {
            displayName: 'Top APIs',
            targetRoute: 'management.settings.top-apis',
            baseRoute: 'management.settings.top-apis',
            permissions: ['environment-top_apis-r'],
          },
        ],
      },
      {
        title: 'Gateway',
        items: [
          {
            displayName: 'API Logging',
            targetRoute: 'management.settings.api-logging',
            baseRoute: 'management.settings.api-logging',
            permissions: ['organization-settings-r'],
          },
          {
            displayName: 'Dictionaries',
            targetRoute: 'management.settings.dictionaries.list',
            baseRoute: 'management.settings.dictionaries',
            permissions: ['environment-dictionary-r'],
          },
        ],
      },
      {
        title: 'User Management',
        items: [
          {
            displayName: 'User Fields',
            targetRoute: 'management.settings.customUserFields',
            baseRoute: 'management.settings.customUserFields',
            permissions: ['organization-custom_user_fields-r'],
          },
          {
            displayName: 'Groups',
            targetRoute: 'management.settings.groups.list',
            baseRoute: 'management.settings.groups',
            permissions: ['environment-group-r'],
          },
        ],
      },
    ];

    const notificationGroupItem: GroupItem = {
      title: 'Notifications',
      items: [
        {
          displayName: 'Notification settings',
          targetRoute: 'management.settings.notification-settings',
          baseRoute: ['management.settings.notification-settings', 'management.settings.notification-settings-details'],
          permissions: ['environment-notification-r'],
        },
      ],
    };
    this.groupItems.push(notificationGroupItem);

    this.groupItems.forEach((groupItem) => {
      groupItem.items = groupItem.items.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
    });
  }

  isActive(baseRoute: MenuItem['baseRoute']): boolean {
    return castArray(baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }

  public computeBreadcrumbItems(): string[] {
    const breadcrumbItems: string[] = [];

    this.groupItems.forEach((groupItem) => {
      groupItem.items.forEach((item) => {
        if (this.isActive(item.baseRoute)) {
          breadcrumbItems.push(groupItem.title);
          breadcrumbItems.push(item.displayName);
        }
      });
    });

    return breadcrumbItems;
  }
}
