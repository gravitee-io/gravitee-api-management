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
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, EMPTY, Observable, of, Subject, combineLatest } from 'rxjs';
import { get, isEqual } from 'lodash';
import { FormControl, FormGroup } from '@angular/forms';
import { AutocompleteOptions, GIO_DIALOG_WIDTH, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';

import { ApplicationService } from '../../../../../services-ngx/application.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { SubscriptionStatus } from '../../../../../entities/subscription/subscription';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';
import { ApplicationSubscriptionCreationDialogComponent } from '../creation';
import { NewSubscriptionEntity } from '../../../../../entities/application';
import { ApplicationSubscriptionService } from '../../../../../services-ngx/application-subscription.service';
import { PlanSecurityType } from '../../../../../entities/plan';

type SubscriptionsTableDS = {
  id: string;
  planName: string;
  securityType: string;
  isSharedApiKey: boolean;
  apiName: string;
  apiPo: string;
  createdAt: Date;
  processedAt: Date;
  startingAt: Date;
  endAt: Date;
  status: string;
  statusBadge: string;
  origin: 'KUBERNETES' | 'MANAGEMENT';
};

type SubscriptionsTableFilters = {
  apis?: string[];
  status?: SubscriptionStatus[];
  apiKey?: string;
};

type SubscriptionsFilters = {
  tableWrapper: GioTableWrapperFilters;
  subscriptionsFilters: SubscriptionsTableFilters;
};

export type ApplicationSubscriptionCreationDialogData = {
  applicationId: string;
};

export type ApplicationSubscriptionCreationDialogResult = {
  planId: string;
  subscriptionToCreate: NewSubscriptionEntity;
};

@Component({
  selector: 'application-subscription-list',
  templateUrl: './application-subscription-list.component.html',
  styleUrls: ['./application-subscription-list.component.scss'],
  standalone: false,
})
export class ApplicationSubscriptionListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public displayedColumns = ['securityType', 'plan', 'api', 'createdAt', 'processedAt', 'startingAt', 'endAt', 'status', 'actions'];
  public subscriptionsTableDS: SubscriptionsTableDS[] = [];
  public subscriptionsCount = 0;
  public filtersForm: FormGroup = new FormGroup({
    apis: new FormControl(),
    status: new FormControl(),
    apiKey: new FormControl(),
  });
  public statuses: { id: SubscriptionStatus; name: string; badge: string }[] = [
    { id: 'ACCEPTED', name: 'Accepted', badge: 'success' },
    { id: 'CLOSED', name: 'Closed', badge: 'neutral' },
    { id: 'PAUSED', name: 'Paused', badge: 'accent' },
    { id: 'PENDING', name: 'Pending', badge: 'warning' },
    { id: 'REJECTED', name: 'Rejected', badge: 'warning' },
    { id: 'RESUMED', name: 'Resumed', badge: 'neutral' },
  ];

  // Create filters stream
  public filtersStream = new BehaviorSubject<SubscriptionsFilters>({
    tableWrapper: {
      pagination: { index: 1, size: 10 },
      searchTerm: '',
    },
    subscriptionsFilters: {
      apis: null,
      status: ['ACCEPTED', 'PAUSED', 'PENDING'],
      apiKey: undefined,
    },
  });
  public isLoadingData = true;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly planService: ApiPlanV2Service,
    private readonly applicationService: ApplicationService,
    private readonly applicationSubscriptionService: ApplicationSubscriptionService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.initFilters();

    this.filtersForm.valueChanges.pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$)).subscribe(({ apis, status, apiKey }) =>
      this.filtersStream.next({
        tableWrapper: {
          ...this.filtersStream.value.tableWrapper,
          pagination: { index: 1, size: this.filtersStream.value.tableWrapper.pagination.size },
        },
        subscriptionsFilters: {
          ...this.filtersStream.value.subscriptionsFilters,
          apis,
          status,
          apiKey,
        },
      }),
    );

    this.filtersStream
      .pipe(
        debounceTime(400),
        distinctUntilChanged(isEqual),
        tap(({ subscriptionsFilters, tableWrapper }) => {
          // Change url params
          this.router.navigate(['.'], {
            relativeTo: this.activatedRoute,
            queryParams: {
              page: tableWrapper.pagination.index,
              size: tableWrapper.pagination.size,
              status: subscriptionsFilters.status?.join(',') || undefined,
              apis: subscriptionsFilters.apis?.join(',') || undefined,
              apiKey: subscriptionsFilters.apiKey || undefined,
            },
          });

          this.filtersForm.controls.apis.setValue(subscriptionsFilters.apis);
          this.filtersForm.controls.status.setValue(subscriptionsFilters.status);
          this.filtersForm.controls.apiKey.setValue(subscriptionsFilters.apiKey);
        }),
        switchMap(({ subscriptionsFilters, tableWrapper }) => {
          return combineLatest([
            this.applicationService.getSubscriptionsPage(
              this.activatedRoute.snapshot.params.applicationId,
              {
                apiKey: subscriptionsFilters.apiKey,
                status: subscriptionsFilters.status,
                apis: subscriptionsFilters.apis,
              },
              tableWrapper.pagination.index,
              tableWrapper.pagination.size,
            ),
            this.applicationService.getLastApplicationFetch(this.activatedRoute.snapshot.params.applicationId),
          ]);
        }),
        catchError(() => {
          this.snackBarService.error('Unable to get subscriptions, please try again');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(([applicationSubscriptions, application]) => {
        this.subscriptionsCount = applicationSubscriptions.page.total_elements;

        this.subscriptionsTableDS = (applicationSubscriptions.data ?? []).map((subscription) => {
          const status = this.statuses.find((status) => status.id === subscription.status);

          const planMetadata = get(applicationSubscriptions.metadata, [subscription.plan], {});
          const apiMetadata = get(applicationSubscriptions.metadata, [subscription.api], {});

          return {
            id: subscription.id,
            apiName: apiMetadata['name'] ? `${apiMetadata['name']} - ${apiMetadata['apiVersion']}` : subscription.api,
            apiPo: apiMetadata['apiPrimaryOwner'] ?? 'Unknown API owner',
            createdAt: subscription.created_at,
            endAt: subscription.ending_at,
            planName: planMetadata['name'] ?? subscription.plan,
            securityType: planMetadata['planMode'] === 'PUSH' ? 'PUSH' : planMetadata['securityType'] ?? 'UNKNOWN',
            isSharedApiKey: application.api_key_mode === 'SHARED' && planMetadata['securityType'] === PlanSecurityType.API_KEY,
            processedAt: subscription.processed_at,
            startingAt: subscription.starting_at,
            status: status?.name,
            statusBadge: status?.badge,
            origin: subscription.origin,
          };
        });
        this.isLoadingData = false;
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next({ ...this.filtersStream.value, tableWrapper: filters });
  }

  public closeSubscription(subscription: SubscriptionsTableDS) {
    const applicationId = this.activatedRoute.snapshot.params.applicationId;

    let content =
      'Are you sure you want to close this subscription? <br> <br> The application will not be able to consume this API anymore.';
    if (subscription.securityType === PlanSecurityType.API_KEY && subscription.isSharedApiKey) {
      content += '<br/>All Api-keys associated to this subscription will be closed and could not be used.';
    }

    return this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        data: {
          title: 'Close subscription',
          content,
        },
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap(() => this.applicationSubscriptionService.closeSubscription(applicationId, subscription.id)),
        takeUntil(this.unsubscribe$),
      )

      .subscribe({
        next: () => {
          this.snackBarService.success('The subscription has been closed');
          this.ngOnInit();
        },
        error: () => {
          this.snackBarService.error('An error occurred while closing the subscription!');
        },
      });
  }

  private initFilters() {
    const initialPageNumber = this.activatedRoute.snapshot.queryParams?.page ? Number(this.activatedRoute.snapshot.queryParams.page) : 1;
    const initialPageSize = this.activatedRoute.snapshot.queryParams?.size ? Number(this.activatedRoute.snapshot.queryParams.size) : 10;
    const initialApiIds = this.activatedRoute.snapshot.queryParams.apis?.split(',') ?? null;
    const initialStatuses = this.activatedRoute.snapshot.queryParams.status?.split(',') ?? ['ACCEPTED', 'PAUSED', 'PENDING'];
    const initialApiKey = this.activatedRoute.snapshot.queryParams.apiKey;

    this.filtersForm.patchValue({
      apis: initialApiIds,
      statuses: initialStatuses,
      apiKey: initialApiKey,
    });

    this.filtersStream.next({
      tableWrapper: {
        searchTerm: '',
        pagination: { index: initialPageNumber, size: initialPageSize },
      },
      subscriptionsFilters: {
        apis: initialApiIds,
        status: initialStatuses,
        apiKey: initialApiKey,
      },
    });
  }

  public resetFilters() {
    this.filtersStream.next({
      tableWrapper: {
        searchTerm: '',
        pagination: { index: 1, size: 10 },
      },
      subscriptionsFilters: {
        apis: null,
        status: ['ACCEPTED', 'PAUSED', 'PENDING'],
        apiKey: null,
      },
    });
  }

  public searchApis: (searchTerm: string) => Observable<AutocompleteOptions> = (searchTerm: string) => {
    return this.applicationService
      .getSubscribedAPIList(this.activatedRoute.snapshot.params.applicationId)
      .pipe(
        map((subscribers) =>
          subscribers
            ?.filter((subscriber) => subscriber.name.includes(searchTerm) && !this.filtersForm.controls.apis.value?.includes(subscriber.id))
            ?.map((subscriber) => ({ value: subscriber.id, label: subscriber.name })),
        ),
      );
  };

  public displayApi: (value: string) => Observable<string> = (value: string) => {
    return this.apiService.get(value).pipe(
      map((api) => `${api.name} (${api.primaryOwner?.displayName})`),
      catchError(() => of(value)),
    );
  };

  protected createSubscription() {
    this.matDialog
      .open<
        ApplicationSubscriptionCreationDialogComponent,
        ApplicationSubscriptionCreationDialogData,
        ApplicationSubscriptionCreationDialogResult
      >(ApplicationSubscriptionCreationDialogComponent, {
        role: 'alertdialog',
        id: 'createSubscriptionDialog',
        data: { applicationId: this.activatedRoute.snapshot.params.applicationId },
        width: GIO_DIALOG_WIDTH.MEDIUM,
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((result) => {
          return this.applicationSubscriptionService.subscribe(
            this.activatedRoute.snapshot.params.applicationId,
            result.planId,
            result.subscriptionToCreate,
          );
        }),
        tap(() => {
          this.snackBarService.success(`Subscription successfully created`);
        }),
        catchError(() => {
          this.snackBarService.error('An error occured during subscription creation');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((subscription) => this.router.navigate(['.', subscription.id], { relativeTo: this.activatedRoute }));
  }
}
