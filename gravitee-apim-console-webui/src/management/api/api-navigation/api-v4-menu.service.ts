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
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { map } from 'rxjs/operators';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';

@Injectable()
export class ApiV4MenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    @Inject(Constants) private readonly constants: Constants,
  ) {}
  public getMenu(api: ApiV4): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const hasTcpListeners = api.listeners.find((listener) => listener.type === 'TCP') != null;

    const subMenuItems: MenuItem[] = [
      this.addConfigurationMenuEntry(),
      ...(this.constants.org.settings?.scoring?.enabled && api.type !== 'NATIVE' ? [this.addApiScoreMenuEntry()] : []),
      ...(api.type !== 'NATIVE' ? [this.addEntrypointsMenuEntry(hasTcpListeners)] : []),
      ...(api.type !== 'NATIVE' ? [this.addEndpointsMenuEntry(api, hasTcpListeners)] : []),
      ...(api.type !== 'NATIVE' ? [this.addPoliciesMenuEntry(hasTcpListeners)] : []),
      ...(api.type !== 'NATIVE' ? [this.addConsumersMenuEntry(hasTcpListeners)] : []),
      this.addDocumentationMenuEntry(api),
      ...(api.type !== 'NATIVE' ? [this.addDeploymentMenuEntry()] : []),
      ...(api.type !== 'NATIVE' ? [this.addApiTrafficMenuEntry(hasTcpListeners)] : []),
      ...(api.type !== 'NATIVE' ? [this.addApiRuntimeAlertsMenuEntry()] : []),
    ].filter((entry) => entry != null && !entry.tabs?.every((tab) => tab.routerLink === 'DISABLED'));

    return { subMenuItems, groupItems: [] };
  }

  private addConfigurationMenuEntry(): MenuItem {
    const license = { feature: ApimFeature.APIM_AUDIT_TRAIL, context: UTMTags.CONTEXT_API };
    const iconRight$ = this.gioLicenseService
      .isMissingFeature$(license.feature)
      .pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));

    const tabs: MenuItem[] = [
      {
        displayName: 'General',
        routerLink: '.',
        routerLinkActiveOptions: { exact: true },
      },
    ];

    if (this.permissionService.hasAnyMatching(['api-member-r'])) {
      tabs.push({
        displayName: 'User Permissions',
        routerLink: 'members',
      });
    }

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      tabs.push(
        {
          displayName: 'Properties',
          routerLink: 'properties',
        },
        {
          displayName: 'Resources',
          routerLink: 'resources',
          routerLinkActiveOptions: { exact: true },
        },
      );
    }

    if (this.permissionService.hasAnyMatching(['api-notification-r'])) {
      tabs.push({
        displayName: 'Notifications',
        routerLink: 'notifications',
        routerLinkActiveOptions: { exact: true },
      });
    }

    if (this.permissionService.hasAnyMatching(['api-audit-r'])) {
      tabs.push({
        displayName: 'Audit Logs',
        routerLink: 'v4/audit',
        license,
        iconRight$,
      });
    }

    return {
      displayName: 'Configuration',
      icon: 'settings',
      routerLink: '',
      header: {
        title: 'Configuration',
        subtitle: 'Manage general settings, user permissions, properties, and resources, and track changes to your API',
      },
      tabs: tabs,
    };
  }

  private addApiScoreMenuEntry(): MenuItem {
    return {
      displayName: 'API Score',
      icon: 'shield-check',
      routerLink: 'api-score',
      header: {
        title: 'API Score',
      },
    };
  }

  private addEntrypointsMenuEntry(hasTcpListeners: boolean): MenuItem {
    const tabs: MenuItem[] = [
      {
        displayName: 'Entrypoints',
        routerLink: 'v4/entrypoints',
      },
    ];

    if (this.permissionService.hasAnyMatching(['api-response_templates-r'])) {
      tabs.push({
        displayName: 'Response Templates',
        routerLink: hasTcpListeners ? 'DISABLED' : 'response-templates',
      });
    }

    if (!hasTcpListeners) {
      tabs.push({
        displayName: 'CORS',
        routerLink: 'v4/cors',
      });
    }

    return {
      displayName: 'Entrypoints',
      icon: 'entrypoints',
      routerLink: '',
      header: {
        title: 'Entrypoints',
        subtitle: 'Define the protocol and configuration settings by which the API consumer accesses the Gateway API',
      },
      tabs: tabs,
    };
  }

  private addEndpointsMenuEntry(api: ApiV4, hasTcpListeners: boolean): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      tabs.push({
        displayName: 'Endpoints',
        routerLink: 'v4/endpoints',
      });
    }

    if ((api.type === 'PROXY' && !hasTcpListeners) || api.type === 'MESSAGE')
      tabs.push({
        displayName: 'Failover',
        routerLink: 'v4/failover',
      });

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      if (api.type === 'PROXY' && !hasTcpListeners) {
        tabs.push({
          displayName: 'Health Check Dashboard',
          routerLink: 'v4/health-check-dashboard',
        });
      }
    }

    return {
      displayName: 'Endpoints',
      icon: 'endpoints',
      routerLink: tabs.length === 1 ? tabs[0].routerLink : '',
      header: {
        title: 'Endpoints',
        subtitle:
          'Define the protocol and configuration settings by which the Gateway API will fetch data from, or post data to, the backend API',
      },
      tabs: tabs.length === 1 ? undefined : tabs,
    };
  }

  private addConsumersMenuEntry(hasTcpListeners: boolean): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-plan-r'])) {
      tabs.push({
        displayName: 'Plans',
        routerLink: 'plans',
      });
    }

    if (this.permissionService.hasAnyMatching(['api-subscription-r']) && !hasTcpListeners) {
      tabs.push({
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
      });
    }

    tabs.push({ displayName: 'Broadcasts', routerLink: 'messages' });
    return {
      displayName: 'Consumers',
      icon: 'cloud-consumers',
      routerLink: '',
      header: {
        title: 'Consumers',
        subtitle: 'Manage how your API is consumed',
      },
      tabs: tabs,
    };
  }

  private addDocumentationMenuEntry(api: ApiV4): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      tabs.push(
        {
          displayName: 'Main Pages',
          routerLink: 'v4/documentation/main-pages',
          routerLinkActiveOptions: { exact: false },
        },
        {
          displayName: 'Documentation Pages',
          routerLink: 'v4/documentation/pages',
          routerLinkActiveOptions: { exact: false },
        },
      );
    }

    if (this.permissionService.hasAnyMatching(['api-metadata-r'])) {
      tabs.push({
        displayName: 'Metadata',
        routerLink: 'v4/documentation/metadata',
        routerLinkActiveOptions: { exact: true },
      });
    }

    return {
      displayName: 'Documentation',
      icon: 'book',
      routerLink: '',
      header: {
        title: 'Documentation',
        subtitle: 'Documentation pages appear in the Developer Portal and inform API consumers how to use your API',
        action: {
          text: 'Open API in Developer Portal',
          targetUrl: this.apiDocumentationV2Service.getApiPortalUrl(api.id),
          disabled: api.lifecycleState !== 'PUBLISHED',
          disabledTooltip: this.apiDocumentationV2Service.getApiNotInPortalTooltip(api.lifecycleState),
        },
      },
      tabs: tabs,
    };
  }

  private addDeploymentMenuEntry(): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      tabs.push({
        displayName: 'Configuration',
        routerLink: 'deployments',
      });
    }

    if (this.permissionService.hasAnyMatching(['api-event-r'])) {
      tabs.push({
        displayName: 'Deployment History',
        routerLink: 'v4/history',
      });
    }

    return {
      displayName: 'Deployment',
      icon: 'rocket',
      routerLink: '',
      header: {
        title: 'Deployment',
        subtitle: 'Manage sharding tags and track every change of your API',
      },
      tabs: tabs,
    };
  }

  private addPoliciesMenuEntry(hasTcpListeners: boolean): MenuItem {
    return {
      displayName: 'Policies',
      icon: 'shield-star',
      header: {
        title: 'Policies',
        subtitle: 'Policies let you customize and enhance your API behavior and functionality',
      },
      routerLink: hasTcpListeners ? 'DISABLED' : 'v4/policy-studio',
      tabs: undefined,
    };
  }

  private addApiTrafficMenuEntry(hasTcpListeners: boolean): MenuItem {
    const apiTrafficTabs = [];

    if (this.permissionService.hasAnyMatching(['api-analytics-r'])) {
      apiTrafficTabs.push({
        displayName: 'Analytics',
        routerLink: 'v4/analytics',
      });
    }

    if (this.permissionService.hasAnyMatching(['api-log-r'])) {
      apiTrafficTabs.push({
        displayName: 'Runtime Logs',
        routerLink: 'v4/runtime-logs',
      });
    }
    if (this.permissionService.hasAnyMatching(['api-definition-u', 'api-log-u'])) {
      apiTrafficTabs.push({
        displayName: 'Settings',
        routerLink: 'v4/runtime-logs-settings',
      });
    }
    if (apiTrafficTabs.length > 0) {
      return {
        displayName: 'API Traffic',
        icon: 'bar-chart-2',
        routerLink: hasTcpListeners ? 'DISABLED' : '',
        header: {
          title: 'API Traffic',
          subtitle: 'Gain actionable insights into API performance with real-time metrics, logs, and notifications',
        },
        tabs: apiTrafficTabs,
      };
    }
    return null;
  }

  private addApiRuntimeAlertsMenuEntry(): MenuItem {
    const tabs = [
      {
        displayName: 'Alerts',
        routerLink: 'DISABLED',
      },
      {
        displayName: 'Notifications',
        routerLink: 'DISABLED',
      },
      {
        displayName: 'History',
        routerLink: 'DISABLED',
      },
    ];
    return {
      displayName: 'Runtime Alerts',
      icon: 'alarm',
      routerLink: '',
      header: {
        title: 'Runtime Alerts',
        subtitle: 'Gain actionable insights into API performance with real-time metrics and connection logs',
      },
      tabs,
    };
  }
}
