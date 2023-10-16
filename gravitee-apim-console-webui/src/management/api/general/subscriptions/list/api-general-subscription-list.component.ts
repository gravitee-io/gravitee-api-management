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
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { BehaviorSubject, EMPTY, Observable, of, Subject } from 'rxjs';
import { StateService } from '@uirouter/core';
import { isEqual } from 'lodash';
import { FormControl, FormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AutocompleteOptions } from '@gravitee/ui-particles-angular';
import * as _ from 'lodash';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { SubscriptionStatus } from '../../../../../entities/subscription/subscription';
import { GioTableWrapperFilters } from '../../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { ApiSubscriptionV2Service } from '../../../../../services-ngx/api-subscription-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Api, ApiV4, Plan } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';
import {
  ApiPortalSubscriptionCreationDialogComponent,
  ApiPortalSubscriptionCreationDialogData,
  ApiPortalSubscriptionCreationDialogResult,
} from '../components/dialogs/creation/api-portal-subscription-creation-dialog.component';
import { ApplicationService } from '../../../../../services-ngx/application.service';

type SubscriptionsTableDS = {
  id: string;
  plan: string;
  application: string;
  createdAt: Date;
  processedAt: Date;
  startingAt: Date;
  endAt: Date;
  status: string;
};

@Component({
  selector: 'api-general-subscription-list',
  template: require('./api-general-subscription-list.component.html'),
  styles: [require('./api-general-subscription-list.component.scss')],
})
export class ApiGeneralSubscriptionListComponent implements OnInit, OnDestroy {
  public displayedColumns = ['plan', 'application', 'createdAt', 'processedAt', 'startingAt', 'endAt', 'status', 'actions'];
  public subscriptionsTableDS: SubscriptionsTableDS[] = [];
  public nbTotalSubscriptions = 0;

  public filtersForm: FormGroup;

  public plans: Plan[] = [];
  public statuses: { id: SubscriptionStatus; name: string }[] = [
    { id: 'ACCEPTED', name: 'Accepted' },
    { id: 'CLOSED', name: 'Closed' },
    { id: 'PAUSED', name: 'Paused' },
    { id: 'PENDING', name: 'Pending' },
    { id: 'REJECTED', name: 'Rejected' },
    { id: 'RESUMED', name: 'Resumed' },
  ];

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  // Create filters stream
  public filtersStream = new BehaviorSubject<{
    tableWrapper: GioTableWrapperFilters;
    subscriptionsFilters: {
      planIds?: string[];
      applicationIds?: string[];
      statuses?: SubscriptionStatus[];
      apiKey?: string;
    };
  }>({
    tableWrapper: {
      pagination: { index: 1, size: 10 },
      searchTerm: '',
    },
    subscriptionsFilters: {
      planIds: null,
      applicationIds: null,
      statuses: ['ACCEPTED', 'PAUSED', 'PENDING'],
      apiKey: undefined,
    },
  });

  public api: Api;

  public isLoadingData = true;
  public canUpdate = false;
  private isKubernetesOrigin = false;
  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiV2Service,
    private readonly apiPlanService: ApiPlanV2Service,
    private readonly apiSubscriptionService: ApiSubscriptionV2Service,
    private readonly applicationService: ApplicationService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
  ) {}

  public ngOnInit(): void {
    this.initFilters();

    this.filtersForm.valueChanges
      .pipe(distinctUntilChanged(isEqual), takeUntil(this.unsubscribe$))
      .subscribe(({ planIds, applicationIds, statuses, apiKey }) =>
        this.filtersStream.next({
          tableWrapper: {
            ...this.filtersStream.value.tableWrapper,
            // go to first page when filters change
            pagination: { index: 1, size: this.filtersStream.value.tableWrapper.pagination.size },
          },
          subscriptionsFilters: {
            ...this.filtersStream.value.subscriptionsFilters,
            planIds,
            applicationIds,
            statuses,
            apiKey,
          },
        }),
      );

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api) => {
          this.api = api;
          this.isKubernetesOrigin = api.definitionContext?.origin === 'KUBERNETES';
          this.canUpdate =
            this.permissionService.hasAnyMatching(['api-subscription-u']) &&
            !this.isKubernetesOrigin &&
            this.api.definitionVersion !== 'V1';
        }),
        switchMap(() => this.apiPlanService.list(this.ajsStateParams.apiId, null, null, null, 1, 9999)),
        tap((plansResponse) => (this.plans = plansResponse.data)),
        catchError((error) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    this.filtersStream
      .pipe(
        debounceTime(400),
        distinctUntilChanged(isEqual),
        tap(({ subscriptionsFilters, tableWrapper }) => {
          // Change url params
          this.ajsState.go(
            '.',
            {
              page: tableWrapper.pagination.index,
              size: tableWrapper.pagination.size,
              status: subscriptionsFilters.statuses?.join(','),
              plan: subscriptionsFilters.planIds?.join(','),
              application: subscriptionsFilters.applicationIds?.join(','),
              apiKey: subscriptionsFilters.apiKey,
            },
            { notify: false },
          );
        }),
        tap(({ subscriptionsFilters }) => {
          this.filtersForm.get('planIds').setValue(subscriptionsFilters.planIds);
          this.filtersForm.get('applicationIds').setValue(subscriptionsFilters.applicationIds);
          this.filtersForm.get('statuses').setValue(subscriptionsFilters.statuses);
          this.filtersForm.get('apiKey').setValue(subscriptionsFilters.apiKey);
        }),
        switchMap(({ subscriptionsFilters, tableWrapper }) =>
          this.apiSubscriptionService
            .list(
              this.ajsStateParams.apiId,
              tableWrapper.pagination.index,
              tableWrapper.pagination.size,
              subscriptionsFilters.statuses,
              subscriptionsFilters.applicationIds,
              subscriptionsFilters.planIds,
              subscriptionsFilters.apiKey,
              ['application', 'plan'],
            )
            .pipe(
              catchError(() => {
                this.snackBarService.error('Unable to try the request, please try again');
                return EMPTY;
              }),
            ),
        ),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((apiSubscriptionsResponse) => {
        this.nbTotalSubscriptions = apiSubscriptionsResponse.pagination.totalCount;
        this.subscriptionsTableDS = (apiSubscriptionsResponse.data ?? []).map((subscription) => ({
          id: subscription.id,
          application: `${subscription.application?.name} (${subscription.application?.primaryOwner?.displayName})`,
          createdAt: subscription.createdAt,
          endAt: subscription.endingAt,
          plan: subscription.plan?.name,
          processedAt: subscription.processedAt,
          startingAt: subscription.startingAt,
          status: this.statuses.find((status) => status.id === subscription.status)?.name,
        }));
        this.isLoadingData = false;
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public navigateToSubscription(subscriptionId: string): void {
    this.ajsState.go(`management.apis.subscription.edit`, { subscriptionId });
  }

  public exportAsCSV(): void {
    const apiId = this.ajsStateParams.apiId;
    const page = '1';
    const perPage = `${this.nbTotalSubscriptions}`;
    const planIds: string[] = this.filtersStream.value.subscriptionsFilters.statuses;
    const applicationIds = this.filtersStream.value.subscriptionsFilters.applicationIds;
    const statuses = this.filtersStream.value.subscriptionsFilters.planIds;
    const apiKey = this.filtersStream.value.subscriptionsFilters.apiKey;

    this.apiSubscriptionService
      .exportAsCSV(apiId, page, perPage, planIds, applicationIds, statuses, apiKey)
      .pipe(
        tap((blob) => {
          let fileName = `subscriptions-${this.api.name}-${this.api.apiVersion}-${_.now()}.csv`;
          fileName = fileName.replace(/[\s]/gi, '-').replace(/[^\w]/gi, '-');

          const anchor = document.createElement('a');
          anchor.download = fileName;
          anchor.href = (window.webkitURL || window.URL).createObjectURL(blob);
          anchor.click();
        }),
        catchError(() => {
          this.snackBarService.error('An error occurred while exporting the subscriptions.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public createSubscription(): void {
    this.matDialog
      .open<
        ApiPortalSubscriptionCreationDialogComponent,
        ApiPortalSubscriptionCreationDialogData,
        ApiPortalSubscriptionCreationDialogResult
      >(ApiPortalSubscriptionCreationDialogComponent, {
        role: 'alertdialog',
        id: 'createSubscriptionDialog',
        data: {
          availableSubscriptionEntrypoints: this.getApiSubscriptionEntrypoints(this.api),
          plans: this.plans.filter((plan) => plan.status),
        },
      })
      .afterClosed()
      .pipe(
        filter((result) => !!result),
        switchMap((result) => {
          if (!result?.apiKeyMode) {
            return of(result);
          }
          return this.applicationService.update({ ...result.application, api_key_mode: result.apiKeyMode }).pipe(map(() => result));
        }),
        switchMap((result) => {
          return this.apiSubscriptionService.create(this.api.id, result.subscriptionToCreate);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (subscription) => {
          this.snackBarService.success(`Subscription successfully created`);
          this.ajsState.go('management.apis.subscription.edit', { subscriptionId: subscription.id });
        },
        (err) => this.snackBarService.error(err.message),
      );
  }

  onFiltersChanged(filters: GioTableWrapperFilters) {
    this.filtersStream.next({ ...this.filtersStream.value, tableWrapper: filters });
  }

  private initFilters() {
    const initialPageNumber = this.ajsStateParams.page ? Number(this.ajsStateParams.page) : 1;
    const initialPageSize = this.ajsStateParams.size ? Number(this.ajsStateParams.size) : 10;
    const initialPlanIds: string[] = this.ajsStateParams.plan ? this.ajsStateParams.plan.split(',') : null;
    const initialApplicationIds = this.ajsStateParams.application ? this.ajsStateParams.application.split(',') : null;
    const initialStatuses = this.ajsStateParams.status ? this.ajsStateParams.status.split(',') : ['ACCEPTED', 'PAUSED', 'PENDING'];
    const initialApiKey = this.ajsStateParams.apiKey ? this.ajsStateParams.apiKey : undefined;

    this.filtersForm = new FormGroup({
      planIds: new FormControl(initialPlanIds),
      applicationIds: new FormControl(initialApplicationIds),
      statuses: new FormControl(initialStatuses),
      apiKey: new FormControl(initialApiKey),
    });

    this.filtersStream.next({
      tableWrapper: {
        searchTerm: '',
        pagination: { index: initialPageNumber, size: initialPageSize },
      },
      subscriptionsFilters: {
        planIds: initialPlanIds,
        applicationIds: initialApplicationIds,
        statuses: initialStatuses,
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
        planIds: null,
        applicationIds: null,
        statuses: ['ACCEPTED', 'PAUSED', 'PENDING'],
        apiKey: undefined,
      },
    });
  }

  private getApiSubscriptionEntrypoints(api: Api) {
    if (api.definitionVersion !== 'V4') {
      return [];
    }

    return (api as ApiV4).listeners.filter((listener) => listener.type === 'SUBSCRIPTION').flatMap((listener) => listener.entrypoints);
  }

  public searchApplications: (searchTerm: string) => Observable<AutocompleteOptions> = (searchTerm: string) => {
    return this.apiService.getSubscribers(this.ajsStateParams.apiId, searchTerm, 1, 20).pipe(
      map((subscribers) =>
        subscribers?.data?.map((subscriber) => ({
          value: subscriber.id,
          label: `${subscriber.name} (${subscriber.primaryOwner?.displayName})`,
        })),
      ),
    );
  };

  public displayApplication: (value: string) => Observable<string> = (value: string) => {
    return this.applicationService.getById(value).pipe(
      map((application) => `${application.name} (${application.owner?.displayName})`),
      catchError(() => {
        return of(value);
      }),
    );
  };
}
