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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';

import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { toSort, toOrder } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Constants } from '../../../entities/Constants';
import { ApiProduct, ApiProductsResponse } from '../../../entities/management-api-v2/api-product';

export type ApiProductTableDS = {
  id: string;
  name: string;
  version: string;
  numberOfApis: number;
  owner: string;
  picture?: string;
}[];

interface ApiProductListTableWrapperFilters extends GioTableWrapperFilters {
  // Add any additional filters here if needed
}

@Component({
  selector: 'api-product-list',
  templateUrl: './api-product-list.component.html',
  styleUrls: ['./api-product-list.component.scss'],
  standalone: false,
})
export class ApiProductListComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'apis', 'version', 'owner', 'actions'];
  apiProductsTableDS: ApiProductTableDS = [];
  apiProductsTableDSUnpaginatedLength = 0;
  filters: ApiProductListTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  searchLabel = 'Search';
  isLoadingData = true;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<ApiProductListTableWrapperFilters>(this.filters);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
    private readonly apiProductV2Service: ApiProductV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.initFilters();

    this.filters$
      .pipe(
        debounceTime(100),
        distinctUntilChanged(isEqual),
        tap((filters: ApiProductListTableWrapperFilters) => {
          // Update URL params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: {
              q: filters.searchTerm,
              page: filters.pagination.index,
              size: filters.pagination.size,
              ...(filters.sort ? { order: toOrder(filters.sort) } : {}),
            },
            queryParamsHandling: 'merge',
          });
        }),
        tap(() => {
          this.isLoadingData = true;
        }),
        switchMap((filters: ApiProductListTableWrapperFilters) => {
          const page = filters.pagination?.index || 1;
          const perPage = filters.pagination?.size || 10;
          return this.apiProductV2Service.list(page, perPage).pipe(
            catchError((error) => {
              this.isLoadingData = false;
              this.snackBarService.error(error.error?.message || 'An error occurred while loading API Products');
              return of({ data: [], pagination: { totalCount: 0 } } as ApiProductsResponse);
            }),
          );
        }),
        tap((response: ApiProductsResponse) => {
          this.apiProductsTableDS = this.toApiProductsTableDS(response.data || []);
          this.apiProductsTableDSUnpaginatedLength = response.pagination?.totalCount || 0;
          this.isLoadingData = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private initFilters(): void {
    const initialSearchValue = this.activatedRoute.snapshot.queryParams.q ?? this.filters.searchTerm;
    const initialPageNumber = this.activatedRoute.snapshot.queryParams.page
      ? Number(this.activatedRoute.snapshot.queryParams.page)
      : this.filters.pagination.index;
    const initialPageSize = this.activatedRoute.snapshot.queryParams.size
      ? Number(this.activatedRoute.snapshot.queryParams.size)
      : this.filters.pagination.size;
    const initialSort = toSort(this.activatedRoute.snapshot.queryParams.order, this.filters.sort);

    this.filters = {
      searchTerm: initialSearchValue,
      sort: initialSort,
      pagination: {
        index: initialPageNumber,
        size: initialPageSize,
      },
    };
    this.filters$.next(this.filters);
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
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  get hasApiProducts(): boolean {
    return this.apiProductsTableDS.length > 0;
  }
}
