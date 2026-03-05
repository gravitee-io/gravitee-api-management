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
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, EMPTY, filter, map, Observable, of, switchMap, tap } from 'rxjs';
import { isEqual, now, omitBy, isNil } from 'lodash';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AutocompleteOptions, GioFormTagsInputModule, GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe } from '@angular/common';

import { ApiProductSubscriptionV2Service } from '../../../../services-ngx/api-product-subscription-v2.service';
import { ApiProductPlanV2Service } from '../../../../services-ngx/api-product-plan-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApplicationService } from '../../../../services-ngx/application.service';
import { GioTableWrapperModule } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioTableWrapperFilters } from '../../../../shared/components/gio-table-wrapper/gio-table-wrapper.component';
import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import {
  DEFAULT_SUBSCRIPTION_FILTER_STATUSES,
  SubscriptionsTableDS,
  SUBSCRIPTION_STATUS_DISPLAY,
  SubscriptionStatus,
} from '../../../../entities/subscription/subscription';
import { Plan } from '../../../../entities/management-api-v2';
import {
  ApiPortalSubscriptionCreationDialogComponent,
  ApiPortalSubscriptionCreationDialogData,
  ApiPortalSubscriptionCreationDialogResult,
} from '../../../api/subscriptions/components/dialogs/creation/api-portal-subscription-creation-dialog.component';
import { ApiSubscriptionsModule } from '../../../api/subscriptions/api-subscriptions.module';

export type SubscriptionsFiltersValue = {
  planIds: string[] | null;
  applicationIds: string[] | null;
  statuses: SubscriptionStatus[];
  apiKey: string | undefined;
};

type SubscriptionsFiltersForm = {
  planIds: FormControl<string[] | null>;
  applicationIds: FormControl<string[] | null>;
  statuses: FormControl<SubscriptionStatus[]>;
  apiKey: FormControl<string | undefined>;
};

@Component({
  selector: 'api-product-subscription-list',
  templateUrl: './api-product-subscription-list.component.html',
  styleUrls: ['./api-product-subscription-list.component.scss'],
  standalone: true,
  imports: [
    RouterModule,
    ReactiveFormsModule,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    GioFormTagsInputModule,
    GioIconsModule,
    GioLoaderModule,
    GioPermissionModule,
    GioTableWrapperModule,
    ApiSubscriptionsModule,
  ],
})
export class ApiProductSubscriptionListComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly subscriptionService = inject(ApiProductSubscriptionV2Service);
  private readonly planService = inject(ApiProductPlanV2Service);
  private readonly applicationService = inject(ApplicationService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly displayedColumns = [
    'securityType',
    'plan',
    'application',
    'createdAt',
    'processedAt',
    'startingAt',
    'endAt',
    'status',
    'actions',
  ];

  protected readonly statuses = SUBSCRIPTION_STATUS_DISPLAY;

  private readonly apiProductId = (this.activatedRoute.snapshot.params as { apiProductId: string }).apiProductId;

  // Source of truth: The URL query parameters
  protected readonly filters = toSignal(
    this.activatedRoute.queryParams.pipe(
      map(params => this.parseFiltersFromQueryParams(params)),
      distinctUntilChanged(isEqual),
    ),
  );

  protected readonly filtersForm: FormGroup<SubscriptionsFiltersForm> = this.buildFiltersForm();

  protected readonly plans = toSignal(
    this.planService.list(this.apiProductId, undefined, ['PUBLISHED'], undefined, undefined, 1, 9999).pipe(
      map(response => response.data.filter(plan => plan.security?.type !== 'KEY_LESS')),
      catchError(() => {
        this.handleError('Unable to load plans. Please try again.');
        return of([] as Plan[]);
      }),
    ),
    { initialValue: [] as Plan[] },
  );

  protected readonly canUpdate = computed(() => this.permissionService.hasAnyMatching(['api_product-subscription-u']));

  private readonly subscriptionsData = toSignal(
    toObservable(this.filters).pipe(
      debounceTime(400),
      distinctUntilChanged(isEqual),
      tap(() => this.isLoadingData.set(true)),
      switchMap(filters =>
        this.subscriptionService
          .list(this.apiProductId, {
            page: `${filters.tableWrapper.pagination.index}`,
            perPage: `${filters.tableWrapper.pagination.size}`,
            statuses: filters.subscriptionsFilters.statuses,
            applicationIds: filters.subscriptionsFilters.applicationIds,
            planIds: filters.subscriptionsFilters.planIds,
            apiKey: filters.subscriptionsFilters.apiKey,
            expands: ['application', 'plan'],
          })
          .pipe(
            catchError(() => {
              this.handleError('Unable to load subscriptions, please try again');
              return of({ data: [], pagination: { totalCount: 0 } });
            }),
          ),
      ),
      tap(() => this.isLoadingData.set(false)),
    ),
  );

  protected readonly subscriptionsTableDS = computed<SubscriptionsTableDS[]>(() => {
    const data = this.subscriptionsData()?.data ?? [];
    return data.map(subscription => {
      const statusEntry = this.statuses.find(s => s.id === subscription.status);
      return {
        id: subscription.id,
        application: `${subscription.application?.name} (${subscription.application?.primaryOwner?.displayName})`,
        createdAt: subscription.createdAt,
        endAt: subscription.endingAt,
        securityType: subscription.plan?.security?.type ?? 'UNKNOWN',
        isSharedApiKey: subscription.plan?.security?.type === 'API_KEY' && subscription.application?.apiKeyMode === 'SHARED',
        plan: subscription.plan?.name,
        processedAt: subscription.processedAt,
        startingAt: subscription.startingAt,
        status: statusEntry?.name,
        statusBadge: statusEntry?.badge,
      };
    });
  });

  protected readonly totalSubscriptions = computed(() => this.subscriptionsData()?.pagination?.totalCount ?? 0);
  protected readonly isLoadingData = signal(true);

  protected readonly searchApplications$: (searchTerm: string) => Observable<AutocompleteOptions> = searchTerm =>
    this.applicationService.list(undefined, searchTerm, 'name', 1, 20).pipe(
      map(page =>
        (page.data ?? []).map(app => ({
          value: app.id,
          label: `${app.name} (${app.owner?.displayName ?? ''})`,
        })),
      ),
    );

  protected readonly displayApplication$: (value: string) => Observable<string> = value =>
    this.applicationService.getById(value).pipe(
      map(application => `${application.name} (${application.owner?.displayName ?? ''})`),
      catchError(() => of(value)),
    );

  // Keep URL in sync with form changes to enable browser back/forward navigation
  private readonly syncFormToUrl = effect(() => {
    const subscription = this.filtersForm.valueChanges.pipe(distinctUntilChanged(isEqual)).subscribe(formValues => {
      this.navigateToFilters(
        {
          ...this.filters().subscriptionsFilters,
          ...formValues,
        },
        {
          ...this.filters().tableWrapper,
          pagination: { ...this.filters().tableWrapper.pagination, index: 1 },
        },
      );
    });
    return () => subscription.unsubscribe();
  });

  // Sync URL -> Form (e.g. on back/forward) without triggering navigation (emitEvent: false)
  private readonly syncUrlToForm = effect(() => {
    const filters = this.filters();
    if (filters) {
      this.filtersForm.patchValue(filters.subscriptionsFilters, { emitEvent: false });
    }
  });

  // Initial sync of planIds once plans are loaded
  private readonly hasSyncedPlanIdsWhenPlansLoaded = signal(false);
  private readonly syncPlanIdsWhenPlansLoaded = effect(() => {
    const plans = this.plans();
    if (plans.length === 0) return;
    if (this.hasSyncedPlanIdsWhenPlansLoaded()) return;
    this.hasSyncedPlanIdsWhenPlansLoaded.set(true);
    const filters = this.filters();
    this.filtersForm.get('planIds')?.setValue(filters?.subscriptionsFilters.planIds ?? null, { emitEvent: false });
  });

  protected exportAsCSV(): void {
    const filters = this.filters();
    this.subscriptionService
      .exportAsCSV(
        this.apiProductId,
        '1',
        `${this.totalSubscriptions()}`,
        filters.subscriptionsFilters.statuses,
        filters.subscriptionsFilters.applicationIds,
        filters.subscriptionsFilters.planIds,
        filters.subscriptionsFilters.apiKey,
      )
      .pipe(
        tap(blob => {
          let fileName = `subscriptions-api-product-${this.apiProductId}-${now()}.csv`;
          fileName = fileName.replaceAll(/\s/gi, '-').replaceAll(/\W/gi, '-');
          const anchor = document.createElement('a');
          anchor.download = fileName;
          anchor.href = (globalThis.webkitURL || globalThis.URL).createObjectURL(blob);
          anchor.click();
        }),
        catchError(() => {
          this.handleError('An error occurred while exporting the subscriptions.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  protected createSubscription(): void {
    this.matDialog
      .open<
        ApiPortalSubscriptionCreationDialogComponent,
        ApiPortalSubscriptionCreationDialogData,
        ApiPortalSubscriptionCreationDialogResult
      >(ApiPortalSubscriptionCreationDialogComponent, {
        role: 'alertdialog',
        id: 'createSubscriptionDialog',
        data: {
          isFederatedApi: false,
          availableSubscriptionEntrypoints: [],
          plans: this.plans().filter(plan => plan.status === 'PUBLISHED'),
        },
      })
      .afterClosed()
      .pipe(
        filter(result => !!result),
        switchMap(result => {
          const toCreate = result.subscriptionToCreate;
          if (result.apiKeyMode) {
            toCreate.apiKeyMode = result.apiKeyMode;
          }
          return this.subscriptionService.create(this.apiProductId, toCreate);
        }),
        catchError(err => {
          this.handleError(err?.error?.message ?? err?.message ?? 'Failed to create subscription');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(subscription => {
        this.snackBarService.success('Subscription successfully created');
        this.router.navigate(['.', subscription.id], { relativeTo: this.activatedRoute });
      });
  }

  protected onFiltersChanged(filters: GioTableWrapperFilters): void {
    this.navigateToFilters(this.filters().subscriptionsFilters, filters);
  }

  protected resetFilters(): void {
    this.navigateToFilters(
      {
        planIds: null,
        applicationIds: null,
        statuses: DEFAULT_SUBSCRIPTION_FILTER_STATUSES,
        apiKey: undefined,
      },
      { searchTerm: '', pagination: { index: 1, size: 10 } },
    );
  }

  private navigateToFilters(subscriptionsFilters: SubscriptionsFiltersValue, tableWrapper: GioTableWrapperFilters) {
    const params = omitBy(
      {
        page: tableWrapper.pagination.index,
        size: tableWrapper.pagination.size,
        status: subscriptionsFilters.statuses?.join(','),
        plan: subscriptionsFilters.planIds?.join(','),
        application: subscriptionsFilters.applicationIds?.join(','),
        apiKey: subscriptionsFilters.apiKey,
      },
      isNil,
    );

    if (!isEqual(this.activatedRoute.snapshot.queryParams, params)) {
      this.router.navigate(['.'], {
        relativeTo: this.activatedRoute,
        queryParams: params,
      });
    }
  }

  private parseFiltersFromQueryParams(queryParams: Params) {
    const initialPageNumber = queryParams?.['page'] ? Number(queryParams['page']) : 1;
    const initialPageSize = queryParams?.['size'] ? Number(queryParams['size']) : 10;
    const initialPlanIds: string[] | null = queryParams?.['plan'] ? queryParams['plan'].split(',') : null;
    const initialApplicationIds: string[] | null = queryParams?.['application'] ? queryParams['application'].split(',') : null;
    const initialStatuses: SubscriptionStatus[] = queryParams?.['status']
      ? queryParams['status'].split(',')
      : DEFAULT_SUBSCRIPTION_FILTER_STATUSES;
    const initialApiKey: string | undefined = queryParams?.['apiKey'] ?? undefined;

    return {
      tableWrapper: { searchTerm: '', pagination: { index: initialPageNumber, size: initialPageSize } },
      subscriptionsFilters: {
        planIds: initialPlanIds,
        applicationIds: initialApplicationIds,
        statuses: initialStatuses,
        apiKey: initialApiKey,
      },
    };
  }

  private buildFiltersForm(): FormGroup<SubscriptionsFiltersForm> {
    const initialFilters = this.parseFiltersFromQueryParams(this.activatedRoute.snapshot.queryParams);
    const { subscriptionsFilters } = initialFilters;
    return new FormGroup<SubscriptionsFiltersForm>({
      planIds: new FormControl<string[] | null>(subscriptionsFilters.planIds),
      applicationIds: new FormControl<string[] | null>(subscriptionsFilters.applicationIds),
      statuses: new FormControl<SubscriptionStatus[]>(subscriptionsFilters.statuses),
      apiKey: new FormControl<string | undefined>(subscriptionsFilters.apiKey),
    });
  }

  private handleError(message: string): void {
    this.snackBarService.error(message);
  }
}
