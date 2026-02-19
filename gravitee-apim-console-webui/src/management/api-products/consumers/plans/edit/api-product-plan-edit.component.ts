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
import { Component, computed, DestroyRef, effect, inject, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { EMPTY, of } from 'rxjs';
import { catchError, map, switchMap, take, tap } from 'rxjs/operators';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { GioSaveBarModule, GioFormFocusInvalidModule } from '@gravitee/ui-particles-angular';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiPlanFormComponent, PlanFormValue } from '../../../../api/component/plan/api-plan-form.component';
import { AVAILABLE_PLANS_FOR_MENU, PlanFormType, PlanMenuItemVM } from '../../../../../services-ngx/constants.service';
import { CreatePlanV4, Plan, PlanStatus , PlanSecurityType } from '../../../../../entities/management-api-v2';
import { ApiProductPlanService } from '../../../../../services-ngx/api-product-plan.service';
import { GioGoBackButtonModule } from '../../../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { ApiPlanFormModule } from '../../../../api/component/plan/api-plan-form.module';

/** Plan types supported for API Products */
const API_PRODUCT_PLAN_TYPES: string[] = ['API_KEY', 'JWT', 'MTLS'];

type PlanFormGroup = FormGroup<{ plan: FormControl<Plan | null> }>;

type PlanEditState = {
  planForm: PlanFormGroup;
  initialPlanFormValue: { plan: Plan | null };
  planMenuItem: PlanMenuItemVM;
  currentPlanStatus: PlanStatus | undefined;
  mode: 'create' | 'edit';
  isReadOnly: boolean;
};

@Component({
  selector: 'api-product-plan-edit',
  templateUrl: './api-product-plan-edit.component.html',
  styleUrls: ['./api-product-plan-edit.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    GioGoBackButtonModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    ApiPlanFormModule,
  ],
})
export class ApiProductPlanEditComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly planService = inject(ApiProductPlanService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('apiProductPlanForm')
  apiProductPlanForm: ApiPlanFormComponent | undefined;

  readonly hasNextStep = signal(false);
  readonly hasPreviousStep = signal(false);

  private readonly loadPlan$ = this.activatedRoute.params.pipe(
    take(1),
    switchMap(() => {
      const params = this.activatedRoute.snapshot.params;
      const apiProductId = params['apiProductId'];
      const planId = params['planId'];
      const mode: 'create' | 'edit' = planId ? 'edit' : 'create';
      const isReadOnly = !this.permissionService.hasAnyMatching(['environment-api_product_plan-u']);
      return (mode === 'edit' ? this.planService.get(apiProductId, planId) : of(undefined)).pipe(
        catchError(error => {
          this.snackBarService.error(error.error?.message ?? 'An error occurred.');
          return EMPTY;
        }),
        map((plan: Plan | undefined) => {
          const planForm = new FormGroup({
            plan: new FormControl<Plan | null>({ value: plan ?? null, disabled: isReadOnly }),
          });
          const planFormType: PlanFormType =
            mode === 'edit'
              ? (plan?.security?.type as PlanFormType)
              : this.activatedRoute.snapshot.queryParams['selectedPlanMenuItem'];
          const planMenuItem = AVAILABLE_PLANS_FOR_MENU.filter(vm =>
            API_PRODUCT_PLAN_TYPES.includes(vm.planFormType),
          ).find(vm => vm.planFormType === planFormType) as PlanMenuItemVM;
          return {
            planForm,
            initialPlanFormValue: { plan: plan ?? null },
            planMenuItem,
            currentPlanStatus: plan?.status,
            mode,
            isReadOnly,
          } as PlanEditState;
        }),
      );
    }),
  );

  readonly planEditState = toSignal(this.loadPlan$, { initialValue: null as PlanEditState | null });

  private readonly _stepperSyncEffect = effect(() => {
    if (this.planEditState() != null) {
      queueMicrotask(() => this.syncStepperSignals());
    }
  });

  readonly mode = computed(() => this.planEditState()?.mode ?? 'create');
  readonly planForm = computed(() => this.planEditState()?.planForm ?? null);
  readonly initialPlanFormValue = computed(() => this.planEditState()?.initialPlanFormValue);
  readonly isReadOnly = computed(() => this.planEditState()?.isReadOnly ?? false);
  readonly planMenuItem = computed(() => this.planEditState()?.planMenuItem);
  readonly currentPlanStatus = computed(() => this.planEditState()?.currentPlanStatus);

  onPreviousStep(): void {
    this.apiProductPlanForm?.previousStep();
    this.syncStepperSignals();
  }

  onNextStep(): void {
    this.apiProductPlanForm?.waitForNextStep();
    this.syncStepperSignals();
  }

  private syncStepperSignals(): void {
    const form = this.apiProductPlanForm;
    if (form) {
      this.hasNextStep.set(form.hasNextStep());
      this.hasPreviousStep.set(form.hasPreviousStep());
    }
  }

  onSubmit(): void {
    const form = this.planForm();
    if (!form?.valid) {
      return;
    }

    const apiProductId = this.activatedRoute.snapshot.params['apiProductId'];
    const planFormValue: PlanFormValue = {
      ...form.get('plan').value,
    } as PlanFormValue;

    const planMenuItem = this.planMenuItem();
    if (!planMenuItem) {
      return;
    }

    const savePlan$ =
      this.mode() === 'edit'
        ? this.planService
            .get(apiProductId, this.activatedRoute.snapshot.params['planId'])
            .pipe(
              switchMap(planToUpdate =>
                this.planService.update(apiProductId, planToUpdate.id, { ...planToUpdate, ...planFormValue }),
              ),
            )
        : this.planService.create(apiProductId, createApiProductPlan(planFormValue, planMenuItem.planFormType));

    savePlan$
      .pipe(
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(error => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while saving configuration');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        if (this.mode() === 'edit') {
          this.router.navigate(['../'], {
            relativeTo: this.activatedRoute,
            queryParams: { status: this.currentPlanStatus() ?? 'PUBLISHED' },
          });
        } else {
          this.router.navigate(['../'], { relativeTo: this.activatedRoute, queryParams: { status: 'STAGING' } });
        }
      });
  }
}

function createApiProductPlan(planFormValue: PlanFormValue, planFormType: PlanFormType): CreatePlanV4 {
  return {
    ...planFormValue,
    definitionVersion: 'V4',
    mode: 'STANDARD',
    security: {
      type: planFormType as PlanSecurityType,
      configuration: planFormValue.security?.configuration,
    },
  };
}
