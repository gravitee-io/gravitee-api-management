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
import { STEPPER_GLOBAL_OPTIONS } from '@angular/cdk/stepper';
import { AfterViewInit, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { EMPTY, of, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { StateService } from '@uirouter/angularjs';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { Api } from '../../../../../entities/api';
import { ApiService } from '../../../../../services-ngx/api.service';
import { PlanService } from '../../../../../services-ngx/plan.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-portal-plan-edit',
  template: require('./api-portal-plan-edit.component.html'),
  styles: [require('./api-portal-plan-edit.component.scss')],
  providers: [
    {
      provide: STEPPER_GLOBAL_OPTIONS,
      useValue: { displayDefaultIndicatorType: false, showError: true },
    },
  ],
})
export class ApiPortalPlanEditComponent implements OnInit, AfterViewInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public mode: 'create' | 'edit' = 'create';
  public isLoadingData = true;

  public planForm = new FormGroup({});
  public initialPlanFormValue: unknown;
  public api: Api;
  public isReadOnly = false;
  public displaySubscriptionsSection = true;

  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly planService: PlanService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit() {
    this.mode = this.ajsStateParams.planId ? 'edit' : 'create';
  }

  ngAfterViewInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.api = api;
          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-plan-u']) || this.api.definition_context?.origin === 'kubernetes';
        }),
        switchMap(() =>
          this.mode === 'edit' ? this.planService.get(this.ajsStateParams.apiId, this.ajsStateParams.planId) : of(undefined),
        ),
        tap((_plan) => {
          // TODO: 🚧
          // this.initPlanForm(
          //   plan
          //     ? {
          //         general: {
          //           name: plan.name,
          //           description: plan.description,
          //           characteristics: plan.characteristics,
          //           generalConditions: plan.general_conditions,
          //           shardingTags: plan.tags,
          //           commentRequired: plan.comment_required,
          //           commentMessage: plan.comment_message,
          //           validation: plan.validation,
          //           excludedGroups: plan.excluded_groups,
          //         },
          //         secure: {
          //           securityTypes: plan.security,
          //           securityConfig: plan.securityDefinition ? JSON.parse(plan.securityDefinition) : {},
          //           selectionRule: plan.selection_rule,
          //         },
          //       }
          //     : undefined,
          // );
          // Manually trigger change detection to avoid ExpressionChangedAfterItHasBeenCheckedError in test
          // Needed to have only one detectChanges() after load each step child component
          this.changeDetectorRef.detectChanges();
        }),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An ');
          return EMPTY;
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    // TODO 🚧
    // const newPlan: NewPlan = {
    //   api: this.api.id,
    //   // General
    //   name: this.planForm.get('general').get('name').value,
    //   description: this.planForm.get('general').get('description').value,
    //   characteristics: this.planForm.get('general').get('characteristics').value,
    //   general_conditions: this.planForm.get('general').get('generalConditions').value,
    //   tags: this.planForm.get('general').get('shardingTags').value,
    //   comment_required: this.planForm.get('general').get('commentRequired').value,
    //   comment_message: this.planForm.get('general').get('commentMessage').value,
    //   validation: this.planForm.get('general').get('validation').value ? PlanValidation.AUTO : PlanValidation.MANUAL,
    //   excluded_groups: this.planForm.get('general').get('excludedGroups').value,
    //
    //   // Secure
    //   security: this.planForm.get('secure').get('securityTypes').value,
    //   securityDefinition: JSON.stringify(this.planForm.get('secure').get('securityConfig').value),
    //   selection_rule: this.planForm.get('secure').get('selectionRule').value,
    //
    //   // Restriction (only for create mode)
    //   ...(this.mode === 'edit' ? {} : { flows: this.initFlowsWithRestriction() }),
    // };
    //
    // (this.mode === 'edit'
    //   ? this.planService
    //       .get(this.ajsStateParams.apiId, this.ajsStateParams.planId)
    //       .pipe(switchMap((planToUpdate) => this.planService.update(this.api, { ...planToUpdate, ...newPlan })))
    //   : this.planService.create(this.api, newPlan)
    // )
    //   .pipe(
    //     takeUntil(this.unsubscribe$),
    //     tap(() => this.snackBarService.success('Configuration successfully saved!')),
    //     catchError((error) => {
    //       this.snackBarService.error(error.error?.message ?? 'An error occurs while saving configuration');
    //       return EMPTY;
    //     }),
    //   )
    //   .subscribe(() => this.ajsState.go('management.apis.detail.portal.plans'));
  }
}
