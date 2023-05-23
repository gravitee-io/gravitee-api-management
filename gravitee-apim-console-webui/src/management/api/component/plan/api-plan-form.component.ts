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
import { asyncScheduler, Subject } from 'rxjs';
import { distinctUntilChanged, map, observeOn, startWith, takeUntil, tap } from 'rxjs/operators';
import { MatStepper } from '@angular/material/stepper';
import { GIO_FORM_FOCUS_INVALID_IGNORE_SELECTOR } from '@gravitee/ui-particles-angular';
import { isEmpty, isEqual } from 'lodash';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';

import { Api as ApiV3 } from '../../../../entities/api';
import { ApiV4 } from '../../../../entities/management-api-v2';
import {
  NewPlan as NewPlanV3,
  Plan as PlanV3,
  PlanSecurityType as PlanSecurityTypeV3,
  PlanValidation as PlanValidationV3,
} from '../../../../entities/plan';
import {
  NewPlan as NewPlanV4,
  Plan as PlanV4,
  PlanSecurityType as PlanSecurityTypeV4,
  PlanValidation as PlanValidationV4,
  Flow as FlowV4,
  FlowStep,
} from '../../../../entities/plan-v4';
import { Flow, Step } from '../../../../entities/flow/flow';
import { isApiV1V2FromMAPIV1 } from '../../../../util';

type InternalPlanFormValue = {
  general: {
    name: string;
    description: string;
    characteristics: string[];
    generalConditions: string;
    shardingTags: string[];
    commentRequired: boolean;
    commentMessage: string;
    autoValidation: boolean;
    excludedGroups: string[];
  };
  secure: {
    securityType: string;
    securityConfig: unknown;
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

type InternalPlanV3Value = Partial<PlanV3 | NewPlanV3>;
type InternalPlanV4Value = Partial<NewPlanV4 | PlanV4>;
type InternalPlanValue = InternalPlanV3Value | InternalPlanV4Value;

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
  api?: ApiV3 | ApiV4;

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

  private _onChange: (_: InternalPlanValue) => void;
  private _onTouched: () => void;

  private controlValue?: InternalPlanValue;
  private isDisabled = false;
  private get isV3Api(): boolean {
    // If no api defined, considered it's a v4 by default
    if (!this.api) {
      return false;
    }
    return isApiV1V2FromMAPIV1(this.api);
  }
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
  writeValue(obj: InternalPlanValue): void {
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
    this.changeDetectorRef.markForCheck();

    return null;
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
    const value = this.isPlanV3(this.controlValue)
      ? planV3ToInternalFormValue(this.controlValue)
      : planV4ToInternalFormValue(this.controlValue);

    this.planForm = new FormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
      secure: this.planEditSecureStepComponent.secureForm,
      ...(this.mode === 'create' ? { restriction: this.planEditRestrictionStepComponent.restrictionForm } : {}),
    });

    if (value) {
      // TODO: Delete if-check once rest-api-v2 is implemented
      if (typeof value.secure?.securityConfig === 'string') {
        value.secure.securityConfig = JSON.parse(<string>value.secure.securityConfig);
      }
      this.planForm.patchValue(value);
      this.planForm.updateValueAndValidity();
    }
    this.initialPlanFormValue = this.planForm.getRawValue();

    if (this.mode === 'edit') {
      this.planForm.get('secure').get('securityType').disable();
    }

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
          this.planForm.get('general').get('autoValidation').disable();
          this.planForm.get('general').get('autoValidation').setValue(false);
          return;
        }
        !this.isDisabled && this.planForm.get('general').get('commentRequired').enable();
        !this.isDisabled && this.planForm.get('general').get('autoValidation').enable();
      });

    // After init internal planForm. Subscribe to it and emit changes when needed
    this.planForm.valueChanges
      .pipe(
        takeUntil(this.unsubscribe$),
        map(() => this.getPlan()),
        distinctUntilChanged(isEqual),
        tap((plan) => {
          this._onChange(plan);
          this._onTouched();
        }),
        observeOn(asyncScheduler),
      )
      .subscribe(() => {
        this.ngControl?.control?.updateValueAndValidity();
      });
  }

  private getPlan() {
    return this.isV3Api
      ? internalFormValueToPlanV3(this.planForm.getRawValue(), this.mode)
      : internalFormValueToPlanV4(this.planForm.getRawValue(), this.mode);
  }

  private isPlanV3(plan: InternalPlanValue): plan is InternalPlanV3Value {
    return this.isV3Api;
  }
}

const planV3ToInternalFormValue = (plan: InternalPlanV3Value | undefined): InternalPlanFormValue | undefined => {
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
      autoValidation: plan.validation === PlanValidationV3.AUTO,
      excludedGroups: plan.excluded_groups,
    },
    secure: {
      securityType: plan.security,
      securityConfig: plan.securityDefinition,
      selectionRule: plan.selection_rule,
    },
  };
};

const planV4ToInternalFormValue = (plan: InternalPlanV4Value | undefined): InternalPlanFormValue | undefined => {
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
      autoValidation: plan.validation === PlanValidationV4.AUTO,
      excludedGroups: plan.excluded_groups,
    },
    secure: {
      securityType: plan.security.type,
      securityConfig: plan.security.configuration,
      selectionRule: plan.selection_rule,
    },
  };
};

const internalFormValueToPlanV3 = (value: InternalPlanFormValue, mode: 'create' | 'edit'): InternalPlanV3Value => {
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
    validation: value.general.autoValidation ? PlanValidationV3.AUTO : PlanValidationV3.MANUAL,
    excluded_groups: value.general.excludedGroups,

    // Secure
    security: value.secure.securityType as PlanSecurityTypeV3,
    securityDefinition: JSON.stringify(value.secure.securityConfig), // TODO: remove stringify when rest-api-v2 implemented
    selection_rule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};

const internalFormValueToPlanV4 = (value: InternalPlanFormValue, mode: 'create' | 'edit'): InternalPlanV4Value => {
  // Init flows with restriction step. Only used in create mode
  const initFlowsWithRestriction = (restriction: InternalPlanFormValue['restriction']): FlowV4[] => {
    const restrictionPolicies: FlowStep[] = [
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
        selectors: [{ type: 'http', path: '/', pathOperator: 'STARTS_WITH' }],
        enabled: true,
        request: [...restrictionPolicies],
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
    validation: value.general.autoValidation ? PlanValidationV4.AUTO : PlanValidationV4.MANUAL,
    excluded_groups: value.general.excludedGroups,

    // Secure
    security: {
      type: value.secure.securityType as PlanSecurityTypeV4,
      configuration: value.secure.securityConfig,
    },
    selection_rule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};
