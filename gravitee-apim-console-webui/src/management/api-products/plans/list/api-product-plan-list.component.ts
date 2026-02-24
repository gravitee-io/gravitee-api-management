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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router, ActivatedRoute } from '@angular/router';
import { catchError, combineLatest, EMPTY, filter, map, merge, of, scan, skip, Subject, switchMap, take, withLatestFrom } from 'rxjs';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { orderBy } from 'lodash';
import {
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { ApiProductPlanV2Service } from '../../../../services-ngx/api-product-plan-v2.service';
import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ConstantsService, PlanMenuItemVM } from '../../../../services-ngx/constants.service';
import { Plan, PLAN_STATUS, PlanStatus } from '../../../../entities/management-api-v2';
import { PlanListComponent, PlanDS } from '../../../api/component/plan/plan-list/plan-list.component';

const API_PRODUCT_PLAN_TYPES = ['API_KEY', 'JWT', 'MTLS'] as const;
const INITIAL_API_PLAN_STATUS: { name: PlanStatus; number: number | null }[] = PLAN_STATUS.map(status => ({
  name: status,
  number: null,
}));

@Component({
  selector: 'api-product-plan-list',
  templateUrl: './api-product-plan-list.component.html',
  styleUrls: ['./api-product-plan-list.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PlanListComponent],
})
export class ApiProductPlanListComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly plansService = inject(ApiProductPlanV2Service);
  private readonly apiProductV2Service = inject(ApiProductV2Service);
  private readonly constantsService = inject(ConstantsService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);

  private readonly filterOverride = signal<PlanStatus | null>(null);
  private readonly reloadTrigger$ = new Subject<void>();

  private readonly apiProductId = toSignal(this.activatedRoute.paramMap.pipe(map(p => p.get('apiProductId') ?? '')), { initialValue: '' });
  private readonly initialStatusFromRoute = toSignal(
    this.activatedRoute.queryParamMap.pipe(map(q => (q.get('status') ?? 'PUBLISHED') as PlanStatus)),
    { initialValue: 'PUBLISHED' as PlanStatus },
  );
  protected readonly selectedStatus = computed(() => this.filterOverride() ?? this.initialStatusFromRoute());

  private readonly plansData = toSignal(this.buildPlansStream(), {
    initialValue: {
      plans: [] as PlanDS[],
      apiPlanStatus: INITIAL_API_PLAN_STATUS,
      loading: true,
    },
  });

  protected readonly plansTableDS = computed(() => this.plansData()?.plans ?? []);
  protected readonly isLoadingData = computed(() => this.plansData()?.loading ?? true);
  protected readonly apiPlanStatus = computed(() => this.plansData()?.apiPlanStatus ?? INITIAL_API_PLAN_STATUS);
  protected readonly isReadOnly = computed(() => !this.permissionService.hasAnyMatching(['api_product-plan-u']));

  protected readonly planMenuItems: PlanMenuItemVM[] = this.constantsService
    .getEnabledPlanMenuItems()
    .filter(p => (API_PRODUCT_PLAN_TYPES as readonly string[]).includes(p.planFormType));

  private buildPlansStream() {
    const apiProductId$ = toObservable(this.apiProductId);
    const selectedStatus$ = toObservable(this.selectedStatus);
    const idAndStatus$ = combineLatest([apiProductId$, selectedStatus$]);
    const loadTrigger$ = merge(
      idAndStatus$.pipe(
        take(1),
        map(([id, status]) => ({ id, status, fullReload: true })),
      ),
      idAndStatus$.pipe(
        skip(1),
        map(([id, status]) => ({ id, status, fullReload: false })),
      ),
      this.reloadTrigger$.pipe(
        withLatestFrom(apiProductId$, selectedStatus$),
        map(([, id, status]) => ({ id, status, fullReload: true })),
      ),
    ).pipe(takeUntilDestroyed(this.destroyRef));

    type PlansData = {
      plans: PlanDS[];
      apiPlanStatus: { name: PlanStatus; number: number | null }[];
      loading: boolean;
    };

    return loadTrigger$.pipe(
      switchMap(({ id, status, fullReload }) => {
        if (!id) {
          return of({
            plans: [] as PlanDS[],
            apiPlanStatus: INITIAL_API_PLAN_STATUS,
            loading: false,
          });
        }
        const list$ = fullReload
          ? this.plansService.list(id, undefined, [...PLAN_STATUS], undefined, ['-flow'], 1, 9999).pipe(
              map(response => {
                const plansNumber = response.data.reduce(
                  (acc, plan) => {
                    const status = plan.status.toUpperCase();
                    acc[status] = (acc[status] ?? 0) + 1;
                    return acc;
                  },
                  {} as Record<string, number>,
                );
                const apiPlanStatus = PLAN_STATUS.map(status => ({
                  name: status,
                  number: plansNumber[status.toUpperCase()] ?? 0,
                }));
                const plans = response.data
                  .filter(plan => plan.status === status)
                  .map(plan => ({
                    ...plan,
                    securityTypeLabel: this.getSecurityTypeLabel(plan.security?.type),
                  }));
                return { plans: orderBy(plans, 'order', 'asc'), apiPlanStatus, loading: false };
              }),
            )
          : this.plansService.list(id, undefined, [status], undefined, ['-flow'], 1, 9999).pipe(
              map(response => ({
                plans: orderBy(
                  response.data.map(plan => ({
                    ...plan,
                    securityTypeLabel: this.getSecurityTypeLabel(plan.security?.type),
                  })),
                  'order',
                  'asc',
                ),
                apiPlanStatus: null as { name: PlanStatus; number: number | null }[] | null,
                loading: false,
              })),
            );
        return list$.pipe(
          catchError(({ error }) => {
            this.snackBarService.error(error?.message ?? 'An error occurred while loading plans.');
            return of({
              plans: [] as PlanDS[],
              apiPlanStatus: null as { name: PlanStatus; number: number | null }[] | null,
              loading: false,
            });
          }),
        );
      }),
      scan<PlansData & { apiPlanStatus: { name: PlanStatus; number: number | null }[] | null }, PlansData>(
        (prev, curr) => ({
          ...curr,
          apiPlanStatus: curr.apiPlanStatus ?? prev.apiPlanStatus,
        }),
        {
          plans: [] as PlanDS[],
          apiPlanStatus: INITIAL_API_PLAN_STATUS,
          loading: true,
        },
      ),
    );
  }

  protected onStatusFilterChanged(status: PlanStatus): void {
    this.filterOverride.set(status);
  }

  protected onPlanTypeSelected(planFormType: string): void {
    this.router.navigate(['./new'], {
      relativeTo: this.activatedRoute,
      queryParams: { selectedPlanMenuItem: planFormType },
    });
  }

  protected onPlanReordered(event: CdkDragDrop<string[]>): void {
    const current = this.plansTableDS();
    const currentData = [...current];
    const elm = currentData[event.previousIndex];
    currentData.splice(event.previousIndex, 1);
    currentData.splice(event.currentIndex, 0, elm);

    const movedPlan = { ...currentData[event.currentIndex] };
    movedPlan.order = event.currentIndex + 1;
    delete movedPlan.securityTypeLabel;

    const apiProductId = this.apiProductId();
    this.plansService
      .update(apiProductId, movedPlan.id, movedPlan)
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.reloadTrigger$.next());
  }

  protected onPublishPlan(plan: Plan): void {
    const apiProductId = this.apiProductId();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Publish plan',
          content: `Are you sure you want to publish the plan ${plan.name}?`,
          confirmButton: 'Publish',
        },
        role: 'alertdialog',
        id: 'publishPlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.publish(apiProductId, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(published => {
        this.snackBarService.success(`The plan ${published.name} has been published with success.`);
        this.reloadTrigger$.next();
      });
  }

  protected onDeprecatePlan(plan: Plan): void {
    const apiProductId = this.apiProductId();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Deprecate plan',
          content: `A deprecated plan is no longer available on the Developer Portal and new subscriptions to the plan cannot be created. Existing subscriptions are maintained.<br /><br />Are you sure you want to deprecate the plan: ${plan.name}?`,
          confirmButton: 'Deprecate',
        },
        role: 'alertdialog',
        id: 'deprecatePlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.deprecate(apiProductId, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(deprecated => {
        this.snackBarService.success(`The plan ${deprecated.name} has been deprecated with success.`);
        this.reloadTrigger$.next();
      });
  }

  protected onClosePlan(plan: Plan): void {
    const apiProductId = this.apiProductId();
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data: {
          title: 'Close plan',
          warning: 'This operation is irreversible.',
          validationMessage: `Please, type in the name of the plan <code>${plan.name}</code> to confirm.`,
          validationValue: plan.name,
          content: `No subscription is associated to this plan. You can delete it safely.`,
          confirmButton: 'Yes, close this plan.',
        },
        role: 'alertdialog',
        id: 'closePlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.close(apiProductId, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(closed => {
        this.snackBarService.success(`The plan ${closed.name} has been closed with success.`);
        this.reloadTrigger$.next();
      });
  }

  private getSecurityTypeLabel(type?: string): string {
    const labels: Record<string, string> = {
      MTLS: 'mTLS',
      API_KEY: 'API Key',
      JWT: 'JWT',
    };
    return labels[type] ?? type ?? '';
  }
}
