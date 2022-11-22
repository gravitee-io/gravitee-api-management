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
import { Component, Inject, OnDestroy, OnInit } from "@angular/core";
import { catchError, filter, map, switchMap, takeUntil, tap } from "rxjs/operators";
import { combineLatest, EMPTY, Subject } from "rxjs";
import { StateService } from "@uirouter/core";
import { CdkDragDrop } from "@angular/cdk/drag-drop";
import { orderBy } from "lodash";
import {
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData
} from "@gravitee/ui-particles-angular";
import { MatDialog } from "@angular/material/dialog";
import { IRootScopeService } from "angular";

import { AjsRootScope, UIRouterState, UIRouterStateParams } from "../../../../../ajs-upgraded-providers";
import { PlanService } from "../../../../../services-ngx/plan.service";
import { Api, API_PLAN_STATUS, ApiPlan, ApiPlanStatus } from "../../../../../entities/api";
import { ApiService } from "../../../../../services-ngx/api.service";
import { SubscriptionService } from "../../../../../services-ngx/subscription.service";
import { GioPermissionService } from "../../../../../shared/components/gio-permission/gio-permission.service";
import { SnackBarService } from "../../../../../services-ngx/snack-bar.service";
import { PlanSecurityType } from "../../../../../entities/plan/plan";

@Component({
  selector: 'api-portal-plan-list',
  template: require('./api-portal-plan-list.component.html'),
  styles: [require('./api-portal-plan-list.component.scss')],
})
export class ApiPortalPlanListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: Api;
  public displayedColumns = ['drag-icon', 'name', 'security', 'status', 'deploy-on', 'actions'];
  public plansTableDS: ApiPlan[] = [];
  public isLoadingData = true;
  public apiPlanStatus = API_PLAN_STATUS;
  public status: ApiPlanStatus;
  public isReadOnly = false;
  public isV2Api: boolean;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(AjsRootScope) private readonly ajsRootScope: IRootScopeService,
    private readonly plansService: PlanService,
    private readonly apiService: ApiService,
    private readonly subscriptionService: SubscriptionService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.status = this.ajsStateParams.status ?? 'PUBLISHED';

    combineLatest([this.apiService.get(this.ajsStateParams.apiId), this.plansService.getApiPlans(this.ajsStateParams.apiId, this.status)])
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(([api, plans]) => {
          this.onInit(api, plans);
        }),
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

  public searchPlansByStatus(status: ApiPlanStatus): void {
    this.status = status;

    this.plansService
      .getApiPlans(this.ajsStateParams.apiId, status)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((plans) => this.onInit(this.api, plans)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  public dropRow({ previousIndex, currentIndex }: CdkDragDrop<ApiPlan[], any>) {
    const movedPlan = this.plansTableDS[previousIndex];
    movedPlan.order = currentIndex + 1;

    this.plansService
      .update(this.api, movedPlan)
      .pipe(
        takeUntil(this.unsubscribe$),
        map(() => this.ngOnInit()),
      )
      .subscribe();
  }

  public navigateToPlan(planId: string): void {
    this.ajsState.go('management.apis.detail.portal.plans.plan', { planId });
  }

  public navigateToNewPlan(): void {
    this.ajsState.go('management.apis.detail.portal.plans.new');
  }

  public designPlan(planId: string): void {
    this.ajsState.go('management.apis.detail.design.flowsNg', { apiId: this.api.id, flows: `${planId}_0` });
  }

  public publishPlan(plan: ApiPlan): void {
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
        switchMap(() => this.plansService.get(plan.api, plan.id)),
        switchMap((plan) =>
          this.plansService.publish(this.api, {
            ...plan,
            status: 'PUBLISHED',
          }),
        ),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          this.snackBarService.success(`The plan ${plan.name} has been published with success.`);
          this.ajsRootScope.$broadcast('planChangeSuccess', { state: 'PUBLISHED' });
          this.searchPlansByStatus('PUBLISHED');
        }),
      )
      .subscribe();
  }

  public deprecatePlan(plan: ApiPlan): void {
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
        switchMap(() => this.plansService.get(plan.api, plan.id)),
        switchMap((plan) =>
          this.plansService.deprecate(this.api, {
            ...plan,
            status: 'DEPRECATED',
          }),
        ),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          this.snackBarService.success(`The plan ${plan.name} has been deprecated with success.`);
          this.ajsRootScope.$broadcast('planChangeSuccess', { state: 'DEPRECATED' });
          this.searchPlansByStatus('DEPRECATED');
        }),
      )
      .subscribe();
  }

  public closePlan(plan: ApiPlan): void {
    this.subscriptionService
      .getApiSubscriptionsByPlan(plan.api, plan.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((subscriptions) => {
          let content = '';
          if (plan.security === PlanSecurityType.KEY_LESS) {
            content = 'A keyless plan may have consumers. <br/>' + 'By closing this plan you will remove free access to this API.';
          } else {
            if (subscriptions.page.size === 0) {
              content = 'No subscription is associated to this plan. You can delete it safely.';
            } else if (subscriptions.page.size > 0) {
              content = `There are <code>subscriptions</code> subscription(s) associated to this plan.<br/> By closing this plan, all relative active subscriptions will also be closed.`;
            }
          }
          let confirmButton = 'Yes, close this plan.';
          if (subscriptions.page.size === 0 && plan.security === PlanSecurityType.API_KEY) {
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
        switchMap(() =>
          this.plansService.close(this.api, {
            ...plan,
            status: 'CLOSED',
          }),
        ),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map((plan) => {
          this.snackBarService.success(`The plan ${plan.name} has been closed with success.`);
          this.ajsRootScope.$broadcast('planChangeSuccess', { state: 'CLOSED' });
          this.searchPlansByStatus('CLOSED');
        }),
      )
      .subscribe();
  }

  private onInit(api, plans) {
    this.ajsState.go('management.apis.detail.portal.ng-plans.list', { status: this.status }, { notify: false });
    this.api = api;
    this.isV2Api = api && api.gravitee === '2.0.0';
    this.isReadOnly = !this.permissionService.hasAnyMatching(['api-plan-u']) || api.definition_context?.origin === 'kubernetes';

    this.plansTableDS = orderBy(plans, 'order', 'asc');
    this.isLoadingData = false;
  }
}
