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
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormControl,
  UntypedFormGroup,
  NgControl,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import { asyncScheduler, interval, Subject } from 'rxjs';
import { distinctUntilChanged, filter, map, observeOn, take, takeUntil, tap } from 'rxjs/operators';
import { MatStepper } from '@angular/material/stepper';
import { GIO_FORM_FOCUS_INVALID_IGNORE_SELECTOR } from '@gravitee/ui-particles-angular';
import { isEmpty, isEqual } from 'lodash';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';
import { PlanEditSecureStepComponent } from './2-secure-step/plan-edit-secure-step.component';
import { PlanEditRestrictionStepComponent } from './3-restriction-step/plan-edit-restriction-step.component';

import {
  ApiV2,
  ApiV4,
  FlowV2,
  FlowV4,
  StepV2,
  StepV4,
  CreatePlan,
  Plan,
  UpdatePlan,
  PlanMode,
  ApiType,
  PlanStatus,
  ApiFederated,
} from '../../../../entities/management-api-v2';
import { isApiV2FromMAPIV2 } from '../../../../util';
import { PlanFormType, PlanMenuItemVM } from '../../../../services-ngx/constants.service';

export type InternalPlanFormValue = {
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

export type PlanFormValue = Pick<
  CreatePlan | UpdatePlan | Plan,
  | 'name'
  | 'description'
  | 'characteristics'
  | 'generalConditions'
  | 'commentRequired'
  | 'commentMessage'
  | 'validation'
  | 'excludedGroups'
  | 'security'
> & { mode?: PlanMode; selectionRule?: string; tags?: string[]; flows?: Array<FlowV2 | FlowV4> };

@Component({
  selector: 'api-plan-form',
  templateUrl: './api-plan-form.component.html',
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
  api?: ApiV2 | ApiV4 | ApiFederated;

  @Input()
  isFederated = false;

  @Input()
  apiType?: ApiType;

  @Input()
  mode: 'create' | 'edit';

  @Input()
  planMenuItem: PlanMenuItemVM;

  @Input()
  planStatus?: PlanStatus;

  @Input({ required: true }) isTcpApi!: boolean;

  public isInit = false;

  public planForm = new UntypedFormGroup({});
  public initialPlanFormValue: unknown;
  public displaySubscriptionsSection = true;
  public displayRestrictionStep: boolean;
  public displaySecurityStep: boolean;

  @ViewChild(PlanEditGeneralStepComponent)
  private planEditGeneralStepComponent: PlanEditGeneralStepComponent;
  @ViewChild(PlanEditSecureStepComponent)
  private planEditSecureStepComponent: PlanEditSecureStepComponent;
  @ViewChild(PlanEditRestrictionStepComponent)
  private planEditRestrictionStepComponent: PlanEditRestrictionStepComponent;

  @ViewChild('stepper', { static: true })
  private matStepper: MatStepper;

  private _onChange: (_: PlanFormValue) => void;
  private _onTouched: () => void;

  private controlValue?: PlanFormValue;
  private isDisabled = false;

  private get isV2Api(): boolean {
    // If no api defined, considered it's a v4 by default
    if (!this.api) {
      return false;
    }
    return isApiV2FromMAPIV2(this.api);
  }
  constructor(
    private readonly changeDetectorRef: ChangeDetectorRef,
    @Host() @Optional() public readonly ngControl?: NgControl,
  ) {
    if (ngControl) {
      // Setting the value accessor directly (instead of using
      // the providers `NG_VALUE_ACCESSOR`) to avoid running into a circular import.
      ngControl.valueAccessor = this;
    }
  }

  ngOnInit() {
    if (!this.api && !this.apiType) {
      throw new Error('ApiPlanFormComponent must take either an API or apiType');
    }

    if (!this.ngControl?.control) {
      throw new Error('ApiPlanFormComponent must be used with a form control');
    }

    // Add default validator to the form control
    this.ngControl.control.setValidators(this.validate.bind(this));
    this.ngControl.control.updateValueAndValidity();

    this.displayRestrictionStep = this.mode === 'create' && !this.isTcpApi;
    this.displaySecurityStep =
      !this.isFederated &&
      !['KEY_LESS', 'PUSH'].includes(this.planMenuItem.planFormType) &&
      !('MTLS' === this.planMenuItem.planFormType && this.isTcpApi);

    // When the parent form is touched, mark all the sub formGroup as touched
    const parentControl = this.ngControl.control.parent;
    parentControl?.statusChanges
      ?.pipe(
        map(() => parentControl?.touched),
        distinctUntilChanged(),
        takeUntil(this.unsubscribe$),
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
  writeValue(obj: PlanFormValue): void {
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

  /**
   * Wait for the next step to be selected
   */
  waitForNextStep() {
    const nextStep = this.matStepper.selectedIndex + 1;
    return interval(100)
      .pipe(
        map(() => this.nextStep()),
        filter(() => this.matStepper.selectedIndex === nextStep),
        take(1),
      )
      .toPromise();
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
    this.planForm = new UntypedFormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
      secure:
        this.planEditSecureStepComponent?.secureForm ??
        new UntypedFormGroup({
          securityConfig: new UntypedFormControl({}),
          selectionRule: new UntypedFormControl(),
        }),
      ...(this.displayRestrictionStep ? { restriction: this.planEditRestrictionStepComponent.restrictionForm } : {}),
    });

    const value = planToInternalFormValue(this.controlValue, this.mode, this.isV2Api);

    if (value) {
      this.planForm.patchValue(value);
      this.planForm.updateValueAndValidity();
    }

    this.initialPlanFormValue = this.planForm.getRawValue();

    if (this.isDisabled) {
      this.planForm.disable();
    }

    // Display subscriptions section only for none KEY_LESS security type
    this.displaySubscriptionsSection = this.planMenuItem.planFormType !== 'KEY_LESS';

    // Disable unnecessary fields with KEY_LESS security type
    if (this.planMenuItem.planFormType === 'KEY_LESS') {
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
        map(() => this.getPlanFormValue()),
        distinctUntilChanged(isEqual),
        tap((plan) => {
          this._onChange(plan);
          this._onTouched();
        }),
        observeOn(asyncScheduler),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.ngControl?.control?.updateValueAndValidity();
      });
  }

  private getPlanFormValue(): PlanFormValue {
    if (this.isV2Api) {
      return internalFormValueToPlanV2(this.planForm.getRawValue(), this.mode, this.planMenuItem.planFormType);
    }
    const apiType = this.api ? (this.api as ApiV4).type : this.apiType;
    return internalFormValueToPlanV4(this.planForm.getRawValue(), this.mode, this.planMenuItem.planFormType, apiType);
  }
}

const planToInternalFormValue = (
  plan: PlanFormValue | undefined,
  mode: 'create' | 'edit',
  isV2Api: boolean,
): InternalPlanFormValue | undefined => {
  if (!plan) {
    return undefined;
  }

  const restriction: InternalPlanFormValue['restriction'] = {};
  if (mode === 'create' && plan.flows?.length > 0) {
    if (isV2Api) {
      const flow = plan.flows.map((flow) => flow as FlowV2)[0];
      if (flow.enabled) {
        flow.pre.forEach((step) => {
          if (step.policy === 'rate-limit' && step.enabled) {
            restriction.rateLimitEnabled = true;
            restriction.rateLimitConfig = step.configuration;
          }
          if (step.policy === 'quota' && step.enabled) {
            restriction.quotaEnabled = true;
            restriction.quotaConfig = step.configuration;
          }
          if (step.policy === 'resource-filtering' && step.enabled) {
            restriction.resourceFilteringEnabled = true;
            restriction.resourceFilteringConfig = step.configuration;
          }
        });
      }
    } else {
      const flow = plan.flows.map((flow) => flow as FlowV4)[0];
      if (flow.enabled) {
        flow.request.forEach((step) => {
          if (step.policy === 'rate-limit' && step.enabled) {
            restriction.rateLimitEnabled = true;
            restriction.rateLimitConfig = step.configuration;
          }
          if (step.policy === 'quota' && step.enabled) {
            restriction.quotaEnabled = true;
            restriction.quotaConfig = step.configuration;
          }
          if (step.policy === 'resource-filtering' && step.enabled) {
            restriction.resourceFilteringEnabled = true;
            restriction.resourceFilteringConfig = step.configuration;
          }
        });
      }
    }
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
      securityConfig: plan.security?.configuration,
      selectionRule: plan.selectionRule,
    },
    restriction,
  };
};

const internalFormValueToPlanV2 = (value: InternalPlanFormValue, mode: 'create' | 'edit', planFormType: PlanFormType): PlanFormValue => {
  // Init flows with restriction step. Only used in create mode
  const initFlowsWithRestriction = (restriction: InternalPlanFormValue['restriction']): FlowV2[] => {
    const restrictionPolicies: StepV2[] = [
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
        pathOperator: {
          path: '/',
          operator: 'STARTS_WITH',
        },
        enabled: true,
        pre: [...restrictionPolicies],
        post: [],
      },
    ];
  };

  if (planFormType === 'PUSH') {
    throw new Error('Push plan is not supported in V2 API');
  }

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
      type: planFormType,
      configuration: value.secure.securityConfig,
    },
    selectionRule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};

const internalFormValueToPlanV4 = (
  value: InternalPlanFormValue,
  mode: 'create' | 'edit',
  planFormType: PlanFormType,
  apiType: ApiType,
): PlanFormValue => {
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
        selectors: [
          apiType === 'PROXY'
            ? { type: 'HTTP', path: '/', pathOperator: 'STARTS_WITH' }
            : { type: 'CHANNEL', channel: '/', channelOperator: 'STARTS_WITH' },
        ],
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

    // Secure
    mode: planFormType === 'PUSH' ? 'PUSH' : 'STANDARD',
    // Security only availble in standard mode
    ...(planFormType !== 'PUSH'
      ? {
          security: {
            type: planFormType,
            configuration: value.secure.securityConfig,
          },
        }
      : {}),

    excludedGroups: value.general.excludedGroups,
    selectionRule: value.secure.selectionRule,

    // Restriction (only for create mode)
    ...(mode === 'edit' ? {} : { flows: initFlowsWithRestriction(value.restriction) }),
  };
};
