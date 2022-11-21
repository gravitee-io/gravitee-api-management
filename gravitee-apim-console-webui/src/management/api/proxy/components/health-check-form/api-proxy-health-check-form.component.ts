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
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, startWith, takeUntil } from 'rxjs/operators';
import { omit } from 'lodash';
import '@gravitee/ui-components/wc/gv-cron-editor';

import { HealthCheck } from '../../../../../entities/health-check';

@Component({
  selector: 'api-proxy-health-check-form',
  template: require('./api-proxy-health-check-form.component.html'),
  styles: [require('./api-proxy-health-check-form.component.scss')],
})
export class ApiProxyHealthCheckFormComponent implements OnChanges, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public static NewHealthCheckFormGroup = (healthCheck?: HealthCheck, isReadOnly = true): FormGroup => {
    const healthCheckStep = healthCheck?.steps?.length > 0 ? healthCheck.steps[0] : undefined;

    return new FormGroup({
      enabled: new FormControl({
        value: healthCheck?.enabled ?? false,
        disabled: isReadOnly,
      }),
      inherit: new FormControl({
        value: healthCheck?.inherit ?? false,
        disabled: isReadOnly,
      }),
      // Trigger
      schedule: new FormControl({
        value: healthCheck?.schedule ?? undefined,
        disabled: isReadOnly,
      }),
      // Request
      method: new FormControl(
        {
          value: healthCheckStep?.request?.method,
          disabled: isReadOnly,
        },
        [Validators.required],
      ),
      path: new FormControl(
        {
          value: healthCheckStep?.request?.path,
          disabled: isReadOnly,
        },
        [Validators.required],
      ),
      body: new FormControl({
        value: healthCheckStep?.request?.body,
        disabled: isReadOnly,
      }),
      headers: new FormControl({
        value: [...(healthCheckStep?.request?.headers ?? [])].map((header) => ({ key: header.name, value: header.value })),
        disabled: isReadOnly,
      }),
      fromRoot: new FormControl({
        value: healthCheckStep?.request?.fromRoot,
        disabled: isReadOnly,
      }),
      // Assertions
      assertions: new FormArray(
        [...(healthCheckStep?.response?.assertions ?? ['#response.status == 200'])].map(
          (assertion) =>
            new FormControl(
              {
                value: assertion,
                disabled: isReadOnly,
              },
              [Validators.required],
            ),
        ),
        [Validators.required],
      ),
    });
  };

  public static HealthCheckFromFormGroup(healthCheckForm: FormGroup): HealthCheck {
    if (!healthCheckForm.get('enabled').value) {
      return {
        enabled: false,
      };
    }

    if (healthCheckForm.get('enabled').value && healthCheckForm.get('inherit').value) {
      return {
        enabled: true,
        inherit: true,
      };
    }

    return {
      enabled: healthCheckForm.get('enabled').value,
      schedule: healthCheckForm.get('schedule').value,
      inherit: healthCheckForm.get('inherit').value,
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

  @Input()
  // If provided, the inherit option is enabled
  public inheritHealthCheck?: HealthCheck;
  private healthCheckFormInitialValue: unknown;

  public isDisabled$: Observable<boolean>;

  public httpMethods = ['GET', 'POST', 'PUT'];

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes.healthCheckForm || changes.inheritHealthCheck) && this.healthCheckForm) {
      const controlKeys = ['schedule', 'method', 'path', 'body', 'fromRoot', 'headers', 'assertions'];

      this.isDisabled$ = combineLatest([
        this.healthCheckForm.get('enabled').valueChanges.pipe(startWith(this.healthCheckForm.get('enabled').value)),
        this.healthCheckForm.get('inherit').valueChanges.pipe(startWith(this.healthCheckForm.get('inherit').value)),
      ]).pipe(
        takeUntil(this.unsubscribe$),
        map(([enabledChecked, inheritChecked]) => this.healthCheckForm.get('enabled').disabled || !enabledChecked || inheritChecked),
      );

      this.isDisabled$.subscribe((disableAll) => {
        if (this.inheritHealthCheck) {
          // inherit health check is enable ony if :
          // - the health check `enabled` field is not disabled
          // - the health check `enabled` field is checked
          // - the inherit health check is enabled
          this.healthCheckForm.get('enabled').enabled && this.healthCheckForm.get('enabled').value && this.inheritHealthCheck.enabled
            ? this.healthCheckForm.get('inherit').enable({ emitEvent: false })
            : this.healthCheckForm.get('inherit').disable({ emitEvent: false });
        }

        controlKeys.forEach((k) => {
          return disableAll
            ? this.healthCheckForm.get(k).disable({ emitEvent: false })
            : this.healthCheckForm.get(k).enable({ emitEvent: false });
        });
      });
    }

    if (changes.inheritHealthCheck && this.inheritHealthCheck) {
      this.healthCheckForm
        .get('inherit')
        .valueChanges.pipe(takeUntil(this.unsubscribe$))
        .subscribe((checked) => {
          // Save or restore previous health check value.
          if (checked) {
            this.healthCheckFormInitialValue = omit(this.healthCheckForm.getRawValue(), ['inherit', 'enabled']);

            const inheritHealthCheckFormValue = ApiProxyHealthCheckFormComponent.NewHealthCheckFormGroup(
              this.inheritHealthCheck,
            ).getRawValue();

            this.healthCheckForm.patchValue(omit(inheritHealthCheckFormValue, ['inherit', 'enabled']));
          } else {
            this.healthCheckForm.patchValue(this.healthCheckFormInitialValue);
          }
        });
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
