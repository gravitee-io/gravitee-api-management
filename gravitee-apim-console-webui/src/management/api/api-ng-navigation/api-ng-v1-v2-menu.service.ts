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
import { map } from 'rxjs/operators';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Feature } from '../../../shared/components/gio-license/gio-license-features';
import { UTMMedium } from '../../../shared/components/gio-license/gio-license-utm';
import { CurrentUserService } from '../../../ajs-upgraded-providers';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';
import { GioLicenseService } from '../../../shared/components/gio-license/gio-license.service';
import { ApiV1, ApiV2, DefinitionVersion } from '../../../entities/management-api-v2';

@Injectable()
export class ApiNgV1V2MenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  public getMenu(api: ApiV1 | ApiV2): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      {
        displayName: 'Policy Studio',
        targetRoute: 'management.apis.ng.policy-studio-v2',
        baseRoute: 'management.apis.ng.policy-studio-v2',
      },
      {
        displayName: 'Messages',
        targetRoute: 'management.apis.ng.messages',
        baseRoute: 'management.apis.ng.messages',
      },
    ];

    const groupItems: MenuGroupItem[] = [
      this.getPortalGroup(),
      this.getProxyGroup(api.definitionVersion),
      this.getBackendServicesGroup(),
      this.getAnalyticsGroup(),
      this.getAuditGroup(),
      this.getNotificationsGroup(),
    ].filter((group) => !!group);

    return { subMenuItems, groupItems };
  }

  private getPortalGroup(): MenuGroupItem {
    const portalGroup: MenuGroupItem = {
      title: 'General',
      items: [
        {
          displayName: 'Info',
          targetRoute: 'management.apis.ng.general',
          baseRoute: 'management.apis.ng.general',
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
        targetRoute: 'management.apis.ng.plans',
        baseRoute: ['management.apis.ng.plans', 'management.apis.ng.plan'],
      });
    }
    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Subscriptions',
        targetRoute: 'management.apis.ng.subscriptions',
        baseRoute: ['management.apis.ng.subscriptions', 'management.apis.ng.subscription'],
      });
    }
    if (plansMenuItem.tabs.length > 0) {
      portalGroup.items.push(plansMenuItem);
    }

    // Documentation
    const documentationMenuItem: MenuItem = {
      displayName: 'Documentation',
      tabs: [],
    };
    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      documentationMenuItem.tabs.push({
        displayName: 'Pages',
        targetRoute: 'management.apis.ng.documentation',
        baseRoute: 'management.apis.ng.documentation',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-metadata-r'])) {
      documentationMenuItem.tabs.push({
        displayName: 'Metadata',
        targetRoute: 'management.apis.ng.metadata',
        baseRoute: 'management.apis.ng.metadata',
      });
    }
    if (documentationMenuItem.tabs.length > 0) {
      portalGroup.items.push(documentationMenuItem);
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
          targetRoute: 'management.apis.ng.members',
          baseRoute: 'management.apis.ng.members',
        },
        {
          displayName: 'Groups',
          targetRoute: 'management.apis.ng.groups',
          baseRoute: 'management.apis.ng.groups',
        },
      );
    }
    if (this.currentUserService.currentUser.isOrganizationAdmin() || this.permissionService.hasAnyMatching(['api-member-u'])) {
      userAndGroupAccessMenuItems.tabs.push({
        displayName: 'Transfer ownership',
        targetRoute: 'management.apis.ng.transferOwnership',
        baseRoute: 'management.apis.ng.transferOwnership',
      });
    }
    if (userAndGroupAccessMenuItems.tabs.length > 0) {
      portalGroup.items.push(userAndGroupAccessMenuItems);
    }

    return portalGroup;
  }

  private getProxyGroup(definitionVersion: DefinitionVersion): MenuGroupItem {
    const proxyGroup: MenuGroupItem = {
      title: 'Proxy',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      proxyGroup.items.push({
        displayName: 'Entrypoints',
        targetRoute: 'management.apis.ng.entrypoints-v2',
        baseRoute: 'management.apis.ng.entrypoints-v2',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      proxyGroup.items.push(
        {
          displayName: 'CORS',
          targetRoute: 'management.apis.ng.cors',
          baseRoute: 'management.apis.ng.cors',
        },
        {
          displayName: 'Deployments',
          targetRoute: 'management.apis.ng.deployments',
          baseRoute: 'management.apis.ng.deployments',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-response_templates-r'])) {
      proxyGroup.items.push({
        displayName: 'Response Templates',
        targetRoute: 'management.apis.ng.responseTemplates',
        baseRoute: 'management.apis.ng.responseTemplates',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      if (definitionVersion === 'V1') {
        proxyGroup.items.push(
          {
            displayName: 'Properties',
            targetRoute: 'management.apis.detail.proxy.properties',
            baseRoute: 'management.apis.detail.proxy.properties',
          },
          {
            displayName: 'Resources',
            targetRoute: 'management.apis.detail.proxy.resources',
            baseRoute: 'management.apis.detail.proxy.resources',
          },
        );
      } else {
        proxyGroup.items.push(
          {
            displayName: 'Properties',
            targetRoute: 'management.apis.ng.properties',
            baseRoute: 'management.apis.ng.properties',
          },
          {
            displayName: 'Resources',
            targetRoute: 'management.apis.ng.resources',
            baseRoute: 'management.apis.ng.resources',
          },
        );
      }
    }

    if (proxyGroup.items.length > 0) {
      return proxyGroup;
    }
    return undefined;
  }
  private getBackendServicesGroup(): MenuGroupItem {
    // Backend services
    const backendServicesGroup: MenuGroupItem = {
      title: 'Backend services',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      backendServicesGroup.items.push(
        {
          displayName: 'Endpoints',
          targetRoute: 'management.apis.detail.proxy.endpoints',
          baseRoute: ['management.apis.detail.proxy.endpoints', 'management.apis.detail.proxy.endpoint'],
        },
        {
          displayName: 'Failover',
          targetRoute: 'management.apis.detail.proxy.failover',
          baseRoute: 'management.apis.detail.proxy.failover',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check',
        targetRoute: 'management.apis.detail.proxy.healthcheck',
        baseRoute: 'management.apis.detail.proxy.healthcheck',
      });
    }

    // Health-check dashboard
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check dashboard',
        baseRoute: 'management.apis.detail.proxy.healthCheckDashboard.visualize',
        targetRoute: 'management.apis.detail.proxy.healthCheckDashboard.visualize',
      });
    }

    if (backendServicesGroup.items.length > 0) {
      return backendServicesGroup;
    }
    return undefined;
  }

  private getAnalyticsGroup(): MenuGroupItem {
    const analyticsGroup: MenuGroupItem = {
      title: 'Analytics',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-analytics-r'])) {
      analyticsGroup.items.push({
        displayName: 'Overview',
        targetRoute: 'management.apis.detail.analytics.overview',
        baseRoute: 'management.apis.detail.analytics.overview',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-log-r'])) {
      analyticsGroup.items.push({
        displayName: 'Logs',
        targetRoute: 'management.apis.detail.analytics.logs.list',
        baseRoute: 'management.apis.detail.analytics.logs',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u'])) {
      analyticsGroup.items.push({
        displayName: 'Path mappings',
        targetRoute: 'management.apis.detail.analytics.pathMappings',
        baseRoute: 'management.apis.detail.analytics.pathMappings',
      });
    }
    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      analyticsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'management.apis.detail.analytics.alerts',
        baseRoute: 'management.apis.detail.analytics.alerts',
      });
    }
    if (analyticsGroup.items.length > 0) {
      return analyticsGroup;
    }
    return undefined;
  }

  private getAuditGroup(): MenuGroupItem {
    const license = { feature: Feature.APIM_AUDIT_TRAIL, utmMedium: UTMMedium.AUDIT_TRAIL_API };
    const iconRight$ = this.gioLicenseService
      .isMissingFeature$(license.feature)
      .pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));

    const auditGroup: MenuGroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        targetRoute: 'management.apis.detail.audit.general',
        baseRoute: 'management.apis.detail.audit.general',
        license,
        iconRight$,
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      auditGroup.items.push({
        displayName: 'History',
        targetRoute: 'management.apis.detail.audit.history',
        baseRoute: 'management.apis.detail.audit.history',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-u'])) {
      auditGroup.items.push({
        displayName: 'Events',
        targetRoute: 'management.apis.ng.events',
        baseRoute: 'management.apis.ng.events',
      });
    }

    if (auditGroup.items.length > 0) {
      return auditGroup;
    }
    return undefined;
  }
  private getNotificationsGroup(): MenuGroupItem {
    const notificationsGroup: MenuGroupItem = {
      title: 'Notifications',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-notification-r'])) {
      notificationsGroup.items.push({
        displayName: 'Notifications',
        targetRoute: 'management.apis.detail.notifications',
        baseRoute: 'management.apis.detail.notifications',
      });
    }

    if (this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      notificationsGroup.items.push({
        displayName: 'Alerts',
        targetRoute: 'management.apis.detail.alerts.list',
        baseRoute: 'management.apis.detail.alerts',
      });
    }

    if (notificationsGroup.items.length > 0) {
      return notificationsGroup;
    }
    return undefined;
  }
}
