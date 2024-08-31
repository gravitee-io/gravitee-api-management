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

import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../entities/Constants';
import { ApiFederated } from '../../../entities/management-api-v2';
import { ApiDocumentationV2Service } from '../../../services-ngx/api-documentation-v2.service';

@Injectable()
export class ApiFederatedMenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
    private readonly apiDocumentationV2Service: ApiDocumentationV2Service,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getMenu(api: ApiFederated): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      this.addConfigurationMenuEntry(),
      ...(this.constants.org.settings?.scoring?.enabled ? [this.addApiScoreMenuEntry()] : []),
      this.addConsumersMenuEntry(),
      this.addDocumentationMenuEntry(api),
    ];

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

  private addConsumersMenuEntry(): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-plan-r'])) {
      tabs.push({
        displayName: 'Plans',
        routerLink: 'plans',
      });
    }

    if (this.permissionService.hasAnyMatching(['api-subscription-r'])) {
      tabs.push({
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
      });
    }

    return {
      displayName: 'Consumers',
      icon: 'cloud-consumers',
      routerLink: '',
      header: {
        title: 'Consumers',
        subtitle: 'Manage how your API is consumed',
      },
      tabs: [...tabs, { displayName: 'Broadcasts', routerLink: 'messages' }],
    };
  }

  private addDocumentationMenuEntry(api: ApiFederated): MenuItem {
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
      tabs,
    };
  }
}
