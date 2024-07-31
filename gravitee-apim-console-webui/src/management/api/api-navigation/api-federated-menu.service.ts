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

@Injectable()
export class ApiFederatedMenuService implements ApiMenuService {
  constructor(
    private readonly permissionService: GioPermissionService,
    private readonly gioLicenseService: GioLicenseService,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public getMenu(): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [this.addConfigurationMenuEntry(), this.addConsumersMenuEntry(), this.addDocumentationMenuEntry()];

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

  private addDocumentationMenuEntry(): MenuItem {
    const tabs: MenuItem[] = [];

    if (this.permissionService.hasAnyMatching(['api-documentation-r'])) {
      tabs.push(
        {
          displayName: 'Default Pages',
          routerLink: 'v4/documentation/default-pages',
          routerLinkActiveOptions: { exact: false },
        },
        {
          displayName: 'Custom Pages',
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
      },
      tabs,
    };
  }
}
