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
import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { EMPTY, of, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPlanFormComponent, PlanFormValue } from '../../component/plan/api-plan-form.component';
import { AVAILABLE_PLANS_FOR_MENU, PlanFormType, PlanMenuItemVM } from '../../../../services-ngx/constants.service';
import { Api, CreatePlanV2, CreatePlanV4, Plan, PlanStatus } from '../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';
import { isApiV4 } from '../../../../util';

@Component({
  selector: 'api-plan-edit',
  templateUrl: './api-plan-edit.component.html',
  styleUrls: ['./api-plan-edit.component.scss'],
})
export class ApiPlanEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public mode: 'create' | 'edit' = 'create';

  public planForm: UntypedFormGroup;
  public initialPlanFormValue: unknown;
  public api: Api;
  public isReadOnly = false;
  public planMenuItem: PlanMenuItemVM;

  @ViewChild('apiPlanForm')
  private apiPlanForm: ApiPlanFormComponent;
  public currentPlanStatus: PlanStatus;
  public hasTcpListeners;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly planService: ApiPlanV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly changeDetectorRef: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.mode = this.activatedRoute.snapshot.params.planId ? 'edit' : 'create';

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api) => {
          this.api = api;
          this.hasTcpListeners = isApiV4(api) && api.listeners.find((listener) => listener.type === 'TCP') != null;
          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-plan-u']) ||
            this.api.definitionContext?.origin === 'KUBERNETES' ||
            this.api.definitionVersion === 'V1';
        }),
        switchMap(() =>
          this.mode === 'edit'
            ? this.planService.get(this.activatedRoute.snapshot.params.apiId, this.activatedRoute.snapshot.params.planId)
            : of(undefined),
        ),
        tap((plan: Plan) => {
          this.currentPlanStatus = plan?.status;
          this.planForm = new UntypedFormGroup({
            plan: new UntypedFormControl({
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
            : this.activatedRoute.snapshot.queryParams.selectedPlanMenuItem;

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
            .get(this.activatedRoute.snapshot.params.apiId, this.activatedRoute.snapshot.params.planId)
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
          this.router.navigate(['../'], {
            relativeTo: this.activatedRoute,
            queryParams: { status: this.currentPlanStatus ?? 'PUBLISHED' },
          });
        } else {
          this.router.navigate(['../'], { relativeTo: this.activatedRoute, queryParams: { status: 'STAGING' } });
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
