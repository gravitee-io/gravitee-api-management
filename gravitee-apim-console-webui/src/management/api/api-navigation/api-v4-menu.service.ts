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
import { Injectable } from '@angular/core';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { map } from 'rxjs/operators';

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';
import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { ApiType } from '../../../entities/management-api-v2/api/v4/apiType';

@Injectable()
export class ApiV4MenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    private readonly environmentSettingsService: EnvironmentSettingsService,
  ) {}
  public getMenu(api: ApiV4): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const hasTcpListeners = api.listeners.find((listener) => listener.type === 'TCP') != null;

    const subMenuItems: MenuItem[] = [
      this.addConfigurationMenuEntry(),
      ...(this.environmentSettingsService.getSnapshot().apiScore.enabled ? [this.addApiScoreMenuEntry()] : []),
      this.addEntrypointsMenuEntry(hasTcpListeners, api),
      this.addEndpointsMenuEntry(api, hasTcpListeners),
      this.addPoliciesMenuEntry(hasTcpListeners),
      this.addConsumersMenuEntry(hasTcpListeners),
      this.addDocumentationMenuEntry(api),
      this.addDeploymentMenuEntry(),
      ...(api.type !== 'LLM_PROXY' ? [this.addApiTrafficMenuEntry(hasTcpListeners, api.type)] : []),
      ...(api.type !== 'NATIVE' ? [this.addLogs(hasTcpListeners)] : []),
      ...(api.type !== 'NATIVE' ? [this.addApiRuntimeAlertsMenuEntry()] : []),
      ...(api.type !== 'LLM_PROXY' ? this.addAlertsMenuEntry() : []),
      ...(api.type === 'PROXY' ? [this.addDebugMenuEntry()] : []),
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

  private addAlertsMenuEntry(): MenuItem[] {
    if (this.permissionService.hasAnyMatching(['api-alert-r'])) {
      return [
        {
          displayName: 'Alerts',
          icon: 'alarm',
          routerLink: 'v4/alerts',
        },
      ];
    } else {
      return [];
    }
  }

  private addEntrypointsMenuEntry(hasTcpListeners: boolean, api: ApiV4): MenuItem {
    const menuItem: MenuItem = {
      displayName: 'Entrypoints',
      icon: 'entrypoints',
      header: {
        title: 'Entrypoints',
        subtitle: 'Define the protocol and configuration settings by which the API consumer accesses the Gateway API',
      },
    };

    if (api.type === 'NATIVE' || api.type === 'MCP_PROXY' || api.type === 'LLM_PROXY') {
      return {
        ...menuItem,
        routerLink: 'v4/entrypoints',
      };
    }

    const tabs: MenuItem[] = [
      {
        displayName: 'Entrypoints',
        routerLink: 'v4/entrypoints',
      },
    ];

    if (api.type === 'PROXY' && !hasTcpListeners) {
      tabs.push({
        displayName: 'MCP Entrypoint',
        routerLink: 'v4/mcp',
      });
    }

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
      ...menuItem,
      routerLink: '',
      tabs: tabs,
    };
  }

  private addEndpointsMenuEntry(api: ApiV4, hasTcpListeners: boolean): MenuItem {
    const menuItem: MenuItem = {
      displayName: 'Endpoints',
      icon: 'endpoints',
      header: {
        title: 'Endpoints',
        subtitle:
          'Define the protocol and configuration settings by which the Gateway API will fetch data from, or post data to, the backend API',
      },
    };

    if (api.type === 'NATIVE') {
      return {
        ...menuItem,
        routerLink: 'v4/endpoints',
      };
    }

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
      ...menuItem,
      routerLink: tabs.length === 1 ? tabs[0].routerLink : '',
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

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      tabs.push({
        displayName: 'Reporter Settings',
        routerLink: 'reporter-settings',
      });
    }

    return {
      displayName: 'Deployment',
      icon: 'rocket',
      routerLink: '',
      header: {
        title: 'Deployment',
        subtitle: 'Manage sharding tags, reporter settings and track every change of your API',
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

  private addApiTrafficMenuEntry(hasTcpListeners: boolean, apiType: ApiType): MenuItem {
    if (this.permissionService.hasAnyMatching(['api-analytics-r'])) {
      const baseMenuItem = {
        displayName: 'API Traffic',
        icon: 'bar-chart-2',
        routerLink: hasTcpListeners ? 'DISABLED' : 'v4/analytics',
      };
      if (apiType === 'PROXY') {
        return baseMenuItem;
      } else {
        return {
          ...baseMenuItem,
          header: {
            title: 'API Traffic',
          },
        };
      }
    }
    return null;
  }

  private addLogs(hasTcpListeners: boolean): MenuItem {
    if (this.permissionService.hasAnyMatching(['api-log-r', 'api-log-u'])) {
      return {
        displayName: 'Logs',
        icon: 'align-justify',
        routerLink: hasTcpListeners ? 'DISABLED' : 'v4/runtime-logs',
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

  private addDebugMenuEntry(): MenuItem {
    return {
      displayName: 'Debug',
      icon: 'verified',
      header: {
        title: 'Debug',
        subtitle:
          'Debug an API by identifying, diagnosing, and fixing issues in its functionality, performance, or integration through inspecting requests, responses, logs and error messages.',
      },
      routerLink: 'v4/debug',
      tabs: undefined,
    };
  }
}
