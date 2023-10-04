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
import { Inject, Injectable } from '@angular/core';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { CurrentUserService } from '../../../ajs-upgraded-providers';
import UserService from '../../../services/user.service';

@Injectable()
export class ApiV4MenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  public getMenu(): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      {
        displayName: 'Policy Studio',
        targetRoute: 'management.apis.policyStudio',
        baseRoute: 'management.apis.policyStudio',
        tabs: undefined,
      },
    ];

    const logsTabs = [];
    if (this.permissionService.hasAnyMatching(['api-log-r'])) {
      logsTabs.push({
        displayName: 'Runtime Logs',
        targetRoute: 'management.apis.runtimeLogs',
        baseRoute: ['management.apis.runtimeLogs', 'management.apis.runtimeLogs-messages'],
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u', 'api-log-u'])) {
      logsTabs.push({
        displayName: 'Settings',
        targetRoute: 'management.apis.runtimeLogs-settings',
        baseRoute: 'management.apis.runtimeLogs-settings',
      });
    }
    if (logsTabs.length > 0) {
      subMenuItems.push({
        displayName: 'Analytics and logs',
        header: {
          title: 'Runtime Logs',
          subtitle: 'Debug and Optimize your API by displaying logs from your API runtime activities',
        },
        tabs: logsTabs,
      });
    }

    const groupItems: MenuGroupItem[] = [
      this.getGeneralGroup(),
      this.getEntrypointsGroup(),
      this.getEndpointsGroup(),
      this.getAnalyticsGroup(),
      this.getAuditGroup(),
      this.getNotificationsGroup(),
    ].filter((group) => !!group);

    return { subMenuItems, groupItems };
  }

  private getGeneralGroup(): MenuGroupItem {
    const generalGroup: MenuGroupItem = {
      title: 'General',
      items: [
        {
          displayName: 'Info',
          targetRoute: 'management.apis.general',
          baseRoute: 'management.apis.general',
        },
      ],
    };
    // Plans
    const plansMenuItem: MenuItem = {
      displayName: 'Plans',
      tabs: [],
    };

    if (this.permissionService.hasAnyMatching(['api-plan-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Plans',
        targetRoute: 'management.apis.plans',
        baseRoute: ['management.apis.plans', 'management.apis.plan'],
      });
    }
    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Subscriptions',
        targetRoute: 'management.apis.subscriptions',
        baseRoute: ['management.apis.subscriptions', 'management.apis.subscription'],
      });
    }
    if (plansMenuItem.tabs.length > 0) {
      generalGroup.items.push(plansMenuItem);
    }

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      generalGroup.items.push(
        {
          displayName: 'Properties',
          targetRoute: 'management.apis.properties',
          baseRoute: 'management.apis.properties',
        },
        {
          displayName: 'Resources',
          targetRoute: 'management.apis.resources',
          baseRoute: 'management.apis.resources',
        },
      );
    }

    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      generalGroup.items.push({
        displayName: 'Documentation',
        targetRoute: 'management.apis.documentationV4',
        baseRoute: ['management.apis.documentationV4'],
      });
    }

    // Users
    const userAndGroupAccessMenuItems: MenuItem = {
      displayName: 'User and group access',
      tabs: [],
    };
    if (this.permissionService.hasAnyMatching(['api-member-r'])) {
      userAndGroupAccessMenuItems.tabs.push(
        {
          displayName: 'Members',
          targetRoute: 'management.apis.members',
          baseRoute: 'management.apis.members',
        },
        {
          displayName: 'Groups',
          targetRoute: 'management.apis.groups',
          baseRoute: 'management.apis.groups',
        },
      );
    }
    if (this.currentUserService.currentUser.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u'])) {
      userAndGroupAccessMenuItems.tabs.push({
        displayName: 'Transfer ownership',
        targetRoute: 'management.apis.transferOwnership',
        baseRoute: 'management.apis.transferOwnership',
      });
    }
    if (userAndGroupAccessMenuItems.tabs.length > 0) {
      generalGroup.items.push(userAndGroupAccessMenuItems);
    }

    return generalGroup;
  }

  private getEntrypointsGroup(): MenuGroupItem {
    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      const entrypointsGroup: MenuGroupItem = {
        title: 'Entrypoints',
        items: [
          {
            displayName: 'General',
            targetRoute: 'management.apis.entrypoints',
            baseRoute: ['management.apis.entrypoints', 'management.apis.entrypoints-edit'],
          },
        ],
      };
      return entrypointsGroup;
    }
    return undefined;
  }

  private getEndpointsGroup(): MenuGroupItem {
    const endpointsGroup: MenuGroupItem = {
      title: 'Endpoints',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      endpointsGroup.items.push({
        displayName: 'Backend services',
        targetRoute: 'management.apis.endpoint-groups',
        baseRoute: [
          'management.apis.endpoint-groups',
          'management.apis.endpoint',
          'management.apis.endpoint-group',
          'management.apis.endpoint-group-new',
        ],
      });
    }

    return endpointsGroup;
  }

  private getAnalyticsGroup(): MenuGroupItem {
    const analyticsGroup: MenuGroupItem = {
      title: 'Analytics',
      items: [],
    };
    if (this.permissionService.hasAnyMatching(['api-analytics-r'])) {
      analyticsGroup.items.push({
        displayName: 'Overview',
        targetRoute: 'DISABLED',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u'])) {
      analyticsGroup.items.push({
        displayName: 'Path mappings',
        targetRoute: 'DISABLED',
      });
    }
    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      analyticsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'DISABLED',
      });
    }

    return analyticsGroup;
  }

  private getAuditGroup(): MenuGroupItem {
    const auditGroup: MenuGroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        targetRoute: 'DISABLED',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      auditGroup.items.push({
        displayName: 'History',
        targetRoute: 'DISABLED',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-u'])) {
      auditGroup.items.push({
        displayName: 'Events',
        targetRoute: 'DISABLED',
      });
    }

    return auditGroup;
  }

  private getNotificationsGroup(): MenuGroupItem {
    const notificationsGroup: MenuGroupItem = {
      title: 'Notifications',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-notification-r'])) {
      notificationsGroup.items.push({
        displayName: 'Notifications',
        targetRoute: 'DISABLED',
      });
    }

    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      notificationsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'DISABLED',
      });
    }

    return notificationsGroup;
  }
}
