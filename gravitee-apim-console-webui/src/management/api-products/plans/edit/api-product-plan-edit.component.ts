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
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, EMPTY, map, of, switchMap, tap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { GioFormFocusInvalidModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';

import { ApiProductPlanV2Service } from '../../../../services-ngx/api-product-plan-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { AVAILABLE_PLANS_FOR_MENU, PlanFormType, PlanMenuItemVM } from '../../../../services-ngx/constants.service';
import { CreatePlanV4, Plan, PlanStatus } from '../../../../entities/management-api-v2';
import { ApiPlanFormModule } from '../../../api/component/plan/api-plan-form.module';
import { ApiPlanFormComponent, PlanFormValue } from '../../../api/component/plan/api-plan-form.component';
import { GioGoBackButtonModule } from '../../../../shared/components/gio-go-back-button/gio-go-back-button.module';

@Component({
  selector: 'api-product-plan-edit',
  templateUrl: './api-product-plan-edit.component.html',
  styleUrls: ['./api-product-plan-edit.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    GioFormFocusInvalidModule,
    GioSaveBarModule,
    GioGoBackButtonModule,
    ApiPlanFormModule,
  ],
})
export class ApiProductPlanEditComponent {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly planService = inject(ApiProductPlanV2Service);
  private readonly permissionService = inject(GioPermissionService);
  private readonly snackBarService = inject(SnackBarService);

  private readonly apiPlanFormRef = viewChild<ApiPlanFormComponent>('apiPlanForm');

  protected readonly planForm = new FormGroup<{ plan: FormControl<Plan | null> }>({
    plan: new FormControl<Plan | null>(null),
  });
  protected readonly initialPlanFormValue = signal<unknown>(null);
  protected readonly planMenuItem = signal<PlanMenuItemVM | undefined>(undefined);
  protected readonly currentPlanStatus = signal<PlanStatus | undefined>(undefined);

  private readonly apiProductId = toSignal(this.activatedRoute.paramMap.pipe(map(p => p.get('apiProductId') ?? '')), { initialValue: '' });
  private readonly planId = toSignal(this.activatedRoute.paramMap.pipe(map(p => p.get('planId') ?? null)), { initialValue: null });

  protected readonly mode = computed(() => (this.planId() ? 'edit' : 'create') as 'create' | 'edit');
  protected readonly isReadOnly = computed(() => !this.permissionService.hasAnyMatching(['api_product-plan-u']));
  protected readonly hasNextStep = computed(() => this.apiPlanFormRef()?.hasNextStep() === true);
  protected readonly hasPreviousStep = computed(() => this.apiPlanFormRef()?.hasPreviousStep() === true);

  private readonly planLoad = toSignal(
    combineLatest([toObservable(this.apiProductId), toObservable(this.planId)]).pipe(
      switchMap(([apiProductId, planId]) => {
        if (!planId) return of(undefined as Plan | undefined);
        return this.planService.get(apiProductId, planId).pipe(catchError(err => this.handleError(err)));
      }),
    ),
    { initialValue: null as Plan | undefined | null },
  );

  private readonly _setupFormEffect = effect(() => {
    const plan = this.planLoad();
    if (plan === null) return;

    untracked(() => {
      const planFormType = this.mode() === 'edit' ? plan?.security?.type : this.activatedRoute.snapshot.queryParams['selectedPlanMenuItem'];

      const planMenuItem = AVAILABLE_PLANS_FOR_MENU.find(vm => vm.planFormType === planFormType);
      this.planForm.reset({ plan: plan ?? null });
      this.planForm.get('plan')![this.isReadOnly() ? 'disable' : 'enable']();
      this.planMenuItem.set(planMenuItem);
      this.currentPlanStatus.set(plan?.status);
      this.initialPlanFormValue.set(this.planForm.getRawValue());
    });
  });

  protected onSubmit(): void {
    if (this.planForm.invalid) return;

    const planFormValue: PlanFormValue = { ...this.planForm.get('plan')!.value };
    const apiProductId = this.apiProductId();
    const planId = this.planId();

    const savePlan$ =
      this.mode() === 'edit'
        ? this.planService
            .get(apiProductId, planId!)
            .pipe(switchMap(existing => this.planService.update(apiProductId, existing.id, { ...existing, ...planFormValue })))
        : this.planService.create(apiProductId, createApiProductPlan(planFormValue, this.planMenuItem()!.planFormType as PlanFormType));

    savePlan$
      .pipe(
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(err => this.handleError(err, 'An error occurred while saving configuration.')),
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

  private handleError(err: unknown, defaultMessage = 'An error occurred.'): typeof EMPTY {
    const message = (err as { error?: { message?: string } })?.error?.message ?? defaultMessage;
    this.snackBarService.error(message);
    return EMPTY;
  }
}

function createApiProductPlan(planFormValue: PlanFormValue, planFormType: PlanFormType): CreatePlanV4 {
  return {
    ...planFormValue,
    definitionVersion: 'V4',
    mode: 'STANDARD',
    ...(planFormType !== 'PUSH'
      ? {
          security: {
            type: planFormType,
            configuration: planFormValue.security?.configuration,
          },
        }
      : { security: undefined }),
  };
}
