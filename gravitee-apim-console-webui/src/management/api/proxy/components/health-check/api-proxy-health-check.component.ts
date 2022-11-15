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
import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { FormGroup, FormControl, Validators, FormArray } from '@angular/forms';
import { Observable, Subject } from 'rxjs';
import { map, startWith, takeUntil } from 'rxjs/operators';

import '@gravitee/ui-components/wc/gv-cron-editor';
import { HealthCheck } from '../../../../../entities/health-check';

@Component({
  selector: 'api-proxy-health-check',
  template: require('./api-proxy-health-check.component.html'),
  styles: [require('./api-proxy-health-check.component.scss')],
})
export class ApiProxyHealthCheckComponent implements OnChanges, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public static NewHealthCheckFormGroup = (healthCheck?: HealthCheck): FormGroup => {
    return new FormGroup({
      enabled: new FormControl(healthCheck?.enabled ?? false),
      // Trigger
      schedule: new FormControl(healthCheck?.schedule ?? undefined),
      // Request
      method: new FormControl(healthCheck?.steps[0]?.request?.method, [Validators.required]),
      path: new FormControl(healthCheck?.steps[0]?.request?.path, [Validators.required]),
      body: new FormControl(healthCheck?.steps[0]?.request?.body),
      headers: new FormControl(
        [...(healthCheck?.steps[0]?.request?.headers ?? [])].map((header) => ({ key: header.name, value: header.value })),
      ),
      fromRoot: new FormControl(healthCheck?.steps[0]?.request?.fromRoot),
      // Assertions
      assertions: new FormArray(
        [...(healthCheck?.steps[0]?.response?.assertions ?? ['#response.status == 200'])].map(
          (assertion) => new FormControl(assertion, [Validators.required]),
        ),
        [Validators.required],
      ),
    });
  };

  public static HealthCheckFromFormGroup(healthCheckForm: FormGroup): HealthCheck {
    return {
      enabled: healthCheckForm.get('enabled').value,
      schedule: healthCheckForm.get('schedule').value,
      steps: [
        {
          request: {
            method: healthCheckForm.get('method').value,
            path: healthCheckForm.get('path').value,
            body: healthCheckForm.get('body').value,
            headers: [...healthCheckForm.get('headers').value].map((h) => ({ name: h.key, value: h.value })),
            fromRoot: healthCheckForm.get('fromRoot').value,
          },
          response: {
            assertions: healthCheckForm.get('assertions').value,
          },
        },
      ],
    };
  }

  @Input()
  // Should be init by static NewHealthCheckForm method
  public healthCheckForm: FormGroup;

  public isDisabled$: Observable<boolean>;

  public httpMethods = ['GET', 'POST', 'PUT'];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.healthCheckForm && this.healthCheckForm) {
      const controlKeys = ['schedule', 'method', 'path', 'body', 'fromRoot', 'headers', 'assertions'];
      this.healthCheckForm
        .get('enabled')
        .valueChanges.pipe(takeUntil(this.unsubscribe$), startWith(this.healthCheckForm.get('enabled').value))
        .subscribe((checked) => {
          controlKeys.forEach((k) => {
            return checked ? this.healthCheckForm.get(k).enable() : this.healthCheckForm.get(k).disable();
          });
        });
      this.isDisabled$ = this.healthCheckForm.get('enabled').valueChanges.pipe(
        takeUntil(this.unsubscribe$),
        startWith(this.healthCheckForm.get('enabled').value),
        map((checked) => !checked),
      );
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  addAssertion() {
    const assertions = this.healthCheckForm.get('assertions') as FormArray;

    const assertionControl = new FormControl('', [Validators.required]);
    assertionControl.markAsTouched();

    assertions.push(assertionControl);
    this.healthCheckForm.markAsDirty();
  }

  removeAssertion(index: number) {
    const assertions = this.healthCheckForm.get('assertions') as FormArray;
    assertions.removeAt(index);
    this.healthCheckForm.markAsDirty();
  }
}
