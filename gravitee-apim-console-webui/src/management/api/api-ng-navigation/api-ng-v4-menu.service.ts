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

import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiMenuService } from './ApiMenuService';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Injectable()
export class ApiNgV4MenuService implements ApiMenuService {
  constructor(private readonly permissionService: GioPermissionService) {}

  public getMenu(): {
    subMenuItems: MenuItem[];
    groupItems: MenuGroupItem[];
  } {
    const subMenuItems: MenuItem[] = [
      {
        displayName: 'Policy Studio',
        targetRoute: 'management.apis.ng.policyStudio',
        baseRoute: 'management.apis.ng.policyStudio',
        tabs: undefined,
      },
    ];
    const groupItems: MenuGroupItem[] = [this.getGeneralGroup(), this.getEntrypointsGroup(), this.getEndpointsGroup()].filter(
      (group) => !!group,
    );

    return { subMenuItems, groupItems };
  }

  private getGeneralGroup(): MenuGroupItem {
    const generalGroup: MenuGroupItem = {
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
      generalGroup.items.push(plansMenuItem);
    }

    if (this.permissionService.hasAnyMatching(['api-definition-r'])) {
      generalGroup.items.push(
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

    return generalGroup;
  }

  private getEntrypointsGroup(): MenuGroupItem {
    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      const entrypointsGroup: MenuGroupItem = {
        title: 'Entrypoints',
        items: [
          {
            displayName: 'General',
            targetRoute: 'management.apis.ng.entrypoints',
            baseRoute: 'management.apis.ng.entrypoints',
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
        targetRoute: 'management.apis.ng.endpoints',
        baseRoute: ['management.apis.ng.endpoints', 'management.apis.ng.endpoint'],
      });
    }

    return endpointsGroup;
  }
}
