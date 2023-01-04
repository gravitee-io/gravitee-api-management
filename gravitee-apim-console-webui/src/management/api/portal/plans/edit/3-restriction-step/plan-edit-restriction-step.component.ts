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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { filter, switchMap, takeUntil, tap } from 'rxjs/operators';

import { PolicyService } from '../../../../../../services-ngx/policy.service';

@Component({
  selector: 'plan-edit-restriction-step',
  template: require('./plan-edit-restriction-step.component.html'),
  styles: [require('./plan-edit-restriction-step.component.scss')],
})
export class PlanEditRestrictionStepComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public restrictionForm: FormGroup;

  public rateLimitSchema: unknown;
  public quotaSchema: unknown;
  public resourceFilteringSchema: unknown;

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

    this.restrictionForm
      .get('rateLimitEnabled')
      .valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        tap(() => (this.rateLimitSchema = undefined)),
        filter((enabled) => enabled),
        switchMap(() => this.policyService.getSchema('rate-limit')),
      )
      .subscribe((schema) => (this.rateLimitSchema = schema));

    this.restrictionForm
      .get('quotaEnabled')
      .valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        tap(() => (this.quotaSchema = undefined)),
        filter((enabled) => enabled),
        switchMap(() => this.policyService.getSchema('quota')),
      )
      .subscribe((schema) => (this.quotaSchema = schema));

    this.restrictionForm
      .get('resourceFilteringEnabled')
      .valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        tap(() => (this.resourceFilteringSchema = undefined)),
        filter((enabled) => enabled),
        switchMap(() => this.policyService.getSchema('resource-filtering')),
      )
      .subscribe((schema) => (this.resourceFilteringSchema = schema));
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onGvSchemaFormError(formKey: string, error: unknown) {
    // Set error at the end of js task. Otherwise it will be reset on value change
    setTimeout(() => {
      this.restrictionForm.get(formKey).setErrors(error ? { error: true } : null);
    }, 0);
  }
}
