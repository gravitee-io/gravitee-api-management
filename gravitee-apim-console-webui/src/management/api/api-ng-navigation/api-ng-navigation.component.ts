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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { IScope } from 'angular';
import { castArray, flatMap } from 'lodash';
import { takeUntil, map } from 'rxjs/operators';
import { GIO_DIALOG_WIDTH, GioMenuService } from '@gravitee/ui-particles-angular';
import { Observable, Subject } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';

import {
  ApiConfirmDeploymentDialogComponent,
  ApiConfirmDeploymentDialogData,
  ApiConfirmDeploymentDialogResult,
} from './api-confirm-deployment-dialog/api-confirm-deployment-dialog.component';
import { ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult } from './api-review-dialog/api-review-dialog.component';

import { AjsRootScope, CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

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

type TopBanner = {
  title: string;
  text: 'info' | 'warning' | 'error' | 'success';
  action?: {
    btnText: string;
    onClick: () => void;
  };
};

@Component({
  selector: 'api-ng-navigation',
  template: require('./api-ng-navigation.component.html'),
  styles: [require('./api-ng-navigation.component.scss')],
})
export class ApiNgNavigationComponent implements OnInit, OnDestroy {
  public currentApi$ = this.apiV2Service.getLastApiFetch(this.ajsStateParams.apiId);

  public subMenuItems: MenuItem[] = [];
  public groupItems: GroupItem[] = [];
  public selectedItemWithTabs: MenuItem = undefined;
  public bannerState: string;
  public hasBreadcrumb = false;
  public breadcrumbItems: string[] = [];
  public banners$: Observable<TopBanner[]> = this.apiV2Service.getLastApiFetch(this.ajsStateParams.apiId).pipe(
    map((api) => {
      const banners = [];

      const isApiReviewer = this.permissionService.hasAnyMatching(['api-reviews-u']);
      // Only for API reviewer
      if (isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'IN_REVIEW') {
        // 'api-reviews-u'
        banners.push({
          title: 'As an API reviewer, there are some changes made on this API that required a review before been applied.',
          type: 'info',
          action: {
            btnText: 'Review changes',
            onClick: () => {
              this.matDialog
                .open<ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult>(ApiReviewDialogComponent, {
                  data: {
                    apiId: api.id,
                  },
                  role: 'alertdialog',
                  id: 'gioApiReviewDialog',
                  width: GIO_DIALOG_WIDTH.MEDIUM,
                })
                .afterClosed()
                .pipe(takeUntil(this.unsubscribe$))
                .subscribe(() => {
                  this.ajsState.reload();
                });
            },
          },
        });
      }
      if (isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'REVIEW_OK') {
        banners.push({
          title: 'As an API reviewer, you have accepted the changes made on this API.',
          type: 'success',
          action: {
            btnText: 'Review changes',
            onClick: () => {
              this.matDialog
                .open<ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult>(ApiReviewDialogComponent, {
                  data: {
                    apiId: api.id,
                  },
                  role: 'alertdialog',
                  id: 'gioApiReviewDialog',
                  width: GIO_DIALOG_WIDTH.MEDIUM,
                })
                .afterClosed()
                .pipe(takeUntil(this.unsubscribe$))
                .subscribe(() => {
                  this.ajsState.reload();
                });
            },
          },
        });
      }
      if (isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'REQUEST_FOR_CHANGES') {
        banners.push({
          title: 'As an API reviewer, you have rejected the changes made on this API.',
          type: 'warning',
          action: {
            btnText: 'Review changes',
            onClick: () => {
              this.matDialog
                .open<ApiReviewDialogComponent, ApiReviewDialogData, ApiReviewDialogResult>(ApiReviewDialogComponent, {
                  data: {
                    apiId: api.id,
                  },
                  role: 'alertdialog',
                  id: 'gioApiReviewDialog',
                  width: GIO_DIALOG_WIDTH.MEDIUM,
                })
                .afterClosed()
                .pipe(takeUntil(this.unsubscribe$))
                .subscribe(() => {
                  this.ajsState.reload();
                });
            },
          },
        });
      }

      // Only for not API reviewer
      if (!isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'IN_REVIEW') {
        // not 'api-reviews-u'
        banners.push({
          title: 'The API reviewer has been asked to review the changes.',
          type: 'info',
        });
      }
      if (!isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'REVIEW_OK') {
        // not 'api-reviews-u'
        banners.push({
          title: 'The API reviewer has accepted the changes made on this API.',
          type: 'success',
        });
      }
      if (!isApiReviewer && this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'REQUEST_FOR_CHANGES') {
        // not 'api-reviews-u'
        banners.push({
          title: 'The API reviewer has asked for changes to be made on this API.',
          type: 'warning',
        });
      }

      // Other banners
      if (this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'DRAFT') {
        banners.push({
          title: 'This API is a draft.',
          type: 'info',
        });
      }

      const apiReviewIsOKOrNotNeeded =
        (this.constants.env.settings?.apiReview?.enabled && api.workflowState === 'REVIEW_OK') ||
        !this.constants.env.settings?.apiReview?.enabled;
      const canUpdateApiDefinition = this.permissionService.hasAnyMatching(['api-definition-u']);
      if (api.deploymentState === 'NEED_REDEPLOY' && apiReviewIsOKOrNotNeeded) {
        banners.push(
          canUpdateApiDefinition
            ? {
                title: 'This API is out of sync.',
                type: 'warning',
                action: {
                  btnText: 'Deploy API',
                  onClick: () => {
                    this.matDialog
                      .open<ApiConfirmDeploymentDialogComponent, ApiConfirmDeploymentDialogData, ApiConfirmDeploymentDialogResult>(
                        ApiConfirmDeploymentDialogComponent,
                        {
                          data: {
                            apiId: api.id,
                          },
                          role: 'alertdialog',
                          id: 'gioApiConfirmDeploymentDialog',
                          width: GIO_DIALOG_WIDTH.MEDIUM,
                        },
                      )
                      .afterClosed()
                      .pipe(takeUntil(this.unsubscribe$))
                      .subscribe(() => {
                        this.ajsState.reload();
                      });
                  },
                },
              }
            : {
                title: 'This API is out of sync.',
                type: 'warning',
              },
        );
      }

      if (api.lifecycleState === 'DEPRECATED') {
        banners.push({
          title: 'This API is deprecated.',
          type: 'error',
        });
      }

      return banners;
    }),
  );

  private unsubscribe$ = new Subject();
  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly permissionService: GioPermissionService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    @Inject(AjsRootScope) private readonly ajsRootScope: IScope,
    private readonly gioMenuService: GioMenuService,
    private readonly apiV2Service: ApiV2Service,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    this.bannerState = localStorage.getItem('gv-api-navigation-banner');

    this.appendPolicyStudio();
    this.appendGeneralGroup();
    this.appendEntrypointsGroup();
    this.appendEndpointsGroup();

    this.selectedItemWithTabs = this.findMenuItemWithTabs();
    this.breadcrumbItems = this.computeBreadcrumbItems();

    this.ajsRootScope.$on('$locationChangeStart', () => {
      this.selectedItemWithTabs = this.findMenuItemWithTabs();
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private appendPolicyStudio() {
    this.subMenuItems.push({
      displayName: 'Policy Studio',
      targetRoute: 'management.apis.ng.policyStudio',
      baseRoute: 'management.apis.ng.policyStudio',
      tabs: undefined,
    });
  }

  private appendGeneralGroup() {
    const generalGroup: GroupItem = {
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

    this.groupItems.push(generalGroup);
  }

  private appendEntrypointsGroup() {
    if (this.permissionService.hasAnyMatching(['api-definition-r', 'api-health-r'])) {
      const entrypointsGroup: GroupItem = {
        title: 'Entrypoints',
        items: [
          {
            displayName: 'General',
            targetRoute: 'management.apis.ng.entrypoints',
            baseRoute: 'management.apis.ng.entrypoints',
          },
        ],
      };
      this.groupItems.push(entrypointsGroup);
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
