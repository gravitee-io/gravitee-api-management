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
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Host,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  Optional,
  ViewChild,
} from '@angular/core';
import { AbstractControl, ControlValueAccessor, FormGroup, NgControl, ValidationErrors, Validator } from '@angular/forms';
import { Subject } from 'rxjs';
import { distinctUntilChanged, map, startWith, takeUntil } from 'rxjs/operators';
import { MatStepper } from '@angular/material/stepper';
import { GIO_FORM_FOCUS_INVALID_IGNORE_SELECTOR } from '@gravitee/ui-particles-angular';
import { isEmpty } from 'lodash';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';

import { Api } from '../../../../entities/api';
import { NewPlan, Plan, PlanSecurityType, PlanValidation } from '../../../../entities/plan';
import { Flow, Step } from '../../../../entities/flow/flow';

type InternalPlanFormValue = {
  general: {
    name: string;
    description: string;
    characteristics: string[];
    generalConditions: string;
    shardingTags: string[];
    commentRequired: boolean;
    commentMessage: string;
    validation: string;
    excludedGroups: string[];
  };
  secure: {
    securityType: string;
    securityConfig: string;
    selectionRule: string;
  };

  restriction?: {
    rateLimitEnabled?: boolean;
    rateLimitConfig?: string;
    quotaEnabled?: boolean;
    quotaConfig?: string;
    resourceFilteringEnabled?: boolean;
    resourceFilteringConfig?: string;
  };
};

@Component({
  selector: 'api-plan-form',
  template: require('./api-plan-form.component.html'),
  providers: [
    {
      provide: STEPPER_GLOBAL_OPTIONS,
      useValue: { displayDefaultIndicatorType: false, showError: true },
    },
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiPlanFormComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor, Validator {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @HostBinding(`attr.${GIO_FORM_FOCUS_INVALID_IGNORE_SELECTOR}`)
  private gioFormFocusInvalidIgnore = true;

  @Input()
  api?: Api;

  @Input()
  mode: 'create' | 'edit';

  public isInit = false;

  public planForm = new FormGroup({});
  public initialPlanFormValue: unknown;
  public displaySubscriptionsSection = true;

  @ViewChild(PlanEditGeneralStepComponent)
  private planEditGeneralStepComponent: PlanEditGeneralStepComponent;
  @ViewChild(PlanEditSecureStepComponent)
  private planEditSecureStepComponent: PlanEditSecureStepComponent;
  @ViewChild(PlanEditRestrictionStepComponent)
  private planEditRestrictionStepComponent: PlanEditRestrictionStepComponent;

  @ViewChild('stepper', { static: true })
  private matStepper: MatStepper;

  private _onChange: (_: NewPlan | Plan) => void;
  private _onTouched: () => void;

  private controlValue?: Plan | NewPlan;
  private isDisabled = false;

  constructor(private readonly changeDetectorRef: ChangeDetectorRef, @Host() @Optional() public readonly ngControl?: NgControl) {
    if (ngControl) {
      // Setting the value accessor directly (instead of using
      // the providers `NG_VALUE_ACCESSOR`) to avoid running into a circular import.
      ngControl.valueAccessor = this;
    }
  }

  ngOnInit() {
    if (!this.ngControl?.control) {
      throw new Error('ApiPlanFormComponent must be used with a form control');
    }

    // Add default validator to the form control
    this.ngControl.control.setValidators(this.validate.bind(this));
    this.ngControl.control.updateValueAndValidity();

    // When the parent form is touched, mark all the sub formGroup as touched
    const parentControl = this.ngControl.control.parent;
    parentControl?.statusChanges
      ?.pipe(
        takeUntil(this.unsubscribe$),
        map(() => parentControl?.touched),
        distinctUntilChanged(),
      )
      .subscribe(() => {
        this.planForm.markAllAsTouched();
      });
  }

  ngAfterViewInit(): void {
    this.initPlanForm();
    this.isInit = true;

    // https://github.com/angular/components/issues/19027
    this.changeDetectorRef.detectChanges();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  // From ControlValueAccessor interface
  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  // From ControlValueAccessor interface
  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  // From ControlValueAccessor interface
  writeValue(obj: Plan | NewPlan): void {
    this.controlValue = obj;

    // Update if the form is already initialized
    if (this.isInit) {
      this.initPlanForm();
    }
  }

  // From ControlValueAccessor interface
  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;

    // Update if the form is already initialized
    if (this.isInit) {
      isDisabled ? this.planForm.disable() : this.planForm.enable();
    }
  }

  // From Validator interface
  validate(_: AbstractControl): ValidationErrors | null {
    if (!this.isInit) {
      return { planFormError: 'planForm is not initialized' };
    }

    if (this.planForm.invalid) {
      return { planFormError: 'planForm is invalid' };
    }

    return null;
  }

  setMatStepper(matStepper: MatStepper) {
    this.matStepper = matStepper;
  }

  hasNextStep(): boolean | null {
    if (isEmpty(this.matStepper?.steps)) {
      return null;
    }
    return !!this.matStepper.steps.get(this.matStepper.selectedIndex + 1);
  }

  nextStep() {
    this.matStepper.next();
  }

  hasPreviousStep(): boolean | null {
    if (isEmpty(this.matStepper?.steps)) {
      return false;
    }
    return !!this.matStepper.steps.get(this.matStepper.selectedIndex - 1);
  }

  previousStep() {
    this.matStepper.previous();
  }

  private initPlanForm() {
    const value = planToInternalFormValue(this.controlValue);

    this.planForm = new FormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
      secure: this.planEditSecureStepComponent.secureForm,
      ...(this.mode === 'create' ? { restriction: this.planEditRestrictionStepComponent.restrictionForm } : {}),
    });

    if (value) {
      this.planForm.patchValue(value);
      this.planForm.get('secure').get('securityType').disable();
      this.planForm.updateValueAndValidity();
    }
    this.initialPlanFormValue = this.planForm.getRawValue();
    // this.isLoadingData = false;

    if (this.isDisabled) {
      this.planForm.disable();
    }

    this.planForm
      .get('secure')
      .get('securityType')
      .valueChanges.pipe(takeUntil(this.unsubscribe$), startWith(this.planForm.get('secure').get('securityType').value))
      .subscribe((securityType) => {
        // Display subscriptions section only for none KEY_LESS security type
        this.displaySubscriptionsSection = securityType !== 'KEY_LESS';

        // Disable unnecessary fields with KEY_LESS security type
        if (securityType === 'KEY_LESS') {
          this.planForm.get('general').get('commentRequired').disable();
          this.planForm.get('general').get('commentRequired').setValue(false);
          this.planForm.get('general').get('validation').disable();
          this.planForm.get('general').get('validation').setValue(false);
          return;
        }
        !this.isDisabled && this.planForm.get('general').get('commentRequired').enable();
        !this.isDisabled && this.planForm.get('general').get('validation').enable();
      });

    this.planForm.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe(() => {
      this._onChange(this.getPlan());
      this._onTouched();
    });
  }

  private getPlan() {
    return internalFormValueToPlan(this.planForm.getRawValue(), this.mode);
  }
}

const planToInternalFormValue = (plan: Plan | NewPlan | undefined): InternalPlanFormValue | undefined => {
  if (!plan) {
    return undefined;
  }

  return {
    general: {
      name: plan.name,
      description: plan.description,
      characteristics: plan.characteristics,
      generalConditions: plan.general_conditions,
      shardingTags: plan.tags,
      commentRequired: plan.comment_required,
      commentMessage: plan.comment_message,
      validation: plan.validation,
      excludedGroups: plan.excluded_groups,
    },
    secure: {
      securityType: plan.security,
      securityConfig: plan.securityDefinition ? JSON.parse(plan.securityDefinition) : {},
      selectionRule: plan.selection_rule,
    },
  };
};

const internalFormValueToPlan = (value: InternalPlanFormValue, mode: 'create' | 'edit'): Plan | NewPlan => {
  // Init flows with restriction step. Only used in create mode
  const initFlowsWithRestriction = (restriction: InternalPlanFormValue['restriction']): Flow[] => {
    const restrictionPolicies: Step[] = [
      ...(restriction.rateLimitEnabled
        ? [
            {
              enabled: true,
              name: 'Rate Limiting',
              configuration: restriction.rateLimitConfig,
              policy: 'rate-limit',
            },
          ]
        : []),
      ...(restriction.quotaEnabled
        ? [
            {
              enabled: true,
              name: 'Quota',
              configuration: restriction.quotaConfig,
              policy: 'quota',
            },
          ]
        : []),
      ...(restriction.resourceFilteringEnabled
        ? [
            {
              enabled: true,
              name: 'Resource Filtering',
              configuration: restriction.resourceFilteringConfig,
              policy: 'resource-filtering',
            },
          ]
        : []),
    ];

    return [
      {
        'path-operator': {
          path: '/',
          operator: 'STARTS_WITH',
        },
        enabled: true,
        pre: [...restrictionPolicies],
        post: [],
      },
    ];
  };

  return {
    name: value.general.name,
    description: value.general.description,
    characteristics: value.general.characteristics,
    general_conditions: value.general.generalConditions,
    tags: value.general.shardingTags,
    comment_required: value.general.commentRequired,
    comment_message: value.general.commentMessage,
    validation: value.general.validation ? PlanValidation.AUTO : PlanValidation.MANUAL,
    excluded_groups: value.general.excludedGroups,

    // Secure
    security: value.secure.securityType as PlanSecurityType,
    securityDefinition: JSON.stringify(value.secure.securityConfig),
    selection_rule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};
