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
import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';

import { PlanEditGeneralStepComponent } from './1-general-step/plan-edit-general-step.component';

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
export class ApiPortalPlanEditComponent implements AfterViewInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public planForm = new FormGroup({});
  public initialPlanFormValue: unknown;

  @ViewChild(PlanEditGeneralStepComponent) planEditGeneralStepComponent: PlanEditGeneralStepComponent;

  constructor(private readonly changeDetectorRef: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    this.planForm = new FormGroup({
      general: this.planEditGeneralStepComponent.generalForm,
    });

    // Manually trigger change detection to avoid ExpressionChangedAfterItHasBeenCheckedError
    this.changeDetectorRef.detectChanges();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
