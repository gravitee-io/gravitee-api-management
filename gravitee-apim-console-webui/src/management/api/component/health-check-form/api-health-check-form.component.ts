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
import { UntypedFormGroup, UntypedFormControl, Validators, UntypedFormArray } from '@angular/forms';
import { combineLatest, Observable, Subject } from 'rxjs';
import { map, shareReplay, startWith, takeUntil } from 'rxjs/operators';
import { omit } from 'lodash';

import { EndpointHealthCheckService } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-health-check-form',
  templateUrl: './api-health-check-form.component.html',
  styleUrls: ['./api-health-check-form.component.scss'],
})
export class ApiHealthCheckFormComponent implements OnChanges, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public static NewHealthCheckFormGroup = (healthCheck?: EndpointHealthCheckService, isReadOnly = true): UntypedFormGroup => {
    // If the health check is disabled and inherit is not false, we need to set inherit to false
    if (healthCheck?.enabled !== undefined && healthCheck?.enabled === false && healthCheck?.inherit !== false) {
      healthCheck.inherit = false;
      healthCheck.enabled = false;
    }

    const healthCheckStep = healthCheck?.steps?.length > 0 ? healthCheck.steps[0] : undefined;
    return new UntypedFormGroup({
      enabled: new UntypedFormControl({
        value: healthCheck?.enabled ?? false,
        disabled: isReadOnly,
      }),
      inherit: new UntypedFormControl({
        value: healthCheck?.inherit ?? true,
        disabled: isReadOnly,
      }),
      // Trigger
      schedule: new UntypedFormControl(
        {
          value: healthCheck?.schedule ?? undefined,
          disabled: isReadOnly,
        },
        [Validators.required],
      ),
      // Request
      method: new UntypedFormControl(
        {
          value: healthCheckStep?.request?.method,
          disabled: isReadOnly,
        },
        [Validators.required],
      ),
      path: new UntypedFormControl(
        {
          value: healthCheckStep?.request?.path,
          disabled: isReadOnly,
        },
        [Validators.required],
      ),
      body: new UntypedFormControl({
        value: healthCheckStep?.request?.body,
        disabled: isReadOnly,
      }),
      headers: new UntypedFormControl({
        value: [...(healthCheckStep?.request?.headers ?? [])].map((header) => ({ key: header.name, value: header.value })),
        disabled: isReadOnly,
      }),
      fromRoot: new UntypedFormControl({
        value: healthCheckStep?.request?.fromRoot,
        disabled: isReadOnly,
      }),
      // Assertions
      assertions: new UntypedFormArray(
        [...(healthCheckStep?.response?.assertions ?? ['#response.status == 200'])].map(
          (assertion) =>
            new UntypedFormControl(
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

  public static HealthCheckFromFormGroup(healthCheckForm: UntypedFormGroup, hasInheritToggle: boolean): EndpointHealthCheckService {
    if (hasInheritToggle && healthCheckForm.get('inherit').value) {
      return {
        inherit: true,
      };
    }

    if (hasInheritToggle && !healthCheckForm.get('inherit').value && !healthCheckForm.get('enabled').value) {
      return {
        enabled: false,
        inherit: false,
      };
    }
    if (!hasInheritToggle && !healthCheckForm.get('enabled').value) {
      return {
        enabled: false,
      };
    }

    return {
      enabled: healthCheckForm.get('enabled').value,
      schedule: healthCheckForm.get('schedule').value,
      ...(hasInheritToggle && { inherit: healthCheckForm.get('inherit').value }),
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
  public healthCheckForm: UntypedFormGroup;

  @Input()
  // If provided, the inherit option is enabled
  public inheritHealthCheck?: EndpointHealthCheckService;

  @Input()
  public isReadOnly: boolean;

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
        map(([enabledChecked, inheritChecked]) => {
          // if the enabled field is disabled, all fields are disabled
          if (this.healthCheckForm.get('enabled').disabled) {
            return true;
          }

          // if the health check is not enabled, all fields are enabled
          if (!enabledChecked) {
            return true;
          }

          // if the inherit health check is configured
          if (this.inheritHealthCheck) {
            // if the inherit health check is enabled, all fields are disabled
            if (inheritChecked) {
              return true;
            }
          }
          return false;
        }),
        shareReplay(1),
        takeUntil(this.unsubscribe$),
      );

      this.isDisabled$.subscribe((disableAll) => {
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
        .valueChanges.pipe(startWith(this.healthCheckForm.get('inherit').value), takeUntil(this.unsubscribe$))
        .subscribe((checked) => {
          // If inherit is checked, "enable" value is set to inherit health check enable value.
          if (checked) {
            this.healthCheckForm.get('enabled').disable({ emitEvent: true });
            this.healthCheckForm.get('enabled').setValue(this.inheritHealthCheck.enabled);
          }

          if (!checked && !this.isReadOnly) {
            this.healthCheckForm.get('enabled').enable({ emitEvent: true });
          }

          // Save or restore previous health check value.
          if (checked) {
            this.healthCheckFormInitialValue = omit(this.healthCheckForm.getRawValue(), ['inherit', 'enabled']);

            if (this.inheritHealthCheck.enabled) {
              const inheritHealthCheckFormValue = ApiHealthCheckFormComponent.NewHealthCheckFormGroup(
                this.inheritHealthCheck,
              ).getRawValue();

              this.healthCheckForm.patchValue(omit(inheritHealthCheckFormValue, ['inherit', 'enabled']));
            }
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
    const assertions = this.healthCheckForm.get('assertions') as UntypedFormArray;

    const assertionControl = new UntypedFormControl('', [Validators.required]);
    assertionControl.markAsTouched();

    assertions.push(assertionControl);
    this.healthCheckForm.markAsDirty();
  }

  removeAssertion(index: number) {
    const assertions = this.healthCheckForm.get('assertions') as UntypedFormArray;
    assertions.removeAt(index);
    this.healthCheckForm.markAsDirty();
  }
}
