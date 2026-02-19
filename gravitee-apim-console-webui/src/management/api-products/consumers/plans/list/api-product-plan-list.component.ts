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
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { catchError, filter, map, mergeMap, switchMap, tap } from 'rxjs/operators';
import { EMPTY, merge, Observable, of, Subject } from 'rxjs';
import { orderBy } from 'lodash';
import {
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioIconsModule,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';

import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { Plan, PLAN_STATUS, PlanStatus } from '../../../../../entities/management-api-v2';
import { ApiProductPlanService } from '../../../../../services-ngx/api-product-plan.service';
import { AVAILABLE_PLANS_FOR_MENU, PlanMenuItemVM } from '../../../../../services-ngx/constants.service';
import { PlanListComponent, PlanListTableRow } from '../../../../../shared/components/plan-list/plan-list.component';

/** Plan types supported for API Products */
const API_PRODUCT_PLAN_TYPES: string[] = ['API_KEY', 'JWT', 'MTLS'];

@Component({
  selector: 'api-product-plan-list',
  templateUrl: './api-product-plan-list.component.html',
  styleUrls: ['./api-product-plan-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    GioIconsModule,
    PlanListComponent,
  ],
})
export class ApiProductPlanListComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly plansService = inject(ApiProductPlanService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly matDialog = inject(MatDialog);
  private readonly snackBarService = inject(SnackBarService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly baseDisplayedColumns: string[] = ['name', 'type', 'status', 'actions'];
  readonly displayedColumns = computed(() =>
    this.isReadOnly() ? [...this.baseDisplayedColumns] : ['drag-icon', ...this.baseDisplayedColumns],
  );
  readonly plansTableDS = signal<PlanListTableRow[]>([]);
  readonly isLoadingData = signal(true);
  readonly apiPlanStatus = signal<{ name: PlanStatus; number: number | null }[]>(
    PLAN_STATUS.map(status => ({ name: status, number: null })),
  );
  readonly status = signal<PlanStatus>('PUBLISHED');
  readonly isReadOnly = signal(false);
  readonly planMenuItems: PlanMenuItemVM[] = AVAILABLE_PLANS_FOR_MENU.filter(item => API_PRODUCT_PLAN_TYPES.includes(item.planFormType));

  /** Emits when to load plans: status to show and whether to reload all statuses (to update tab counts). */
  private readonly statusTrigger$ = new Subject<{ status: PlanStatus; fullReload: boolean }>();

  private readonly _plansSub = (() => {
    const initialStatus = (this.activatedRoute.snapshot.queryParams?.['status'] ?? 'PUBLISHED') as PlanStatus;
    this.status.set(initialStatus);
    this.isReadOnly.set(!this.permissionService.hasAnyMatching(['environment-api_product_plan-u']));
    return merge(of({ status: initialStatus, fullReload: true }), this.statusTrigger$)
      .pipe(
        tap(({ status, fullReload }) => {
          this.status.set(status);
          if (fullReload) {
            this.isLoadingData.set(true);
          }
        }),
        mergeMap(({ status, fullReload }) => this.loadPlans$(status, fullReload)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(plans => {
        this.plansTableDS.set(orderBy(plans, 'order', 'asc'));
        this.isLoadingData.set(false);
      });
  })();

  searchPlansByStatus(status: PlanStatus): void {
    this.statusTrigger$.next({ status, fullReload: false });
  }

  dropRow(event: CdkDragDrop<PlanListTableRow[]>): void {
    const currentData = [...this.plansTableDS()];
    const elm = currentData[event.previousIndex];
    currentData.splice(event.previousIndex, 1);
    currentData.splice(event.currentIndex, 0, elm);
    this.plansTableDS.set([...currentData]);

    const movedRow = this.plansTableDS()[event.currentIndex];
    const { securityTypeLabel: _, ...planForUpdate } = movedRow;
    planForUpdate.order = event.currentIndex + 1;

    const apiProductId = this.activatedRoute.snapshot.params['apiProductId'];
    this.plansService
      .update(apiProductId, planForUpdate.id, planForUpdate)
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.statusTrigger$.next({ status: this.status(), fullReload: true })),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  publishPlan(plan: Plan): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Publish plan`,
          content: `Are you sure you want to publish the plan ${plan.name}?`,
          confirmButton: `Publish`,
        },
        role: 'alertdialog',
        id: 'apiProductPublishPlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.publish(this.activatedRoute.snapshot.params['apiProductId'], plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(publishedPlan => {
          this.snackBarService.success(`The plan ${publishedPlan.name} has been published with success.`);
          this.statusTrigger$.next({ status: this.status(), fullReload: true });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  deprecatePlan(plan: Plan): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Deprecate plan`,
          content: `A deprecated plan is no longer available on the Developer Portal and new subscriptions to the plan cannot be created. Existing subscriptions are maintained.
          <br /><br />Are you sure you want to deprecate the plan: ${plan.name}?`,
          confirmButton: `Deprecate`,
        },
        role: 'alertdialog',
        id: 'apiProductDeprecatePlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.deprecate(this.activatedRoute.snapshot.params['apiProductId'], plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(deprecatedPlan => {
          this.snackBarService.success(`The plan ${deprecatedPlan.name} has been deprecated with success.`);
          this.statusTrigger$.next({ status: this.status(), fullReload: true });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  closePlan(plan: Plan): void {
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data: {
          title: `Close plan`,
          warning: `This operation is irreversible.`,
          validationMessage: `Please, type in the name of the plan <code>${plan.name}</code> to confirm.`,
          validationValue: plan.name,
          content: `Are you sure you want to close the plan: ${plan.name}?`,
          confirmButton: 'Yes, close this plan.',
        },
        role: 'alertdialog',
        id: 'apiProductClosePlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.close(this.activatedRoute.snapshot.params['apiProductId'], plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(closedPlan => {
          this.snackBarService.success(`The plan ${closedPlan.name} has been closed with success.`);
          this.statusTrigger$.next({ status: this.status(), fullReload: true });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private getSecurityTypeLabel(type?: string): string {
    const labels: Record<string, string> = {
      MTLS: 'mTLS',
      API_KEY: 'API Key',
      JWT: 'JWT',
    };
    return labels[type ?? ''] ?? type ?? '';
  }

  private loadPlans$(selectedStatus: PlanStatus, fullReload: boolean): Observable<PlanListTableRow[]> {
    const apiProductId = this.activatedRoute.snapshot.params['apiProductId'];

    const getPlans$: Observable<Plan[]> = fullReload
      ? this.plansService.list(apiProductId, [...PLAN_STATUS]).pipe(
          map(plans => {
            const plansNumber = plans.data.reduce(
              (acc, plan) => {
                const status = plan.status.toUpperCase();
                acc[status] = acc[status] ? acc[status] + 1 : 1;
                return acc;
              },
              {} as Record<PlanStatus, number>,
            );

            this.apiPlanStatus.update(prev => prev.map(p => ({ ...p, number: plansNumber[p.name.toUpperCase()] ?? 0 })));

            return plans.data.filter(p => p.status === selectedStatus);
          }),
        )
      : this.plansService.list(apiProductId, [selectedStatus]).pipe(map(response => response.data));

    return getPlans$.pipe(
      map(plans =>
        plans.map(plan => ({
          ...plan,
          securityTypeLabel: this.getSecurityTypeLabel(plan.security?.type),
        })),
      ),
      tap(() => {
        this.router.navigate([], {
          relativeTo: this.activatedRoute,
          queryParams: { status: this.status() },
          queryParamsHandling: 'merge',
        });
      }),
      catchError(error => {
        this.snackBarService.error(error.message);
        return of([]);
      }),
    );
  }
}
