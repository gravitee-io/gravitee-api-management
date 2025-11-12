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
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { mergeMap, takeUntil, tap } from 'rxjs/operators';

import { PolicyService } from '../../../../../services-ngx/policy.service';
import { InternalPlanFormValue } from '../api-plan-form.component';

@Component({
  selector: 'plan-edit-restriction-step',
  templateUrl: './plan-edit-restriction-step.component.html',
  styleUrls: ['./plan-edit-restriction-step.component.scss'],
  standalone: false,
})
export class PlanEditRestrictionStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input()
  public initialFormValues: InternalPlanFormValue['restriction'];

  public restrictionForm: UntypedFormGroup;

  public rateLimitSchema$: Observable<undefined | unknown>;
  public rateLimitLlmProxySchema$: Observable<undefined | unknown>;
  public quotaSchema$: Observable<undefined | unknown>;
  public resourceFilteringSchema$: Observable<undefined | unknown>;

  constructor(private readonly policyService: PolicyService) {}

  ngOnInit(): void {
    this.restrictionForm = new UntypedFormGroup({
      rateLimitEnabled: new UntypedFormControl(false),
      rateLimitConfig: new UntypedFormControl({}),

      rateLimitLlmProxyEnabled: new UntypedFormControl(false),
      rateLimitLlmProxyConfig: new UntypedFormControl({}),

      quotaEnabled: new UntypedFormControl(false),
      quotaConfig: new UntypedFormControl({}),

      resourceFilteringEnabled: new UntypedFormControl(false),
      resourceFilteringConfig: new UntypedFormControl({}),
    });

    this.rateLimitSchema$ = this.createPolicySchema(
      'rateLimitEnabled',
      'rateLimitConfig',
      'rate-limit',
      this.initialFormValues?.rateLimitConfig,
    );

    this.rateLimitLlmProxySchema$ = this.createPolicySchema(
      'rateLimitLlmProxyEnabled',
      'rateLimitLlmProxyConfig',
      'rate-limit-llm-proxy',
      this.initialFormValues?.rateLimitLlmProxyConfig,
    );

    this.quotaSchema$ = this.createPolicySchema('quotaEnabled', 'quotaConfig', 'quota', this.initialFormValues?.quotaConfig);

    this.resourceFilteringSchema$ = this.createPolicySchema(
      'resourceFilteringEnabled',
      'resourceFilteringConfig',
      'resource-filtering',
      this.initialFormValues?.resourceFilteringConfig,
    );
  }

  private createPolicySchema(
    enabledControlName: string,
    configControlName: string,
    policyName: string,
    initialConfigValue: any,
  ): Observable<unknown | undefined> {
    return this.restrictionForm.get(enabledControlName).valueChanges.pipe(
      mergeMap((enabled) => (!enabled ? of(undefined) : this.policyService.getSchema(policyName))),
      tap(() =>
        this.restrictionForm.setControl(configControlName, new UntypedFormControl(initialConfigValue ?? {}), {
          emitEvent: false,
        }),
      ),
      takeUntil(this.unsubscribe$),
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
