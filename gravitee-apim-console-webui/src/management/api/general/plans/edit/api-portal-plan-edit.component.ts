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
import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { EMPTY, of, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { StateService } from '@uirouter/angularjs';
import { UIRouterGlobals } from '@uirouter/core';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPlanFormComponent, PlanFormValue } from '../../../component/plan/api-plan-form.component';
import { AVAILABLE_PLANS_FOR_MENU, PlanFormType, PlanMenuItemVM } from '../../../../../services-ngx/constants.service';
import { Api, CreatePlanV2, CreatePlanV4, Plan, PlanStatus } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiPlanV2Service } from '../../../../../services-ngx/api-plan-v2.service';

@Component({
  selector: 'api-portal-plan-edit',
  template: require('./api-portal-plan-edit.component.html'),
  styles: [require('./api-portal-plan-edit.component.scss')],
})
export class ApiPortalPlanEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public mode: 'create' | 'edit' = 'create';

  public planForm: FormGroup;
  public initialPlanFormValue: unknown;
  public api: Api;
  public isReadOnly = false;
  public planMenuItem: PlanMenuItemVM;
  public portalPlansRoute: string;

  @ViewChild('apiPlanForm')
  private apiPlanForm: ApiPlanFormComponent;
  private currentPlanStatus: PlanStatus;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly ajsGlobals: UIRouterGlobals,
    private readonly apiService: ApiV2Service,
    private readonly planService: ApiPlanV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    const baseRoute = this.ajsGlobals.current?.data?.baseRouteState ?? 'management.apis.ng';
    this.portalPlansRoute = baseRoute + '.plans';

    this.mode = this.ajsStateParams.planId ? 'edit' : 'create';

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api) => {
          this.api = api;
          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-plan-u']) ||
            this.api.definitionContext?.origin === 'KUBERNETES' ||
            this.api.definitionVersion === 'V1';
        }),
        switchMap(() =>
          this.mode === 'edit' ? this.planService.get(this.ajsStateParams.apiId, this.ajsStateParams.planId) : of(undefined),
        ),
        tap((plan: Plan) => {
          this.currentPlanStatus = plan?.status;
          this.planForm = new FormGroup({
            plan: new FormControl({
              value: plan,
              disabled: this.isReadOnly,
            }),
          });
        }),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurred.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe((plan) => {
        const planFormType =
          this.mode === 'edit'
            ? plan.definitionVersion === 'V4' && plan.mode === 'PUSH'
              ? 'PUSH'
              : plan.security.type
            : this.ajsStateParams.selectedPlanMenuItem;

        this.planMenuItem = AVAILABLE_PLANS_FOR_MENU.find((vm) => vm.planFormType === planFormType);

        this.changeDetectorRef.detectChanges();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (this.planForm.invalid) {
      return;
    }

    const planFormValue: PlanFormValue = {
      ...this.planForm.get('plan').value,
    };

    const savePlan$ =
      this.mode === 'edit'
        ? this.planService
            .get(this.ajsStateParams.apiId, this.ajsStateParams.planId)
            .pipe(switchMap((planToUpdate) => this.planService.update(this.api.id, planToUpdate.id, { ...planToUpdate, ...planFormValue })))
        : this.planService.create(this.api.id, {
            ...(this.api.definitionVersion === 'V4'
              ? createV4Plan(planFormValue, this.planMenuItem.planFormType)
              : createV2Plan(planFormValue, this.planMenuItem.planFormType)),
          });

    savePlan$
      .pipe(
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while saving configuration');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        if (this.mode === 'edit') {
          this.ajsState.go(this.portalPlansRoute, { status: this.currentPlanStatus ?? 'PUBLISHED' });
        } else {
          this.ajsState.go(this.portalPlansRoute, { status: 'STAGING' });
        }
      });
  }
}

function createV2Plan(planFormValue: PlanFormValue, planFormType: PlanFormType): CreatePlanV2 {
  if (planFormType === 'PUSH') {
    throw new Error('Push plans are not supported in V2');
  }
  return {
    ...planFormValue,
    definitionVersion: 'V2',
    security: {
      type: planFormType,
      configuration: planFormValue.security.configuration,
    },
  };
}

function createV4Plan(planFormValue: PlanFormValue, planFormType: PlanFormType): CreatePlanV4 {
  return {
    ...planFormValue,
    definitionVersion: 'V4',
    mode: planFormType === 'PUSH' ? 'PUSH' : 'STANDARD',
    ...(planFormType === 'PUSH'
      ? { security: undefined }
      : {
          security: {
            type: planFormType,
            configuration: planFormValue.security.configuration,
          },
        }),
  };
}
