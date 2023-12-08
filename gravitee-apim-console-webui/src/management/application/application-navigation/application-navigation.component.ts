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
import { castArray, flatMap } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { Application } from '../../../entities/application/application';
import { ApplicationService } from '../../../services-ngx/application.service';

export interface MenuItem {
  displayName: string;
  permissions?: string[];
  tabs?: MenuItem[];
  header?: MenuItemHeader;
  routerLink?: string;
  routerLinkActiveOptions?: { exact: boolean };
}

export interface MenuItemHeader {
  title?: string;
  subtitle?: string;
}

@Component({
  selector: 'application-navigation',
  templateUrl: './application-navigation.component.html',
  styleUrls: ['./application-navigation.component.scss'],
})
export class ApplicationNavigationComponent implements OnInit, OnDestroy {
  public application: Application;
  public subMenuItems: MenuItem[] = [];
  public hasBreadcrumb = false;
  public selectedItemWithTabs: MenuItem = undefined;
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
        permissions: ['application-metadata-r'],
      },
      {
        displayName: 'Subscriptions',
        routerLink: 'subscriptions',
        permissions: ['application-subscription-r'],
      },
      {
        displayName: 'Members',
        routerLink: 'members',
        permissions: ['application-member-r'],
      },
      {
        displayName: 'Analytics',
        routerLink: 'analytics',
        permissions: ['application-analytics-r'],
      },
      {
        displayName: 'Logs',
        routerLink: 'logs',
        permissions: ['application-log-r'],
      },
      {
        displayName: 'Notification settings',
        routerLink: 'notification-settings',
        permissions: ['application-notification-r', 'application-alert-r'],
      },
    ]);

    this.selectedItemWithTabs = this.subMenuItems.find((item) => item.tabs && this.isTabActive(item.tabs));
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
    return [item.routerLink, ...castArray(item.routerLink)]
      .filter((r) => !!r)
      .some((routerLink) => {
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

  private isTabActive(tabs: MenuItem[]): boolean {
    return flatMap(tabs, (tab) => tab).some((tab) => this.isActive(tab));
  }
}
