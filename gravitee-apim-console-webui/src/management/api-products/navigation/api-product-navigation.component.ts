/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { ApiProduct } from '../../../entities/management-api-v2/api-product';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

interface MenuItem {
  displayName: string;
  routerLink: string;
  icon?: string;
}

@Component({
  selector: 'api-product-navigation',
  templateUrl: './api-product-navigation.component.html',
  styleUrls: ['./api-product-navigation.component.scss'],
  standalone: false,
})
export class ApiProductNavigationComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public currentApiProduct: ApiProduct | null = null;
  public apiProductId: string;
  public isLoading = false;
  public subMenuItems: MenuItem[] = [
    {
      displayName: 'Configuration',
      routerLink: 'configuration',
      icon: 'gio:settings',
    },
    {
      displayName: 'APIs',
      routerLink: 'apis',
      icon: 'gio:cloud',
    },
    {
      displayName: 'Consumers',
      routerLink: 'consumers',
      icon: 'gio:users',
    },
  ];
  public hasBreadcrumb = false;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly gioMenuService: GioMenuService,
    private readonly apiProductV2Service: ApiProductV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.gioMenuService.reduced$.pipe(takeUntil(this.unsubscribe$)).subscribe((reduced) => {
      this.hasBreadcrumb = reduced;
    });

    // Get apiProductId from route params (check parent route if not in current route)
    const getApiProductId = (route: ActivatedRoute): string | null => {
      if (route.snapshot.params['apiProductId']) {
        return route.snapshot.params['apiProductId'];
      }
      if (route.parent) {
        return getApiProductId(route.parent);
      }
      return null;
    };

    this.activatedRoute.params
      .pipe(
        tap(() => {
          const apiProductId = getApiProductId(this.activatedRoute);
          if (apiProductId) {
            this.apiProductId = apiProductId;
            this.isLoading = true;
            this.currentApiProduct = null;
          }
        }),
        switchMap(() => {
          const apiProductId = getApiProductId(this.activatedRoute);
          if (apiProductId) {
            return this.apiProductV2Service.get(apiProductId).pipe(
              catchError((error) => {
                this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
                return of(null);
              }),
            );
          }
          return of(null);
        }),
        tap((apiProduct) => {
          this.currentApiProduct = apiProduct;
          this.isLoading = false;
        }),
        switchMap(() => this.router.events),
        filter((event) => event instanceof NavigationEnd),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  isActive(item: MenuItem): boolean {
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

