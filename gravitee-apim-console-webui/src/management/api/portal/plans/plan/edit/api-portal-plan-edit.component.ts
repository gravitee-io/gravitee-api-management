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
import { AfterViewInit, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';

import { UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { Api } from '../../../../../../entities/api';
import { ApiService } from '../../../../../../services-ngx/api.service';
import { PlanService } from '../../../../../../services-ngx/plan.service';
import { NewPlanEntity, PlanValidation } from '../../../../../../entities/plan';
import { Step } from '../../../../../../entities/flow/flow';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

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

  public planForm = new FormGroup({});
  public initialPlanFormValue: unknown;
  public api: Api;

  @ViewChild(PlanEditGeneralStepComponent) planEditGeneralStepComponent: PlanEditGeneralStepComponent;
  @ViewChild(PlanEditSecureStepComponent) planEditSecureStepComponent: PlanEditSecureStepComponent;
  @ViewChild(PlanEditRestrictionStepComponent) planEditRestrictionStepComponent: PlanEditRestrictionStepComponent;

  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly planService: PlanService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiService.get(this.ajsStateParams.apiId).subscribe((api) => (this.api = api));
  }

  ngAfterViewInit(): void {
    this.planForm = new FormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
      secure: this.planEditSecureStepComponent.secureForm,
      restriction: this.planEditRestrictionStepComponent.restrictionForm,
    });

    // Manually trigger change detection to avoid ExpressionChangedAfterItHasBeenCheckedError in test
    // Needed to have only one detectChanges() after load each step child component
    this.changeDetectorRef.detectChanges();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const restrictionPolicies: Step[] = [
      ...(this.planForm.get('restriction').get('rateLimitEnabled').value
        ? [
            {
              enabled: true,
              name: 'Rate Limiting',
              configuration: this.planForm.get('restriction').get('rateLimitConfig').value,
              policy: 'rate-limit',
            },
          ]
        : []),
      ...(this.planForm.get('restriction').get('quotaEnabled').value
        ? [
            {
              enabled: true,
              name: 'Quota',
              configuration: this.planForm.get('restriction').get('quotaConfig').value,
              policy: 'quota',
            },
          ]
        : []),
      ...(this.planForm.get('restriction').get('resourceFilteringEnabled').value
        ? [
            {
              enabled: true,
              name: 'Resource Filtering',
              configuration: this.planForm.get('restriction').get('resourceFilteringConfig').value,
              policy: 'resource-filtering',
            },
          ]
        : []),
    ];

    const newPlan: NewPlanEntity = {
      api: this.api.id,
      // General
      name: this.planForm.get('general').get('name').value,
      description: this.planForm.get('general').get('description').value,
      characteristics: this.planForm.get('general').get('characteristics').value,
      general_conditions: this.planForm.get('general').get('generalConditions').value,
      tags: this.planForm.get('general').get('shardingTags').value,
      comment_required: this.planForm.get('general').get('commentRequired').value,
      comment_message: this.planForm.get('general').get('commentMessage').value,
      validation: this.planForm.get('general').get('validation').value ? PlanValidation.AUTO : PlanValidation.MANUAL,
      excluded_groups: this.planForm.get('general').get('excludedGroups').value,

      // Secure
      security: this.planForm.get('secure').get('securityTypes').value,
      securityDefinition: JSON.stringify(this.planForm.get('secure').get('securityConfig').value),
      selection_rule: this.planForm.get('secure').get('selectionRule').value,

      // Restriction
      flows: [
        {
          'path-operator': {
            path: '/',
            operator: 'STARTS_WITH',
          },
          enabled: true,
          pre: [...restrictionPolicies],
          post: [],
        },
      ],
    };

    this.planService
      .create(this.api, newPlan)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((error) => {
          this.snackBarService.error(error.error?.message ?? 'An error occurs while saving configuration');
          return EMPTY;
        }),
      )
      .subscribe();
  }
}
