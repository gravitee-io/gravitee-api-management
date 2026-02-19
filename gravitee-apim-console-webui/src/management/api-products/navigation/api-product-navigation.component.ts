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

import { AsyncPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioBreadcrumbModule, GioIconsModule, GioMenuModule, GioMenuService, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { catchError, filter, map, of, startWith, switchMap } from 'rxjs';

import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export interface MenuItem {
  displayName: string;
  routerLink: string;
  icon?: string;
}

@Component({
  selector: 'api-product-navigation',
  templateUrl: './api-product-navigation.component.html',
  styleUrls: ['./api-product-navigation.component.scss'],
  standalone: true,
  imports: [AsyncPipe, RouterModule, GioSubmenuModule, GioIconsModule, GioBreadcrumbModule, GioMenuModule, MatIconModule, MatTooltipModule],
})
export class ApiProductNavigationComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly gioMenuService = inject(GioMenuService);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);

  readonly subMenuItems: MenuItem[] = [{ displayName: 'Configuration', routerLink: 'configuration', icon: 'gio:settings' }];
  readonly hasBreadcrumb = toSignal(this.gioMenuService.reduced$, { initialValue: false });

  private readonly navTrigger = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
    ),
    { initialValue: null },
  );
  readonly menuItemsWithActive = computed(() => {
    this.navTrigger();
    return this.subMenuItems.map(item => ({
      ...item,
      active: this.isItemActive(item),
    }));
  });

  readonly currentApiProduct$ = this.activatedRoute.params.pipe(
    map(p => p['apiProductId'] ?? null),
    switchMap(apiProductId => {
      if (apiProductId) {
        return this.apiProductV2Service.get(apiProductId).pipe(
          catchError(error => {
            this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
            return of(null);
          }),
        );
      }
      return of(null);
    }),
  );

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
