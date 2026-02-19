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
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, forkJoin, Observable, of, Subject } from 'rxjs';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { orderBy } from 'lodash';
import {
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
  GioConfirmDialogComponent,
  GioConfirmDialogData,
} from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';

import { SubscriptionService } from '../../../../services-ngx/subscription.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ConstantsService, PlanMenuItemVM } from '../../../../services-ngx/constants.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Api, Plan, PLAN_STATUS, PlanStatus } from '../../../../entities/management-api-v2';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { PlanListTableRow } from '../../../../shared/components/plan-list/plan-list.component';

@Component({
  selector: 'api-plan-list',
  templateUrl: './api-plan-list.component.html',
  styleUrls: ['./api-plan-list.component.scss'],
  standalone: false,
})
export class ApiPlanListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public api: Api;
  public displayedColumns = ['name', 'type', 'status', 'deploy-on', 'actions'];
  public plansTableDS: PlanListTableRow[] = [];
  public isLoadingData = true;
  public apiPlanStatus: { name: PlanStatus; number: number | null }[] = PLAN_STATUS.map(status => ({ name: status, number: null }));
  public status: PlanStatus;
  public isReadOnly = false;
  public isV2Api: boolean;
  public planMenuItems: PlanMenuItemVM[];

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly plansService: ApiPlanV2Service,
    private readonly constantsService: ConstantsService,
    private readonly apiService: ApiV2Service,
    private readonly subscriptionService: SubscriptionService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.status = this.activatedRoute.snapshot.queryParams?.status ?? 'PUBLISHED';

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap(api => {
          this.api = api;
          this.isV2Api = api && api.definitionVersion === 'V2';
          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-plan-u']) ||
            api.definitionContext?.origin === 'KUBERNETES' ||
            api.definitionVersion === 'V1';

          if (!this.isReadOnly && !this.displayedColumns.includes('drag-icon')) {
            this.displayedColumns.unshift('drag-icon');
          }

          this.computePlanOptions();
        }),
        tap(() => this.initPlansTableDS(this.status, true)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public searchPlansByStatus(status: PlanStatus): void {
    this.status = status;
    this.initPlansTableDS(this.status);
  }

  public dropRow(event: CdkDragDrop<PlanListTableRow[]>) {
    const currentData = [...this.plansTableDS];
    const elm = currentData[event.previousIndex];
    currentData.splice(event.previousIndex, 1);
    currentData.splice(event.currentIndex, 0, elm);
    this.plansTableDS = [...currentData];

    const movedRow = this.plansTableDS[event.currentIndex];
    const { securityTypeLabel: _, ...planForUpdate } = movedRow;
    planForUpdate.order = event.currentIndex + 1;

    this.plansService
      .update(this.api.id, planForUpdate.id, planForUpdate)
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return of({});
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public designPlan(planId: string): void {
    if (this.api.definitionVersion === 'V2') {
      this.router.navigate(['../v2/policy-studio'], {
        relativeTo: this.activatedRoute,
        queryParams: { flows: `${planId}_0` },
      });
    }
    if (this.api.definitionVersion === 'V4') {
      this.router.navigate(['../v4/policy-studio'], {
        relativeTo: this.activatedRoute,
      });
    }
  }

  public publishPlan(plan: Plan): void {
    const publishPlan$ =
      this.api.definitionVersion === 'V4' && this.api.type === 'NATIVE' && this.api.listeners.some(l => l.type === 'KAFKA')
        ? this.publishNativeKafkaPlan$(plan)
        : this.httpPlanDialog$(plan);

    publishPlan$
      .pipe(
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(plan => {
          this.snackBarService.success(`The plan ${plan.name} has been published with success.`);
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public deprecatePlan(plan: Plan): void {
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
        id: 'deprecatePlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.deprecate(this.api.id, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(plan => {
          this.snackBarService.success(`The plan ${plan.name} has been deprecated with success.`);
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public closePlan(plan: Plan): void {
    this.subscriptionService
      .getApiSubscriptionsByPlan(plan.apiId, plan.id)
      .pipe(
        switchMap(subscriptions => {
          let content = '';
          if (plan.security?.type === 'KEY_LESS') {
            content = 'A keyless plan may have consumers. <br/>' + 'By closing this plan you will remove free access to this API.';
          } else {
            if (subscriptions.page.size === 0) {
              content = 'No subscription is associated to this plan. You can delete it safely.';
            } else if (subscriptions.page.size > 0) {
              content = `There are <code>subscriptions</code> subscription(s) associated to this plan.<br/> By closing this plan, all relative active subscriptions will also be closed.`;
            }
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
                confirmButton: 'Yes, close this plan.',
              },
              role: 'alertdialog',
              id: 'closePlanDialog',
            })
            .afterClosed();
        }),
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.close(this.api.id, plan.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(plan => {
          this.snackBarService.success(`The plan ${plan.name} has been closed with success.`);
          this.ngOnInit();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private httpPlanDialog$(plan: Plan): Observable<Plan> {
    return this.matDialog
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
        filter(confirm => confirm === true),
        switchMap(() => this.plansService.publish(this.api.id, plan.id)),
      );
  }

  private publishNativeKafkaPlan$(plan: Plan): Observable<Plan> {
    return this.plansService.list(this.api.id, undefined, ['PUBLISHED'], undefined, ['-flow'], 1, 9999).pipe(
      switchMap(plansResponse => {
        const publishedKeylessPlan = plansResponse.data.filter(plan => plan.security.type === 'KEY_LESS');
        const publishedMtlsPlan = plansResponse.data.filter(plan => plan.security.type === 'MTLS');
        const publishedAuthPlans = plansResponse.data.filter(plan => plan.security.type !== 'KEY_LESS' && plan.security.type !== 'MTLS');

        if (plan.security?.type === 'KEY_LESS' && (publishedMtlsPlan.length || publishedAuthPlans.length)) {
          return this.nativeKafkaDialog$(plan, [...publishedMtlsPlan, ...publishedAuthPlans]);
        } else if (plan.security?.type === 'MTLS' && (publishedKeylessPlan.length || publishedAuthPlans.length)) {
          return this.nativeKafkaDialog$(plan, [...publishedKeylessPlan, ...publishedAuthPlans]);
        } else if (
          plan.security?.type !== 'KEY_LESS' &&
          plan.security?.type !== 'MTLS' &&
          (publishedKeylessPlan.length || publishedMtlsPlan.length)
        ) {
          return this.nativeKafkaDialog$(plan, [...publishedKeylessPlan, ...publishedMtlsPlan]);
        } else {
          return this.httpPlanDialog$(plan);
        }
      }),
      takeUntil(this.unsubscribe$),
    );
  }

  private nativeKafkaDialog$(plan: Plan, plansToClose: Plan[]): Observable<Plan> {
    const hasMtls = plansToClose.some(p => p.security.type === 'MTLS');
    const hasAuth = plansToClose.some(p => p.security.type !== 'KEY_LESS' && p.security.type !== 'MTLS');

    let planTypeToPublish: string;
    let ifSubscriptionContent = '';

    const plansList = plansToClose.map(p => `- <strong>${p.name}</strong> (${this.getSecurityTypeLabel(p.security.type)})`).join('\n');

    if (plan.security.type === 'KEY_LESS') {
      planTypeToPublish = 'Keyless';
      if (hasMtls || hasAuth) {
        ifSubscriptionContent = '<b>If there are subscriptions associated to these plans, they will be closed!</b>';
      }
    } else if (plan.security.type === 'MTLS') {
      planTypeToPublish = 'mTLS';
      if (hasAuth) {
        ifSubscriptionContent = '<b>If there are subscriptions associated to these plans, they will be closed!</b>';
      }
    } else {
      planTypeToPublish = 'authentication';
      if (hasMtls) {
        ifSubscriptionContent = '<b>If there are subscriptions associated to these plans, they will be closed!</b>';
      }
    }

    const content = `Kafka APIs cannot have Keyless, mTLS, and authentication (OAuth2, JWT, API Key) plans published together.
Are you sure you want to publish the <b>${planTypeToPublish}</b> plan <b>${plan.name}</b>?

The following ${plansToClose.length > 1 ? 'plans' : 'plan'} will be closed automatically:
${plansList}

${ifSubscriptionContent}`;

    return this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data: {
          title: `Publish plan and close current one${plansToClose.length > 1 ? 's' : ''}`,
          warning: `This operation is irreversible.`,
          validationMessage: `Please, type in the name of the plan <code>${plan.name}</code> to confirm.`,
          validationValue: plan.name,
          content,
          confirmButton: `Publish & Close`,
        },
        role: 'alertdialog',
        id: 'publishNativeKafkaPlanDialog',
      })
      .afterClosed()
      .pipe(
        filter(confirm => confirm === true),
        switchMap(() => forkJoin(plansToClose.map(p => this.plansService.close(this.api.id, p.id)))),
        switchMap(() => this.plansService.publish(this.api.id, plan.id)),
      );
  }

  private getSecurityTypeLabel(type?: string): string {
    const labels: Record<string, string> = {
      KEY_LESS: 'Keyless',
      MTLS: 'mTLS',
      API_KEY: 'API Key',
      OAUTH2: 'OAuth2',
      JWT: 'JWT',
    };
    return labels[type] ?? type ?? '';
  }

  private initPlansTableDS(selectedStatus: PlanStatus, fullReload = false): void {
    // For full reload, we need to reset the number of plans for each status
    const getApiPlans$: Observable<Plan[]> = fullReload
      ? this.plansService.list(this.activatedRoute.snapshot.params.apiId, undefined, [...PLAN_STATUS], undefined, ['-flow'], 1, 9999).pipe(
          map(plans => {
            // Update the number of plans for each status
            const plansNumber = plans.data.reduce(
              (acc, plan) => {
                const status = plan.status.toUpperCase();
                acc[status] = acc[status] ? acc[status] + 1 : 1;
                return acc;
              },
              {} as Record<PlanStatus, number>,
            );

            this.apiPlanStatus.forEach(plan => {
              plan.number = plansNumber[plan.name.toUpperCase()] ?? 0;
            });

            // Filter plans by status
            return plans.data.filter(p => p.status === selectedStatus);
          }),
        )
      : this.plansService
          .list(this.activatedRoute.snapshot.params.apiId, undefined, [selectedStatus], undefined, ['-flow'], 1, 9999)
          .pipe(map(response => response.data));

    getApiPlans$
      .pipe(
        map(plans =>
          plans.map(plan => ({
            ...plan,
            securityTypeLabel: this.getSecurityTypeLabel(plan.security?.type),
          })),
        ),
        tap(plans => {
          this.router.navigate(['../plans'], {
            relativeTo: this.activatedRoute,
            queryParams: { status: this.status },
          });
          this.plansTableDS = orderBy(plans, 'order', 'asc');
          this.isLoadingData = false;
        }),
        catchError(error => {
          this.snackBarService.error(error.message);
          return of({});
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private computePlanOptions(): void {
    this.planMenuItems = this.constantsService.getPlanMenuItems(
      this.api.definitionVersion,
      this.api.definitionVersion === 'V4' ? this.api.listeners.map(l => l.type) : null,
    );
  }
}
