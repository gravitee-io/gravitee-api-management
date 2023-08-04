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
import { FormControl, FormGroup } from '@angular/forms';
import { Observable, of, Subject } from 'rxjs';
import { mergeMap, takeUntil, tap } from 'rxjs/operators';

import { PolicyService } from '../../../../../services-ngx/policy.service';
import { InternalPlanFormValue } from '../api-plan-form.component';

@Component({
  selector: 'plan-edit-restriction-step',
  template: require('./plan-edit-restriction-step.component.html'),
  styles: [require('./plan-edit-restriction-step.component.scss')],
})
export class PlanEditRestrictionStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input()
  public initialFormValues: InternalPlanFormValue['restriction'];

  public restrictionForm: FormGroup;

  public rateLimitSchema$: Observable<undefined | unknown>;
  public quotaSchema$: Observable<undefined | unknown>;
  public resourceFilteringSchema$: Observable<undefined | unknown>;

  constructor(private readonly policyService: PolicyService) {}

  ngOnInit(): void {
    this.restrictionForm = new FormGroup({
      rateLimitEnabled: new FormControl(false),
      rateLimitConfig: new FormControl({}),

      quotaEnabled: new FormControl(false),
      quotaConfig: new FormControl({}),

      resourceFilteringEnabled: new FormControl(false),
      resourceFilteringConfig: new FormControl({}),
    });

    this.rateLimitSchema$ = this.restrictionForm.get('rateLimitEnabled').valueChanges.pipe(
      mergeMap((enabled) => (!enabled ? of(undefined) : this.policyService.getSchema('rate-limit'))),
      tap(() =>
        this.restrictionForm.setControl('rateLimitConfig', new FormControl(this.initialFormValues?.rateLimitConfig ?? {}), {
          emitEvent: false,
        }),
      ),

      takeUntil(this.unsubscribe$),
    );

    this.quotaSchema$ = this.restrictionForm.get('quotaEnabled').valueChanges.pipe(
      mergeMap((enabled) => (!enabled ? of(undefined) : this.policyService.getSchema('quota'))),
      tap(() =>
        this.restrictionForm.setControl('quotaConfig', new FormControl(this.initialFormValues?.quotaConfig ?? {}), { emitEvent: false }),
      ),

      takeUntil(this.unsubscribe$),
    );

    this.resourceFilteringSchema$ = this.restrictionForm.get('resourceFilteringEnabled').valueChanges.pipe(
      mergeMap((enabled) => (!enabled ? of(undefined) : this.policyService.getSchema('resource-filtering'))),
      tap(() =>
        this.restrictionForm.setControl('resourceFilteringConfig', new FormControl(this.initialFormValues?.resourceFilteringConfig ?? {}), {
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
