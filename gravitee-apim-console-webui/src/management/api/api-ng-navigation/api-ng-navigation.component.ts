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
import { MenuGroupItem, MenuItem } from './MenuGroupItem';
import { ApiNgV4MenuService } from './api-ng-v4-menu.service';
import { ApiNgV1V2MenuService } from './api-ng-v1-v2-menu.service';

import { AjsRootScope, CurrentUserService, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import UserService from '../../../services/user.service';
import { Constants } from '../../../entities/Constants';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api } from '../../../entities/management-api-v2';

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
  providers: [ApiNgV1V2MenuService, ApiNgV4MenuService],
})
export class ApiNgNavigationComponent implements OnInit, OnDestroy {
  public currentApi: Api;
  public subMenuItems: MenuItem[] = [];
  public groupItems: MenuGroupItem[] = [];
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
    private readonly apiNgV1V2MenuService: ApiNgV1V2MenuService,
    private readonly apiNgV4MenuService: ApiNgV4MenuService,
  ) {}

  ngOnInit() {
    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    this.bannerState = localStorage.getItem('gv-api-navigation-banner');

    this.apiV2Service
      .getLastApiFetch(this.ajsStateParams.apiId)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((api) => {
        this.currentApi = api;
        const { groupItems, subMenuItems } =
          api.definitionVersion !== 'V4' ? this.apiNgV1V2MenuService.getMenu() : this.apiNgV4MenuService.getMenu();
        this.groupItems = groupItems;
        this.subMenuItems = subMenuItems;

        this.selectedItemWithTabs = this.findMenuItemWithTabs();
        this.breadcrumbItems = this.computeBreadcrumbItems();

        this.ajsRootScope.$on('$locationChangeStart', () => {
          this.selectedItemWithTabs = this.findMenuItemWithTabs();
        });
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
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
