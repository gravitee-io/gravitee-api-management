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

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioAvatarModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';
import { isObject } from 'angular';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { toSort, toOrder } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiProduct, ApiProductsResponse } from '../../../entities/management-api-v2/api-product';

const FILTERS_DEBOUNCE_MS = 100;
const DEFAULT_FILTERS: ApiProductListTableWrapperFilters = {
  pagination: { index: 1, size: 10 },
  searchTerm: '',
};
const EMPTY_API_PRODUCTS_RESPONSE: ApiProductsResponse = {
  data: [],
  pagination: { totalCount: 0 },
};

function queryParamsToFilters(queryParams: Record<string, string>): ApiProductListTableWrapperFilters {
  const searchTerm = queryParams.q ?? DEFAULT_FILTERS.searchTerm;
  const index = queryParams.page ? Number(queryParams.page) : DEFAULT_FILTERS.pagination.index;
  const size = queryParams.size ? Number(queryParams.size) : DEFAULT_FILTERS.pagination.size;
  const sort = queryParams.order ? toSort(queryParams.order, { active: 'name', direction: 'asc' }) : undefined;
  return {
    searchTerm,
    sort,
    pagination: { index, size },
  };
}

function filtersToQueryParams(filters: ApiProductListTableWrapperFilters): Record<string, string | number | null> {
  return {
    q: filters.searchTerm || null,
    page: filters.pagination?.index ?? 1,
    size: filters.pagination?.size ?? 10,
    order: filters.sort ? toOrder(filters.sort) : null,
  };
}

export type ApiProductTableDS = {
  id: string;
  name: string;
  version: string;
  numberOfApis: number;
  owner: string;
  picture?: string;
}[];

type ApiProductListTableWrapperFilters = GioTableWrapperFilters;

@Component({
  selector: 'api-product-list',
  templateUrl: './api-product-list.component.html',
  styleUrls: ['./api-product-list.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatTooltipModule,
    GioIconsModule,
    GioAvatarModule,
    GioTableWrapperModule,
    RouterModule,
  ],
})
export class ApiProductListComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  displayedColumns = ['picture', 'name', 'apis', 'version', 'owner', 'actions'];
  apiProductsTableDS: ApiProductTableDS = [];
  apiProductsTableDSUnpaginatedLength = 0;
  searchLabel = 'Search';
  isLoadingData = true;
  canCreateApiProduct = this.permissionService.hasAnyMatching(['environment-api_product-c']);

  /** Filters derived from router query params (single source of truth) */
  readonly filters = toSignal(this.activatedRoute.queryParams.pipe(map(params => queryParamsToFilters(params))), {
    initialValue: queryParamsToFilters(this.activatedRoute.snapshot.queryParams as Record<string, string>),
  });

  ngOnInit(): void {
    this.activatedRoute.queryParams
      .pipe(
        debounceTime(FILTERS_DEBOUNCE_MS),
        map(params => queryParamsToFilters(params)),
        distinctUntilChanged(isEqual),
        tap(() => {
          this.isLoadingData = true;
        }),
        switchMap((filters: ApiProductListTableWrapperFilters) => {
          const page = filters.pagination?.index || 1;
          const perPage = filters.pagination?.size || 10;
          return this.apiProductV2Service.list(page, perPage).pipe(
            catchError(error => {
              this.isLoadingData = false;
              this.cdr.markForCheck();
              this.snackBarService.error(this.getErrorMessage(error, 'An error occurred while loading API Products'));
              return of(EMPTY_API_PRODUCTS_RESPONSE);
            }),
          );
        }),
        tap((response: ApiProductsResponse) => {
          this.apiProductsTableDS = this.toApiProductsTableDS(response.data || []);
          this.apiProductsTableDSUnpaginatedLength = response.pagination?.totalCount || 0;
          this.isLoadingData = false;
          this.cdr.markForCheck();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getErrorMessage(error: unknown, fallback: string): string {
    if (error && isObject(error) && 'error' in error) {
      const err = (error as { error?: { message?: string } }).error;
      if (err && isObject(err) && typeof err.message === 'string') {
        return err.message;
      }
    }
    if (error instanceof Error && error.message) {
      return error.message;
    }
    return fallback;
  }

  private toApiProductsTableDS(data: ApiProduct[]): ApiProductTableDS {
    if (!data || data.length === 0) {
      return [];
    }
    return data.map((product: ApiProduct) => ({
      id: product.id,
      name: product.name,
      version: product.version,
      numberOfApis: product.apiIds?.length || 0,
      owner: product.primaryOwner?.displayName || 'N/A',
      picture: product._links?.pictureUrl,
    }));
  }

  onFiltersChanged(filters: ApiProductListTableWrapperFilters): void {
    const mergedFilters: ApiProductListTableWrapperFilters = {
      ...this.filters(),
      ...filters,
    };
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: filtersToQueryParams(mergedFilters),
      queryParamsHandling: 'merge',
    });
  }

  get hasApiProducts(): boolean {
    return this.apiProductsTableDSUnpaginatedLength > 0;
  }

  get hasActiveSearch(): boolean {
    return !!this.filters().searchTerm?.trim();
  }
}
