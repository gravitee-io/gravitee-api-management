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
import { BehaviorSubject, Subject, forkJoin, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, takeUntil, tap, filter } from 'rxjs/operators';
import { isEqual } from 'lodash';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { Api, ApiV2, ApiV4, Listener, ListenerType } from '../../../entities/management-api-v2';
import { Constants } from '../../../entities/Constants';

export type ApiProductApiTableDS = {
  id: string;
  name: string;
  contextPath: string;
  definition: string;
  version: string;
  lifecycleState?: string;
}[];

interface ApiProductApisTableWrapperFilters extends GioTableWrapperFilters {
  // Add any additional filters here if needed
}

@Component({
  selector: 'api-product-apis',
  templateUrl: './api-product-apis.component.html',
  styleUrls: ['./api-product-apis.component.scss'],
  standalone: false,
})
export class ApiProductApisComponent implements OnInit, OnDestroy {
  displayedColumns = ['picture', 'name', 'contextPath', 'definition', 'version', 'actions'];
  apisTableDS: ApiProductApiTableDS = [];
  apisTableDSUnpaginatedLength = 0;
  filters: ApiProductApisTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };
  searchLabel = 'Search';
  isLoadingData = true;
  apiProductId: string;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private filters$ = new BehaviorSubject<ApiProductApisTableWrapperFilters>(this.filters);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
    private readonly apiProductV2Service: ApiProductV2Service,
    private readonly apiV2Service: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
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

    this.apiProductId = getApiProductId(this.activatedRoute) || '';
    this.initFilters();

    this.filters$
      .pipe(
        debounceTime(100),
        distinctUntilChanged(isEqual),
        tap((filters: ApiProductApisTableWrapperFilters) => {
          // Update URL params
          this.router.navigate([], {
            relativeTo: this.activatedRoute,
            queryParams: {
              q: filters.searchTerm,
              page: filters.pagination.index,
              size: filters.pagination.size,
            },
            queryParamsHandling: 'merge',
          });
        }),
        switchMap((filters: ApiProductApisTableWrapperFilters) => {
          // Call GET /api-products/{apiProductId} to reload the table
          this.isLoadingData = true;
          return this.apiProductV2Service.get(this.apiProductId).pipe(
            catchError((error) => {
              this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
              return of(null);
            }),
          );
        }),
        switchMap((apiProduct) => {
          if (!apiProduct || !apiProduct.apiIds || apiProduct.apiIds.length === 0) {
            this.apisTableDS = [];
            this.apisTableDSUnpaginatedLength = 0;
            this.isLoadingData = false;
            return of([]);
          }

          // Fetch all APIs for the API Product
          const apiObservables = apiProduct.apiIds.map((apiId) =>
            this.apiV2Service.get(apiId).pipe(
              catchError((error) => {
                console.error(`Error loading API ${apiId}:`, error);
                return of(null);
              }),
            ),
          );

          return forkJoin(apiObservables).pipe(
            catchError((error) => {
              this.snackBarService.error(error.error?.message || 'An error occurred while loading APIs');
              return of([]);
            }),
          );
        }),
        tap((apis: (Api | null)[]) => {
          // Filter out null values and apply search filter
          let filteredApis = apis.filter((api): api is Api => api !== null);
          const searchTerm = this.filters.searchTerm?.toLowerCase() || '';

          if (searchTerm) {
            filteredApis = filteredApis.filter(
              (api) =>
                api.name?.toLowerCase().includes(searchTerm) ||
                this.getContextPath(api)?.toLowerCase().includes(searchTerm) ||
                api.apiVersion?.toLowerCase().includes(searchTerm),
            );
          }

          // Apply pagination
          const page = this.filters.pagination?.index || 1;
          const perPage = this.filters.pagination?.size || 10;
          const startIndex = (page - 1) * perPage;
          const endIndex = startIndex + perPage;
          const paginatedApis = filteredApis.slice(startIndex, endIndex);

          this.apisTableDS = this.toApisTableDS(paginatedApis);
          this.apisTableDSUnpaginatedLength = filteredApis.length;
          this.isLoadingData = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  private initFilters(): void {
    const initialSearchValue = this.activatedRoute.snapshot.queryParams.q ?? this.filters.searchTerm;
    const initialPageNumber = this.activatedRoute.snapshot.queryParams.page
      ? Number(this.activatedRoute.snapshot.queryParams.page)
      : this.filters.pagination.index;
    const initialPageSize = this.activatedRoute.snapshot.queryParams.size
      ? Number(this.activatedRoute.snapshot.queryParams.size)
      : this.filters.pagination.size;

    this.filters = {
      searchTerm: initialSearchValue,
      pagination: {
        index: initialPageNumber,
        size: initialPageSize,
      },
    };
    this.filters$.next(this.filters);
  }

  private toApisTableDS(data: Api[]): ApiProductApiTableDS {
    return data.map((api) => ({
      id: api.id,
      name: api.name,
      contextPath: this.getContextPath(api) || '-',
      definition: this.getApiDefinition(api),
      version: api.apiVersion || '-',
      lifecycleState: api.lifecycleState,
    }));
  }

  private getContextPath(api: Api): string | undefined {
    if (api.definitionVersion === 'V2') {
      return (api as ApiV2).contextPath;
    } else if (api.definitionVersion === 'V4') {
      const apiV4 = api as ApiV4;
      // For V4 APIs, get context path from HTTP listeners
      const httpListener = apiV4.listeners?.find((listener) => listener.type === 'HTTP');
      if (httpListener && 'paths' in httpListener) {
        const paths = (httpListener as any).paths;
        if (paths && paths.length > 0) {
          return paths[0];
        }
      }
      return undefined;
    }
    return undefined;
  }

  private getApiDefinition(api: Api): string {
    if (!api.definitionVersion) {
      return 'HTTP Proxy Gravitee';
    }

    switch (api.definitionVersion) {
      case 'V2':
        return 'HTTP Proxy Gravitee';
      case 'V4':
        return this.getLabelType(api as ApiV4);
      case 'FEDERATED':
        return 'Federated API';
      case 'FEDERATED_AGENT':
        return 'Federated A2A Agent';
      default:
        return 'HTTP Proxy Gravitee';
    }
  }

  private getLabelType(api: ApiV4): string {
    if (api.type === 'MESSAGE') {
      return 'Message';
    }
    if (api.type === 'NATIVE') {
      return api.listeners?.map((listener: Listener): ListenerType => listener.type).includes('KAFKA')
        ? 'Kafka Native'
        : 'Native';
    }
    if (api.type === 'MCP_PROXY') {
      return 'MCP Proxy';
    }
    if (api.type === 'LLM_PROXY') {
      return 'LLM Proxy';
    }

    return api.listeners?.map((listener: Listener): ListenerType => listener.type).includes('TCP')
      ? 'TCP Proxy'
      : 'HTTP Proxy Gravitee';
  }

  onFiltersChanged(filters: ApiProductApisTableWrapperFilters): void {
    this.filters = { ...this.filters, ...filters };
    this.filters$.next(this.filters);
  }

  /**
   * Reload the table by calling GET /api-products/{apiProductId}
   * This method directly triggers the GET request to refresh the API list
   */
  private reloadTable(): void {
    // Directly call GET /api-products/{apiProductId} to reload the table
    // This bypasses distinctUntilChanged to ensure the GET call is always made
    this.isLoadingData = true;
    this.apiProductV2Service
      .get(this.apiProductId)
      .pipe(
        catchError((error) => {
          this.snackBarService.error(error.error?.message || 'An error occurred while loading the API Product');
          this.isLoadingData = false;
          return of(null);
        }),
        switchMap((apiProduct) => {
          if (!apiProduct || !apiProduct.apiIds || apiProduct.apiIds.length === 0) {
            this.apisTableDS = [];
            this.apisTableDSUnpaginatedLength = 0;
            this.isLoadingData = false;
            return of([]);
          }

          // Fetch all APIs for the API Product
          const apiObservables = apiProduct.apiIds.map((apiId) =>
            this.apiV2Service.get(apiId).pipe(
              catchError((error) => {
                console.error(`Error loading API ${apiId}:`, error);
                return of(null);
              }),
            ),
          );

          return forkJoin(apiObservables).pipe(
            catchError((error) => {
              this.snackBarService.error(error.error?.message || 'An error occurred while loading APIs');
              return of([]);
            }),
          );
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((apis: (Api | null)[]) => {
        // Filter out null values and apply search filter
        let filteredApis = apis.filter((api): api is Api => api !== null);
        const searchTerm = this.filters.searchTerm?.toLowerCase() || '';

        if (searchTerm) {
          filteredApis = filteredApis.filter(
            (api) =>
              api.name?.toLowerCase().includes(searchTerm) ||
              this.getContextPath(api)?.toLowerCase().includes(searchTerm) ||
              api.apiVersion?.toLowerCase().includes(searchTerm),
          );
        }

        // Apply pagination
        const page = this.filters.pagination?.index || 1;
        const perPage = this.filters.pagination?.size || 10;
        const startIndex = (page - 1) * perPage;
        const endIndex = startIndex + perPage;
        const paginatedApis = filteredApis.slice(startIndex, endIndex);

        this.apisTableDS = this.toApisTableDS(paginatedApis);
        this.apisTableDSUnpaginatedLength = filteredApis.length;
        this.isLoadingData = false;
      });
  }

  /**
   * Add API to this API Product.
   * Placeholder for future work: no-op for current scope. Button remains visible for UX.
   */
  onAddApi(): void {
    // Functionality to be implemented in a future iteration.
  }


  onDeleteApi(api: ApiProductApiTableDS[0]): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Remove API',
          content: 'Please note that once your API is removed from this API Product, consumers will lose access to this API.',
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'removeApiDialog',
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.apiProductV2Service
            .deleteApiFromApiProduct(this.apiProductId, api.id)
            .pipe(
              catchError((error) => {
                this.snackBarService.error(error.error?.message || 'An error occurred while removing the API');
                return of(null);
              }),
              takeUntil(this.unsubscribe$),
            )
            .subscribe(() => {
              this.snackBarService.success(`API "${api.name}" has been removed from the API Product`);
              // Refresh the list
              this.filters$.next(this.filters);
            });
        }
      });
  }

  get hasApis(): boolean {
    return this.apisTableDS.length > 0;
  }
}
