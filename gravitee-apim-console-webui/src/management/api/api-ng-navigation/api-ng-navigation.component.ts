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
import { Component, Inject, Input, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import { castArray, flatMap } from 'lodash';
import { takeUntil } from 'rxjs/operators';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';

import { AjsRootScope, CurrentUserService, UIRouterState } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';
import { Api } from '../../../entities/management-api-v2';

export interface MenuItem {
  targetRoute?: string;
  baseRoute?: string | string[];
  displayName: string;
  tabs?: MenuItem[];
}

interface GroupItem {
  title: string;
  items: MenuItem[];
}

@Component({
  selector: 'api-ng-navigation',
  template: require('./api-ng-navigation.component.html'),
  styles: [require('./api-ng-navigation.component.scss')],
})
export class ApiNgNavigationComponent implements OnInit {
  @Input()
  public currentApi: Api;

  @Input()
  public currentApiIsSync: boolean;

  public subMenuItems: MenuItem[] = [];
  public groupItems: GroupItem[] = [];
  public selectedItemWithTabs: MenuItem = undefined;
  public bannerState: string;
  public hasBreadcrumb = false;
  private unsubscribe$ = new Subject();
  public breadcrumbItems: string[] = [];

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    @Inject(AjsRootScope) private readonly ajsRootScope: IScope,
    private readonly gioMenuService: GioMenuService,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduce.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    this.bannerState = localStorage.getItem('gv-api-navigation-banner');

    this.appendDesign();
    this.appendPortalGroup();
    this.appendProxyGroup();
    this.appendEndpointsGroup();

    this.selectedItemWithTabs = this.findMenuItemWithTabs();
    this.breadcrumbItems = this.computeBreadcrumbItems();

    this.ajsRootScope.$on('$locationChangeStart', () => {
      this.selectedItemWithTabs = this.findMenuItemWithTabs();
    });
  }

  private appendDesign() {
    this.subMenuItems.push({
      displayName: 'Design',
      targetRoute: 'management.apis.ng.design',
      baseRoute: 'management.apis.ng.design',
      tabs: undefined,
    });
  }

  private appendPortalGroup() {
    const portalGroup: GroupItem = {
      title: 'Portal',
      items: [
        {
          displayName: 'General',
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

    this.groupItems.push(portalGroup);
  }

  private appendProxyGroup() {
    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      const proxyGroup: GroupItem = {
        title: 'Proxy',
        items: [
          {
            displayName: 'Entrypoints',
            targetRoute: 'management.apis.ng.proxy',
            baseRoute: 'management.apis.ng.proxy',
          },
        ],
      };
      this.groupItems.push(proxyGroup);
    }
  }

  private appendEndpointsGroup() {
    const endpointsGroup: GroupItem = {
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

    this.groupItems.push(endpointsGroup);
  }

  private findMenuItemWithTabs(): MenuItem {
    let item: MenuItem = this.findActiveMenuItem(this.subMenuItems);
    if (item) {
      return item;
    }

    for (const groupItem of this.groupItems) {
      item = this.findActiveMenuItem(groupItem.items);
      if (item) {
        return item;
      }
    }
  }

  private findActiveMenuItem(items: MenuItem[]) {
    return items.filter((item) => item.tabs).find((item) => this.isTabActive(item.tabs));
  }

  navigateTo(route: string) {
    this.ajsState.go(route);
  }

  isActive(baseRoute: MenuItem['baseRoute']): boolean {
    return castArray(baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }

  isTabActive(tabs: MenuItem[]): boolean {
    return flatMap(tabs, (tab) => tab.baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
  }

  public computeBreadcrumbItems(): string[] {
    const breadcrumbItems: string[] = [];

    this.groupItems.forEach((groupItem) => {
      groupItem.items.forEach((item) => {
        if (this.isActive(item.baseRoute)) {
          breadcrumbItems.push(groupItem.title);
          breadcrumbItems.push(item.displayName);
        } else if (item.tabs && this.isTabActive(item.tabs)) {
          breadcrumbItems.push(groupItem.title);
          breadcrumbItems.push(item.displayName);
        }
      });
    });

    return breadcrumbItems;
  }
}
