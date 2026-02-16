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
import { GioLicenseService } from '@gravitee/ui-particles-angular';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { ApiV1, ApiV2, DefinitionVersion } from '../../../entities/management-api-v2';
import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { GioRoleService } from '../../../shared/components/gio-role/gio-role.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';

@Injectable()
export class ApiV1V2MenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly roleService: GioRoleService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
    @Inject(Constants) private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  public getMenu(api: ApiV1 | ApiV2): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      ...(api.definitionVersion === 'V1'
        ? [
            {
              displayName: 'Policy Studio',
              routerLink: 'v1/policies',
            },
          ]
        : [
            {
              displayName: 'Policy Studio',
              routerLink: 'v2/policy-studio',
            },
          ]),
      {
        displayName: 'Messages',
        routerLink: 'messages',
      },
    ];

    const groupItems: MenuGroupItem[] = [
      this.getPortalGroup(api.definitionVersion),
      this.getProxyGroup(api.definitionVersion),
      this.getBackendServicesGroup(),
      this.getAnalyticsGroup(),
      this.getAuditGroup(),
      this.getNotificationsGroup(),
    ].filter(group => !!group);

    return { subMenuItems, groupItems };
  }

  private getPortalGroup(definitionVersion: DefinitionVersion): MenuGroupItem {
    const portalGroup: MenuGroupItem = {
      title: 'General',
      items: [
        {
          displayName: 'Info',
          routerLink: '.',
          routerLinkActiveOptions: { exact: true },
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
        routerLink: 'plans',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      plansMenuItem.tabs.push({
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
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
        routerLink: 'documentation',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-metadata-r'])) {
      documentationMenuItem.tabs.push({
        displayName: 'Metadata',
        routerLink: 'metadata',
      });
    }
    if (documentationMenuItem.tabs.length > 0) {
      portalGroup.items.push(documentationMenuItem);
    }

    // Users
    if (this.permissionService.hasAnyMatching(['api-member-r'])) {
      const userAndGroupAccessMenuItems: MenuItem = {
        displayName: 'User and group access',
        routerLink: 'members',
        tabs: [],
      };

      portalGroup.items.push(userAndGroupAccessMenuItems);
    }

    // Api Score
    if (this.environmentSettingsService.getSnapshot().apiScore.enabled && definitionVersion === 'V2') {
      portalGroup.items.push({
        displayName: 'API Score',
        routerLink: 'api-score',
      });
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
        routerLink: 'v2/entrypoints',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      proxyGroup.items.push(
        {
          displayName: 'CORS',
          routerLink: 'cors',
        },
        {
          displayName: 'Deployments',
          routerLink: 'deployments',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-response_templates-r'])) {
      proxyGroup.items.push({
        displayName: 'Response Templates',
        routerLink: 'response-templates',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      if (definitionVersion === 'V1') {
        proxyGroup.items.push(
          {
            displayName: 'Properties',
            routerLink: 'v1/properties',
          },
          {
            displayName: 'Resources',
            routerLink: 'v1/resources',
          },
        );
      } else {
        proxyGroup.items.push(
          {
            displayName: 'Properties',
            routerLink: 'properties',
            tabs: [],
          },
          {
            displayName: 'Resources',
            routerLink: 'resources',
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
          routerLink: 'v2/endpoints',
        },
        {
          displayName: 'Failover',
          routerLink: 'v2/failover',
        },
      );
    }
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check',
        routerLink: 'v2/healthcheck',
      });
    }

    // Health-check dashboard
    if (this.permissionService.hasAnyMatching(['api-health-r'])) {
      backendServicesGroup.items.push({
        displayName: 'Health-check dashboard',
        routerLink: 'v2/healthcheck-dashboard',
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
        routerLink: 'v2/analytics-overview',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-log-r'])) {
      analyticsGroup.items.push({
        displayName: 'Logs',
        routerLink: 'v2/analytics-logs',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u'])) {
      analyticsGroup.items.push({
        displayName: 'Path mappings',
        routerLink: 'v2/path-mappings',
      });
    }
    if (!this.constants.isOEM && this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      const alertEngineLicenseOptions = {
        feature: ApimFeature.ALERT_ENGINE,
        context: UTMTags.CONTEXT_API_ANALYTICS,
      };
      const alertEngineIconRight$ = this.gioLicenseService
        .isMissingFeature$(alertEngineLicenseOptions.feature)
        .pipe(map(notAllowed => (notAllowed ? 'gio:lock' : null)));

      analyticsGroup.items.push({
        displayName: 'Alerts',
        routerLink: 'analytics-alerts',
        license: alertEngineLicenseOptions,
        iconRight$: alertEngineIconRight$,
      });
    }
    if (analyticsGroup.items.length > 0) {
      return analyticsGroup;
    }
    return undefined;
  }

  private getAuditGroup(): MenuGroupItem {
    const license = { feature: ApimFeature.APIM_AUDIT_TRAIL, context: UTMTags.CONTEXT_API };
    const iconRight$ = this.gioLicenseService.isMissingFeature$(license.feature).pipe(map(notAllowed => (notAllowed ? 'gio:lock' : null)));

    const auditGroup: MenuGroupItem = {
      title: 'Audit',
      items: [],
    };

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      auditGroup.items.push({
        displayName: 'Audit',
        routerLink: 'audit',
        license,
        iconRight$,
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      auditGroup.items.push({
        displayName: 'History',
        routerLink: 'history',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-event-u'])) {
      auditGroup.items.push({
        displayName: 'Events',
        routerLink: 'events',
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
        displayName: 'Notification settings',
        routerLink: 'notifications',
      });
    }

    if (!this.constants.isOEM && this.constants.org.settings.alert?.enabled && this.permissionService.hasAnyMatching(['api-alert-r'])) {
      const alertEngineLicenseOptions = {
        feature: ApimFeature.ALERT_ENGINE,
        context: UTMTags.CONTEXT_API_NOTIFICATIONS,
      };
      const alertEngineIconRight$ = this.gioLicenseService
        .isMissingFeature$(alertEngineLicenseOptions.feature)
        .pipe(map(notAllowed => (notAllowed ? 'gio:lock' : null)));

      notificationsGroup.items.push({
        displayName: 'Alerts',
        routerLink: 'alerts',
        license: alertEngineLicenseOptions,
        iconRight$: alertEngineIconRight$,
      });
    }

    if (notificationsGroup.items.length > 0) {
      return notificationsGroup;
    }
    return undefined;
  }
}
