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

import { Component, DestroyRef, inject, Injector, OnInit } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { isEqual } from 'lodash';
import {
  GIO_DIALOG_WIDTH,
  GioAvatarModule,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';

import {
  ApiProductAddApiDialogComponent,
  ApiProductAddApiDialogData,
  ApiProductAddApiDialogResult,
} from './add-api-dialog/api-product-add-api-dialog.component';

import { Api, ApiSearchQuery, ApisResponse, apiSortByParamFromString } from '../../../entities/management-api-v2';
import { filtersToQueryParams, toOrder, toSort } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.util';
import { getApiContextPath } from '../../../shared/utils/api-access.util';
import { GioTableWrapperFilters } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { ApiProductV2Service } from '../../../services-ngx/api-product-v2.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export type ApiProductApiTableDS = {
  id: string;
  name: string;
  picture?: string;
  contextPath: string;
  definition: string;
  version: string;
  lifecycleState?: string;
}[];

interface ApiProductApisTableWrapperFilters extends GioTableWrapperFilters {}

const DEFAULT_FILTERS: ApiProductApisTableWrapperFilters = {
  pagination: { index: 1, size: 25 },
  searchTerm: '',
};

function queryParamsToFilters(queryParams: Record<string, string>): ApiProductApisTableWrapperFilters {
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

@Component({
  selector: 'api-product-apis',
  templateUrl: './api-product-apis.component.html',
  styleUrls: ['./api-product-apis.component.scss'],
  imports: [MatButtonModule, MatIconModule, MatTableModule, MatTooltipModule, GioAvatarModule, GioIconsModule, GioTableWrapperModule],
  standalone: true,
})
export class ApiProductApisComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly apiV2Service = inject(ApiV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly matDialog = inject(MatDialog);

  readonly displayedColumns = ['picture', 'name', 'contextPath', 'definition', 'version', 'actions'] as const;
  apisTableDS: ApiProductApiTableDS = [];
  apisTableDSUnpaginatedLength = 0;
  searchLabel = 'Search APIs...';
  isLoadingData = true;
  readonly apiProductId = toSignal((this.activatedRoute.parent?.params ?? of({})).pipe(map(p => p['apiProductId'] ?? '')), {
    initialValue: '',
  });

  /** Filters derived from router query params (single source of truth) */
  readonly filters = toSignal(this.activatedRoute.queryParams.pipe(map(params => queryParamsToFilters(params))), {
    initialValue: queryParamsToFilters(this.activatedRoute.snapshot.queryParams as Record<string, string>),
  });

  private readonly reloadTrigger$ = new Subject<void>();

  private static readonly LOAD_API_PRODUCT_ERROR = 'An error occurred while loading the API Product';

  private handleError<T>(message: string, fallback: Observable<T>): (error: unknown) => Observable<T> {
    return (error: unknown) => {
      this.snackBarService.error((error as { error?: { message?: string } })?.error?.message || message);
      return fallback;
    };
  }

  private subscribeWithSuccessReload<T>(observable$: Observable<T>, successMessage: string | ((value: T) => string)): void {
    observable$
      .pipe(
        tap(value => {
          const message = typeof successMessage === 'function' ? successMessage(value) : successMessage;
          this.snackBarService.success(message);
          this.reloadTrigger$.next();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  ngOnInit(): void {
    this.initApisTableSubscription();
  }

  private initApisTableSubscription(): void {
    combineLatest([
      this.activatedRoute.queryParams.pipe(
        debounceTime(100),
        map(params => queryParamsToFilters(params)),
        distinctUntilChanged(isEqual),
      ),
      toObservable(this.apiProductId, { injector: this.injector }).pipe(filter(id => !!id)),
      this.reloadTrigger$.pipe(startWith(undefined)),
    ])
      .pipe(
        map(([filters, apiProductId]) => ({ filters, apiProductId })),
        switchMap(({ filters, apiProductId }) => this.loadApisForProduct(apiProductId, filters)),
        tap(apisResponse => this.processAndDisplayApis(apisResponse)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private loadApisForProduct(apiProductId: string, filters: ApiProductApisTableWrapperFilters): Observable<ApisResponse> {
    this.isLoadingData = true;
    const searchBody: ApiSearchQuery = {
      apiProductId,
      query: filters.searchTerm || undefined,
    };
    const order = toOrder(filters.sort) ?? 'name';
    return this.apiV2Service
      .search(searchBody, apiSortByParamFromString(order), filters.pagination?.index ?? 1, filters.pagination?.size ?? 25)
      .pipe(
        catchError(
          this.handleError('An error occurred while loading APIs', of({ data: [], pagination: { totalCount: 0 } } as ApisResponse)),
        ),
      );
  }

  private processAndDisplayApis(apisResponse: ApisResponse): void {
    const apis = apisResponse?.data ?? [];
    this.apisTableDS = this.toApisTableDS(apis);
    this.apisTableDSUnpaginatedLength = apisResponse?.pagination?.totalCount ?? apis.length;
    this.isLoadingData = false;
  }

  private toApisTableDS(data: Api[]): ApiProductApiTableDS {
    return data.map(api => ({
      id: api.id,
      name: api.name,
      picture: api._links?.['pictureUrl'],
      contextPath: getApiContextPath(api) || '-',
      definition: 'HTTP Proxy Gravitee',
      version: api.apiVersion || '-',
      lifecycleState: api.lifecycleState,
    }));
  }

  onFiltersChanged(filters: ApiProductApisTableWrapperFilters): void {
    const mergedFilters: ApiProductApisTableWrapperFilters = {
      ...this.filters(),
      ...filters,
    };
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: filtersToQueryParams(mergedFilters),
      queryParamsHandling: 'merge',
    });
  }

  onAddApi(): void {
    const apiProductId = this.apiProductId();
    const addApis$ = this.apiProductV2Service.get(apiProductId).pipe(
      catchError(this.handleError(ApiProductApisComponent.LOAD_API_PRODUCT_ERROR, EMPTY)),
      switchMap(apiProduct =>
        this.matDialog
          .open<ApiProductAddApiDialogComponent, ApiProductAddApiDialogData, ApiProductAddApiDialogResult>(
            ApiProductAddApiDialogComponent,
            {
              width: GIO_DIALOG_WIDTH.MEDIUM,
              data: {
                apiProductId,
                existingApiIds: apiProduct.apiIds || [],
              },
              role: 'dialog',
              id: 'addApiDialog',
            },
          )
          .afterClosed()
          .pipe(map(selectedApis => ({ apiProduct, selectedApis }))),
      ),
      filter(
        (ctx): ctx is { apiProduct: NonNullable<typeof ctx.apiProduct>; selectedApis: ApiProductAddApiDialogResult } =>
          (ctx.selectedApis?.length ?? 0) > 0,
      ),
      switchMap(({ apiProduct, selectedApis }) => this.addApisToProduct(selectedApis, apiProductId, apiProduct.apiIds || [])),
    );
    this.subscribeWithSuccessReload(addApis$, selectedApis => {
      const apiNames = selectedApis.map(api => api.name).join(', ');
      return selectedApis.length === 1
        ? `API "${apiNames}" has been added to the API Product`
        : `${selectedApis.length} APIs have been added to the API Product`;
    });
  }

  private addApisToProduct(
    selectedApis: ApiProductAddApiDialogResult,
    apiProductId: string,
    currentApiIds: string[],
  ): Observable<ApiProductAddApiDialogResult> {
    const newApiIds = selectedApis.map(api => api.id);
    const updatedApiIds = [...currentApiIds, ...newApiIds];
    return this.apiProductV2Service.updateApiProductApis(apiProductId, updatedApiIds).pipe(
      map(() => selectedApis),
      catchError(this.handleError('An error occurred while adding the APIs', EMPTY)),
    );
  }

  onDeleteApi(api: ApiProductApiTableDS[0]): void {
    const apiProductId = this.apiProductId();
    const removeApi$ = this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
        width: GIO_DIALOG_WIDTH.SMALL,
        data: {
          title: 'Remove API',
          content: 'Please note that once your API is removed from this API Product, consumers will lose access to this API.',
          confirmButton: 'Remove',
        },
        role: 'alertdialog',
        id: 'removeApiDialog',
      })
      .afterClosed()
      .pipe(
        filter((result): result is true => result === true),
        switchMap(() => this.removeApiFromProduct(api, apiProductId)),
      );
    this.subscribeWithSuccessReload(removeApi$, `API "${api.name}" has been removed from the API Product`);
  }

  private removeApiFromProduct(api: ApiProductApiTableDS[0], apiProductId: string): Observable<void> {
    return this.apiProductV2Service.get(apiProductId).pipe(
      catchError(this.handleError(ApiProductApisComponent.LOAD_API_PRODUCT_ERROR, EMPTY)),
      switchMap(apiProduct => {
        const updatedApiIds = (apiProduct.apiIds || []).filter(id => id !== api.id);
        return this.apiProductV2Service.updateApiProductApis(apiProductId, updatedApiIds).pipe(
          catchError(this.handleError('An error occurred while removing the API', EMPTY)),
          map(() => undefined),
        );
      }),
    );
  }

  get hasApis(): boolean {
    return this.apisTableDS.length > 0;
  }
}
