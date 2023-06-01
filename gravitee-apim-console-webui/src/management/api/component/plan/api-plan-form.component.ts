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
import { AbstractControl, ControlValueAccessor, FormControl, FormGroup, NgControl, ValidationErrors, Validator } from '@angular/forms';
import { asyncScheduler, Subject } from 'rxjs';
import { distinctUntilChanged, map, observeOn, takeUntil, tap } from 'rxjs/operators';
import { MatStepper } from '@angular/material/stepper';
import { GIO_FORM_FOCUS_INVALID_IGNORE_SELECTOR } from '@gravitee/ui-particles-angular';
import { isEmpty, isEqual } from 'lodash';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';

import { ApiV2, ApiV4, PlanV4, CreatePlanV4, FlowV4, StepV4, PlanSecurityType } from '../../../../entities/management-api-v2';
import {
  NewPlan as NewPlanV2,
  Plan as PlanV2,
  PlanSecurityType as PlanSecurityTypeV2,
  PlanValidation as PlanValidationV2,
} from '../../../../entities/plan';
import { Flow, Step } from '../../../../entities/flow/flow';
import { isApiV1V2FromMAPIV1 } from '../../../../util';
import { PlanSecurityVM } from '../../../../services-ngx/constants.service';

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

type InternalPlanV2Value = Partial<PlanV2 | NewPlanV2>;
type InternalPlanV4Value = Partial<CreatePlanV4 | PlanV4>;
type InternalPlanValue = InternalPlanV2Value | InternalPlanV4Value;

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
  api?: ApiV2 | ApiV4;

  @Input()
  mode: 'create' | 'edit';

  @Input()
  securityType: PlanSecurityVM;

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
  private get isV2Api(): boolean {
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
    const value = this.isPlanV2(this.controlValue)
      ? planV2ToInternalFormValue(this.controlValue)
      : planV4ToInternalFormValue(this.controlValue);

    this.planForm = new FormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
      secure:
        this.planEditSecureStepComponent?.secureForm ??
        new FormGroup({
          securityConfig: new FormControl({}),
          selectionRule: new FormControl(),
        }),
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

    if (this.isDisabled) {
      this.planForm.disable();
    }

    // Display subscriptions section only for none KEY_LESS security type
    this.displaySubscriptionsSection = this.securityType.id !== 'KEY_LESS';

    // Disable unnecessary fields with KEY_LESS security type
    if (this.securityType.id === 'KEY_LESS') {
      this.planForm.get('general').get('commentRequired').disable();
      this.planForm.get('general').get('commentRequired').setValue(false);
      this.planForm.get('general').get('autoValidation').disable();
      this.planForm.get('general').get('autoValidation').setValue(false);
    } else {
      !this.isDisabled && this.planForm.get('general').get('commentRequired').enable();
      !this.isDisabled && this.planForm.get('general').get('autoValidation').enable();
    }

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
    return this.isV2Api
      ? internalFormValueToPlanV2(this.planForm.getRawValue(), this.mode, this.securityType.id)
      : internalFormValueToPlanV4(this.planForm.getRawValue(), this.mode, this.securityType.id);
  }

  private isPlanV2(plan: InternalPlanValue): plan is InternalPlanV2Value {
    return this.isV2Api;
  }
}

const planV2ToInternalFormValue = (plan: InternalPlanV2Value | undefined): InternalPlanFormValue | undefined => {
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
      autoValidation: plan.validation === PlanValidationV2.AUTO,
      excludedGroups: plan.excluded_groups,
    },
    secure: {
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
      generalConditions: plan.generalConditions,
      shardingTags: plan.tags,
      commentRequired: plan.commentRequired,
      commentMessage: plan.commentMessage,
      autoValidation: plan.validation === 'AUTO',
      excludedGroups: plan.excludedGroups,
    },
    secure: {
      securityConfig: plan.security.configuration,
      selectionRule: plan.selectionRule,
    },
  };
};

const internalFormValueToPlanV2 = (
  value: InternalPlanFormValue,
  mode: 'create' | 'edit',
  securityType: PlanSecurityType,
): InternalPlanV2Value => {
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
    validation: value.general.autoValidation ? PlanValidationV2.AUTO : PlanValidationV2.MANUAL,
    excluded_groups: value.general.excludedGroups,

    // Secure
    security: securityType as PlanSecurityTypeV2,
    securityDefinition: JSON.stringify(value.secure.securityConfig), // TODO: remove stringify when rest-api-v2 implemented
    selection_rule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};

const internalFormValueToPlanV4 = (
  value: InternalPlanFormValue,
  mode: 'create' | 'edit',
  securityType: PlanSecurityType,
): InternalPlanV4Value => {
  // Init flows with restriction step. Only used in create mode
  const initFlowsWithRestriction = (restriction: InternalPlanFormValue['restriction']): FlowV4[] => {
    const restrictionPolicies: StepV4[] = [
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
        selectors: [{ type: 'HTTP', path: '/', pathOperator: 'STARTS_WITH' }],
        enabled: true,
        request: [...restrictionPolicies],
      },
    ];
  };

  return {
    name: value.general.name,
    description: value.general.description,
    characteristics: value.general.characteristics,
    generalConditions: value.general.generalConditions,
    tags: value.general.shardingTags,
    commentRequired: value.general.commentRequired,
    commentMessage: value.general.commentMessage,
    validation: value.general.autoValidation ? 'AUTO' : 'MANUAL',
    excludedGroups: value.general.excludedGroups,

    // Secure
    security: {
      type: securityType,
      configuration: value.secure.securityConfig,
    },
    selectionRule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};
