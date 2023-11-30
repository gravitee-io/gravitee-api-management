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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { castArray } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { MenuItemHeader } from '../../api/api-navigation/MenuGroupItem';
import { Application } from '../../../entities/application/application';
import { ApplicationService } from '../../../services-ngx/application.service';

export interface MenuItem {
  targetRoute?: string;
  baseRoute?: string | string[];
  displayName: string;
  permissions?: string[];
  tabs?: MenuItem[];
  header?: MenuItemHeader;
  routerLink?: string;
  routerLinkActiveOptions?: { exact: boolean };
}

@Component({
  selector: 'application-navigation',
  template: require('./application-navigation.component.html'),
  styles: [require('./application-navigation.component.scss')],
})
export class ApplicationNavigationComponent implements OnInit, OnDestroy {
  public application: Application;
  public subMenuItems: MenuItem[] = [];
  public hasBreadcrumb = false;
  public selectedItemWithTabs: MenuItem = undefined;
  public isMenuTabAvailable = false;
  private unsubscribe$ = new Subject();

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly permissionService: GioPermissionService,
    private readonly gioMenuService: GioMenuService,
    private readonly applicationService: ApplicationService,
  ) {}

  ngOnInit() {
    this.applicationService
      .getLastApplicationFetch(this.activatedRoute.snapshot.params.applicationId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe({
        next: (application) => (this.application = application),
      });

    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });
    this.subMenuItems = this.filterMenuByPermission([
      {
        displayName: 'Global settings',
        routerLink: 'general',
        targetRoute: 'management.applications.application.general',
        baseRoute: 'management.applications.application.general',
        permissions: ['application-definition-r'],
      },
      // {
      //   displayName: "User and group access",
      //   targetRoute: "management.applications.application.membersng",
      //   baseRoute: "management.applications.application.membersng",
      //   permissions: ["application-definition-r"],
      //   tabs: [
      //     {
      //       displayName: "Members",
      //       targetRoute: "management.applications.application.membersng",
      //       baseRoute: "management.applications.application.membersng"
      //     }, {
      //       displayName: "Groups",
      //       targetRoute: "management.applications.application.groupsng",
      //       baseRoute: "management.applications.application.groupsng"
      //     }, {
      //       displayName: "Transfer ownership",
      //       targetRoute: "management.applications.application.transferownershipng",
      //       baseRoute: "management.applications.application.transferownershipng"
      //     }
      //   ]
      // },
      {
        displayName: 'Metadata',
        routerLink: 'metadata',
        targetRoute: 'management.applications.application.metadata',
        baseRoute: 'management.applications.application.metadata',
        permissions: ['application-metadata-r'],
      },
      {
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
        targetRoute: 'management.applications.application.subscriptions.list',
        baseRoute: 'management.applications.application.subscriptions',
        permissions: ['application-subscription-r'],
      },
      {
        displayName: 'Members',
        targetRoute: 'management.applications.application.members',
        baseRoute: 'management.applications.application.members',
        permissions: ['application-member-r'],
      },
      {
        displayName: 'Analytics',
        routerLink: 'analytics',
        targetRoute: 'management.applications.application.analytics',
        baseRoute: 'management.applications.application.analytics',
        permissions: ['application-analytics-r'],
      },
      {
        displayName: 'Logs',
        routerLink: 'logs',
        targetRoute: 'management.applications.application.logs.list',
        baseRoute: 'management.applications.application.logs',
        permissions: ['application-log-r'],
      },
      {
        displayName: 'Notification settings',
        routerLink: 'notification-settings',
        targetRoute: 'management.applications.application.notification-settings',
        baseRoute: [
          'management.applications.application.notification-settings',
          'management.applications.application.notification-settings-details',
        ],
        permissions: ['application-notification-r', 'application-alert-r'],
      },
    ]);
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    if (menuItems) {
      return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
    }
    return [];
  }

  isActive(item: MenuItem): boolean {
    if (!item.routerLink) {
      return false;
    }
    return [item.routerLink, ...castArray(item.baseRoute)]
      .filter((r) => !!r)
      .some((routerLink) => {
        // TODO: Implement into new navigation
        this.subMenuItems.map((selectedItem) => {
          if (selectedItem.baseRoute === item.baseRoute) {
            this.selectedItemWithTabs = selectedItem;
            this.isMenuTabAvailable = true;
          } else {
            this.isMenuTabAvailable = false;
          }
        });
        return this.router.isActive(this.router.createUrlTree([routerLink], { relativeTo: this.activatedRoute }), {
          paths: item.routerLinkActiveOptions?.exact ? 'exact' : 'subset',
          queryParams: 'subset',
          fragment: 'ignored',
          matrixParams: 'ignored',
        });
      });
  }

  public computeBreadcrumbItems(): string[] {
    const breadcrumbItems: string[] = [];

    this.subMenuItems.forEach((item) => {
      if (this.isActive(item)) {
        breadcrumbItems.push(item.displayName);
      }
    });

    return breadcrumbItems;
  }
}
