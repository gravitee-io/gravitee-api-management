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
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Observable, of, Subject } from 'rxjs';
import { StateService, UIRouterGlobals } from '@uirouter/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { orderBy } from 'lodash';
import {
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { IRootScopeService } from 'angular';

import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { PlanService } from '../../../../../services-ngx/plan.service';
import { SubscriptionService } from '../../../../../services-ngx/subscription.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ConstantsService, PlanSecurityVM } from '../../../../../services-ngx/constants.service';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { Api, PLAN_STATUS, Plan, PlanStatus, ApiV4 } from '../../../../../entities/management-api-v2';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';

@Component({
  selector: 'api-portal-plan-list',
  template: require('./api-portal-plan-list.component.html'),
  styles: [require('./api-portal-plan-list.component.scss')],
})
export class ApiPortalPlanListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: Api;
  public displayedColumns = ['name', 'security', 'status', 'deploy-on', 'actions'];
  public plansTableDS: Plan[] = [];
  public isLoadingData = true;
  public apiPlanStatus: { name: PlanStatus; number: number | null }[] = PLAN_STATUS.map((status) => ({ name: status, number: null }));
  public status: PlanStatus;
  public isReadOnly = false;
  public isV2Api: boolean;
  public planSecurityOptions: PlanSecurityVM[];
  private routeBase: string;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(AjsRootScope) private readonly ajsRootScope: IRootScopeService,
    private readonly ajsGlobals: UIRouterGlobals,
    private readonly plansV1Service: PlanService,
    private readonly plansService: ApiPlanV2Service,
    private readonly constantsService: ConstantsService,
    private readonly apiService: ApiV2Service,
    private readonly subscriptionService: SubscriptionService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.routeBase = this.ajsGlobals.current?.data?.baseRouteState ?? 'management.apis.detail.portal';
    this.status = this.ajsStateParams.status ?? 'PUBLISHED';

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.api = api;
          this.isV2Api = api && api.definitionVersion === 'V2';
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-plan-u']) || api.definitionContext?.origin === 'KUBERNETES';

          if (!this.isReadOnly && !this.displayedColumns.includes('drag-icon')) {
            this.displayedColumns.unshift('drag-icon');
          }

          this.computePlanOptions();
        }),
        tap(() => this.onInit(this.status, true)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public searchPlansByStatus(status: PlanStatus): void {
    this.status = status;
    this.onInit(this.status);
  }

  public dropRow(event: CdkDragDrop<string[]>) {
    const currentData = [...this.plansTableDS];
    const elm = currentData[event.previousIndex];
    currentData.splice(event.previousIndex, 1);
    currentData.splice(event.currentIndex, 0, elm);
    this.plansTableDS = [...currentData];

    const movedPlan = this.plansTableDS[event.currentIndex];
    movedPlan.order = event.currentIndex + 1;

    this.plansService
      .update(this.api.id, movedPlan.id, movedPlan)
      .pipe(
        takeUntil(this.unsubscribe$),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return of({});
        }),
        tap(() => this.onInit(this.status)),
      )
      .subscribe();
  }

  public navigateToPlan(planId: string): void {
    this.ajsState.go(`${this.routeBase}.plan.edit`, { planId });
  }

  public navigateToNewPlan(securityType: string): void {
    this.ajsState.go(`${this.routeBase}.plan.new`, { securityType });
  }

  public designPlan(planId: string): void {
    this.ajsState.go('management.apis.detail.design.flowsNg', { apiId: this.api.id, flows: `${planId}_0` });
  }

  public publishPlan(plan: Plan): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Publish plan`,
          content: `Are you sure you want to publish the plan ${plan.name}?`,
          confirmButton: `Publish`,
        },
        role: 'alertdialog',
        id: 'publishPlanDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.plansService.publish(this.api.id, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          if (plan.definitionVersion === 'V2') {
            this.ajsRootScope.$broadcast('apiChangeSuccess', { apiId: plan.apiId });
          }
          this.snackBarService.success(`The plan ${plan.name} has been published with success.`);
          this.onInit(this.status, true);
        }),
      )
      .subscribe();
  }

  public deprecatePlan(plan: Plan): void {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Deprecate plan`,
          content: `Would you like to deprecate the plan ${plan.name}?`,
          confirmButton: `Deprecate`,
        },
        role: 'alertdialog',
        id: 'deprecatePlanDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.plansService.deprecate(this.api.id, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          if (plan.definitionVersion === 'V2') {
            this.ajsRootScope.$broadcast('apiChangeSuccess', { apiId: plan.apiId });
          }
          this.snackBarService.success(`The plan ${plan.name} has been deprecated with success.`);
          this.onInit(this.status, true);
        }),
      )
      .subscribe();
  }

  public closePlan(plan: Plan): void {
    this.subscriptionService
      .getApiSubscriptionsByPlan(plan.apiId, plan.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((subscriptions) => {
          let content = '';
          if (plan.security.type === 'KEY_LESS') {
            content = 'A keyless plan may have consumers. <br/>' + 'By closing this plan you will remove free access to this API.';
          } else {
            if (subscriptions.page.size === 0) {
              content = 'No subscription is associated to this plan. You can delete it safely.';
            } else if (subscriptions.page.size > 0) {
              content = `There are <code>subscriptions</code> subscription(s) associated to this plan.<br/> By closing this plan, all relative active subscriptions will also be closed.`;
            }
          }
          let confirmButton = 'Yes, close this plan.';
          if (subscriptions.page.size === 0 && plan.security.type === 'API_KEY') {
            confirmButton = 'Yes, delete this plan';
          }
          return this.matDialog
            .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
              width: '500px',
              data: {
                title: `Close plan`,
                warning: `This operation is irreversible.`,
                validationMessage: `Please, type in the name of the plan <code>${plan.name}</code> to confirm.`,
                validationValue: plan.name,
                content,
                confirmButton,
              },
              role: 'alertdialog',
              id: 'closePlanDialog',
            })
            .afterClosed();
        }),
        filter((confirm) => confirm === true),
        switchMap(() => this.plansService.close(this.api.id, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          if (plan.definitionVersion === 'V2') {
            this.ajsRootScope.$broadcast('apiChangeSuccess', { apiId: plan.apiId });
          }

          this.snackBarService.success(`The plan ${plan.name} has been closed with success.`);
          this.onInit(this.status, true);
        }),
      )
      .subscribe();
  }

  private onInit(selectedStatus: PlanStatus, fullReload = false): void {
    // For full reload, we need to reset the number of plans for each status
    const getApiPlans$: Observable<Plan[]> = fullReload
      ? this.plansService.list(this.ajsStateParams.apiId, undefined, [...PLAN_STATUS], 1, 9999).pipe(
          map((plans) => {
            // Update the number of plans for each status
            const plansNumber = plans.data.reduce((acc, plan) => {
              const status = plan.status.toUpperCase();
              acc[status] = acc[status] ? acc[status] + 1 : 1;
              return acc;
            }, {} as Record<PlanStatus, number>);

            this.apiPlanStatus.forEach((plan) => {
              plan.number = plansNumber[plan.name.toUpperCase()] ?? 0;
            });

            // Filter plans by status
            return plans.data.filter((p) => p.status === selectedStatus);
          }),
        )
      : this.plansService.list(this.ajsStateParams.apiId, undefined, [selectedStatus], 1, 9999).pipe(map((response) => response.data));

    getApiPlans$
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((plans) => {
          this.ajsState.go(`${this.routeBase}.plans`, { status: this.status }, { notify: false });
          this.plansTableDS = orderBy(plans, 'order', 'asc');
          this.isLoadingData = false;
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return of({});
        }),
      )
      .subscribe();
  }

  private computePlanOptions(): void {
    this.planSecurityOptions = this.constantsService.getEnabledPlanSecurityTypes();

    if (this.api && this.api.definitionVersion !== 'V4') {
      this.planSecurityOptions = this.planSecurityOptions.filter((security) => security.id !== 'PUSH');
    } else {
      if ((this.api as ApiV4)?.listeners?.every((entrypoint) => entrypoint.type === 'SUBSCRIPTION')) {
        this.planSecurityOptions = this.planSecurityOptions.filter((planSecurityType) => planSecurityType.id === 'PUSH');
      }

      if ((this.api as ApiV4)?.listeners?.every((entrypoint) => ['HTTP', 'TCP'].includes(entrypoint.type))) {
        this.planSecurityOptions = this.planSecurityOptions.filter((planSecurityType) => planSecurityType.id !== 'PUSH');
      }
    }
  }
}
