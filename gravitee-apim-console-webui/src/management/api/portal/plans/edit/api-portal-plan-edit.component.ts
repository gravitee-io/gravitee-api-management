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
import { PLAN_SECURITY_TYPES, PlanSecurityVM } from '../../../../../services-ngx/constants.service';
import { Api, Plan } from '../../../../../entities/management-api-v2';
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
  public planSecurity: PlanSecurityVM;
  public portalPlansRoute: string;

  @ViewChild('apiPlanForm')
  private apiPlanForm: ApiPlanFormComponent;

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
    const baseRoute = this.ajsGlobals.current?.data?.baseRouteState ?? 'management.apis.detail.portal';
    this.portalPlansRoute = baseRoute + '.plans';

    this.mode = this.ajsStateParams.planId ? 'edit' : 'create';

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.api = api;
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-plan-u']) || this.api.definitionContext?.origin === 'KUBERNETES';
        }),
        switchMap(() =>
          this.mode === 'edit' ? this.planService.get(this.ajsStateParams.apiId, this.ajsStateParams.planId) : of(undefined),
        ),
        tap((plan: Plan) => {
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
      )
      .subscribe(() => {
        if (this.mode === 'edit') {
          // TODO remove this when the SUBSCRIPTION security type is renamed on the backend side
          if (this.planForm.value.plan.security.type === 'SUBSCRIPTION') {
            this.planSecurity = PLAN_SECURITY_TYPES.find((vm) => vm.id === 'PUSH');
          } else {
            this.planSecurity = PLAN_SECURITY_TYPES.find((vm) => vm.id === this.planForm.value.plan.security.type);
          }
        } else {
          this.planSecurity = PLAN_SECURITY_TYPES.find((vm) => vm.id === this.ajsStateParams.securityType);
        }
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
            ...planFormValue,
            definitionVersion: this.api.definitionVersion === 'V4' ? 'V4' : 'V2',
            security: { type: this.planSecurity.id, configuration: planFormValue.security.configuration },
          });

    savePlan$
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while saving configuration');
          return EMPTY;
        }),
      )
      .subscribe(() => this.ajsState.go(this.portalPlansRoute));
  }
}
