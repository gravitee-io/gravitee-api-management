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

import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  GIO_DIALOG_WIDTH,
  GioBannerModule,
  GioBannerTypes,
  GioBreadcrumbModule,
  GioIconsModule,
  GioMenuModule,
  GioMenuService,
  GioSubmenuModule,
} from '@gravitee/ui-particles-angular';
import { catchError, combineLatest, filter, map, of, startWith, switchMap, tap } from 'rxjs';

import {
  ApiProductConfirmDeploymentDialogComponent,
  ApiProductConfirmDeploymentDialogData,
  ApiProductConfirmDeploymentDialogResult,
} from './api-product-confirm-deployment-dialog/api-product-confirm-deployment-dialog.component';
import {
  ApiProductNavigationTabsComponent,
  ApiProductTabMenuItem,
} from './api-product-navigation-tabs/api-product-navigation-tabs.component';

import { VerifyApiProductDeployResponse } from '../../../entities/management-api-v2/api-product';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

export interface MenuItem {
  displayName: string;
  routerLink: string;
  icon?: string;
  tabs?: ApiProductTabMenuItem[];
  header?: { title: string; subtitle?: string };
}

type TopBanner = {
  title: string;
  body?: string;
  type: GioBannerTypes;
  action?: {
    btnText: string;
    onClick: () => void;
  };
};

@Component({
  selector: 'api-product-navigation',
  templateUrl: './api-product-navigation.component.html',
  styleUrls: ['./api-product-navigation.component.scss'],
  standalone: true,
  imports: [
    RouterModule,
    GioSubmenuModule,
    GioIconsModule,
    GioBreadcrumbModule,
    GioMenuModule,
    GioBannerModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    ApiProductNavigationTabsComponent,
  ],
})
export class ApiProductNavigationComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly gioMenuService = inject(GioMenuService);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  readonly isActionDisabled = signal(false);
  private readonly reloadCount = signal(0);

  private readonly hasUpdatePermission = signal(this.permissionService.hasAnyMatching(['api_product-definition-u']));
  private readonly hasPlanReadPermission = signal(this.permissionService.hasAnyMatching(['api_product-plan-r']));

  readonly subMenuItems = computed<MenuItem[]>(() => {
    const base: MenuItem[] = [{ displayName: 'Configuration', routerLink: 'configuration', icon: 'gio:settings' }];
    const consumers: MenuItem[] = this.hasPlanReadPermission()
      ? [
          {
            displayName: 'Consumers',
            routerLink: 'consumers/plans',
            icon: 'gio:cloud-consumers',
            header: { title: 'Consumers', subtitle: 'Manage how your API Product is consumed' },
            tabs: [{ displayName: 'Plans', routerLink: 'consumers/plans' }],
          },
        ]
      : [];
    return [...base, ...consumers];
  });

  private readonly navTrigger = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
    ),
    { initialValue: null },
  );

  private readonly apiProductData = toSignal(
    combineLatest([
      this.activatedRoute.params.pipe(map(p => p['apiProductId'] ?? null)),
      toObservable(this.reloadCount),
      toObservable(this.apiProductV2Service.planStateVersion),
    ]).pipe(
      switchMap(([apiProductId]) => {
        if (!apiProductId) {
          return of([null, { ok: true }] as [null, VerifyApiProductDeployResponse]);
        }
        return combineLatest([
          this.apiProductV2Service.get(apiProductId).pipe(
            catchError(error => {
              this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
              return of(null);
            }),
          ),
          this.apiProductV2Service
            .verifyDeploy(apiProductId)
            .pipe(catchError(() => of({ ok: false, reason: 'Could not verify deployment compatibility.' }))),
        ]);
      }),
    ),
    { initialValue: [null, { ok: true }] as [null, VerifyApiProductDeployResponse] },
  );

  readonly hasBreadcrumb = toSignal(this.gioMenuService.reduced$, { initialValue: false });

  readonly menuItemsWithActive = computed(() => {
    this.navTrigger();
    return this.subMenuItems().map(item => ({
      ...item,
      active: this.isItemActive(item),
    }));
  });

  readonly selectedItemWithTabs = computed(() => {
    this.navTrigger();
    return this.subMenuItems().find(item => this.isItemActive(item) && (item.tabs?.length ?? 0) > 0) ?? null;
  });

  readonly selectedItemHeader = computed(() => {
    this.navTrigger();
    return this.subMenuItems().find(item => this.isItemActive(item) && item.header)?.header ?? null;
  });

  readonly currentApiProduct = computed(() => this.apiProductData()[0]);

  readonly banners = computed<TopBanner[]>(() => {
    const [apiProduct, verifyDeployResponse] = this.apiProductData();
    if (!apiProduct) {
      return [];
    }

    const banners: TopBanner[] = [];

    if (apiProduct.deploymentState === 'NEED_REDEPLOY' && verifyDeployResponse.ok) {
      banners.push(
        this.hasUpdatePermission()
          ? {
              title: 'This API Product is out of sync.',
              type: 'warning',
              action: {
                btnText: 'Deploy API Product',
                onClick: () => {
                  this.isActionDisabled.set(true);
                  this.matDialog
                    .open<
                      ApiProductConfirmDeploymentDialogComponent,
                      ApiProductConfirmDeploymentDialogData,
                      ApiProductConfirmDeploymentDialogResult
                    >(ApiProductConfirmDeploymentDialogComponent, {
                      data: { apiProductId: apiProduct.id },
                      role: 'alertdialog',
                      id: 'gioApiProductConfirmDeploymentDialog',
                      width: GIO_DIALOG_WIDTH.MEDIUM,
                    })
                    .afterClosed()
                    .pipe(
                      tap(() => {
                        this.isActionDisabled.set(false);
                        this.reloadCount.update(c => c + 1);
                      }),
                      takeUntilDestroyed(this.destroyRef),
                    )
                    .subscribe();
                },
              },
            }
          : {
              title: 'This API Product is out of sync.',
              type: 'warning',
            },
      );
    }

    if (!verifyDeployResponse.ok) {
      banners.push({
        title: 'This API Product cannot be deployed.',
        body: verifyDeployResponse.reason ?? 'Deployment is not allowed.',
        type: 'error',
      });
    }

    return banners;
  });

  private isItemActive(item: MenuItem): boolean {
    if (!item.routerLink) {
      return false;
    }
    return this.router.isActive(this.router.createUrlTree([item.routerLink], { relativeTo: this.activatedRoute }), {
      paths: 'subset',
      queryParams: 'subset',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });
  }
}
