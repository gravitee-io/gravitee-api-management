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
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { cloneDeep, get, merge } from 'lodash';
import { duration } from 'moment/moment';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { isIso8601DateValid } from '../../api/reporter-settings/reporter-settings-message/iso-8601-date.validator';
import { isWindowedCountValidFormat } from '../../api/reporter-settings/reporter-settings-message/windowed-count-format.validator';
import { WindowedCount, WindowedCountFormatError } from '../../api/reporter-settings/reporter-settings-message/windowed-count';

@Component({
  selector: 'api-logging',
  templateUrl: './api-logging.component.html',
  styleUrls: ['./api-logging.component.scss'],
  standalone: false,
})
export class ApiLoggingComponent implements OnInit, OnDestroy {
  isLoading = true;
  providedConfigurationMessage = 'Configuration provided by the system';
  apiLoggingForm: UntypedFormGroup;
  canUpdateSettings: boolean;
  settings: ConsoleSettings;
  private readonly unsubscribe$ = new Subject();
  public formInitialValues: unknown;
  constructor(
    private readonly fb: UntypedFormBuilder,
    private readonly consoleSettingsService: ConsoleSettingsService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.consoleSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(settings => {
        this.isLoading = false;
        this.settings = settings;

        this.apiLoggingForm = this.fb.group({
          duration: this.fb.group({
            maxDurationMillis: [this.toFormState('logging.maxDurationMillis', undefined, 'logging.default.max.duration')],
          }),
          audit: this.fb.group({
            enabled: [this.toFormState('logging.audit.enabled')],
            trail: this.fb.group({
              enabled: [this.toFormState('logging.audit.trail.enabled')],
            }),
          }),
          user: this.fb.group({
            displayed: [this.toFormState('logging.user.displayed')],
          }),
          messageSampling: this.fb.group({
            probabilistic: this.fb.group({
              default: [
                this.toFormState('logging.messageSampling.probabilistic.default'),
                [Validators.required, Validators.min(0.01), Validators.max(1), this.isDefaultLowerOrEqualThanLimit()],
              ],
              limit: [
                this.toFormState('logging.messageSampling.probabilistic.limit'),
                [Validators.required, Validators.min(0.01), Validators.max(1), this.isDefaultLowerOrEqualThanLimit()],
              ],
            }),
            count: this.fb.group({
              default: [
                this.toFormState('logging.messageSampling.count.default'),
                [Validators.required, Validators.min(1), Validators.pattern('^[0-9]*$'), this.isDefaultGreaterOrEqualThanLimit()],
              ],
              limit: [
                this.toFormState('logging.messageSampling.count.limit'),
                [Validators.required, Validators.min(1), Validators.pattern('^[0-9]*$'), this.isDefaultGreaterOrEqualThanLimit()],
              ],
            }),
            temporal: this.fb.group({
              default: [
                this.toFormState('logging.messageSampling.temporal.default'),
                [Validators.required, isIso8601DateValid(), this.isDefaultGreaterOrEqualThanLimitIso8601()],
              ],
              limit: [
                this.toFormState('logging.messageSampling.temporal.limit'),
                [Validators.required, isIso8601DateValid(), this.isDefaultGreaterOrEqualThanLimitIso8601()],
              ],
            }),
            windowedCount: this.fb.group({
              default: [
                this.toFormState('logging.messageSampling.windowedCount.default'),
                [Validators.required, isWindowedCountValidFormat(), this.isDefaultLowerThanMaxRate()],
              ],
              limit: [
                this.toFormState('logging.messageSampling.windowedCount.limit'),
                [Validators.required, isWindowedCountValidFormat(), this.isDefaultLowerThanMaxRate()],
              ],
            }),
          }),
        });

        this.formInitialValues = this.apiLoggingForm.getRawValue();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (this.apiLoggingForm.invalid) {
      return;
    }

    const formSettingsValue = this.apiLoggingForm.getRawValue();

    const settingsToSave = merge(cloneDeep(this.settings), { logging: formSettingsValue });

    // manual mapping because the form does not match the structure of settings object
    settingsToSave.logging.maxDurationMillis = formSettingsValue.duration.maxDurationMillis;

    this.consoleSettingsService
      .save(settingsToSave)
      .pipe(
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  isReadonly(property: string): boolean {
    return ConsoleSettingsService.isReadonly(this.settings, property);
  }

  toFormState(path: string, defaultValue: unknown = undefined, readonlyKey: string = path) {
    return { value: get(this.settings, path, defaultValue), disabled: this.isReadonly(readonlyKey) };
  }

  isDefaultGreaterOrEqualThanLimit(): ValidatorFn | null {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.parent) {
        return null;
      }
      const defaultControl = control.parent.get('default');
      const limitControl = control.parent.get('limit');

      const error = {
        key: 'defaultLowerThanLimit',
        message: 'Default should be greater than Limit',
      };

      if (defaultControl.value < limitControl.value) {
        this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
        return { [error.key]: error.message };
      } else {
        this.clearDefaultAndLimitCustomError(defaultControl, limitControl, error.key);
        return null;
      }
    };
  }

  isDefaultLowerOrEqualThanLimit(): ValidatorFn | null {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.parent) {
        return null;
      }
      const defaultControl = control.parent.get('default');
      const limitControl = control.parent.get('limit');

      const error = {
        key: 'defaultGreaterThanLimit',
        message: 'Default should be lower than Limit',
      };

      if (defaultControl.value > limitControl.value) {
        this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
        return { [error.key]: error.message };
      } else {
        this.clearDefaultAndLimitCustomError(defaultControl, limitControl, error.key);
        return null;
      }
    };
  }

  isDefaultGreaterOrEqualThanLimitIso8601(): ValidatorFn | null {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.parent) {
        return null;
      }
      const defaultControl = control.parent.get('default');
      const limitControl = control.parent.get('limit');

      const error = {
        key: 'defaultLowerThanLimit',
        message: 'Default should be greater than Limit',
      };

      try {
        const defaultDuration = duration(defaultControl.value);
        const limitDuration = duration(limitControl.value);

        if (defaultDuration < limitDuration) {
          this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
          return { [error.key]: error.message };
        }
      } catch (e) {
        this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
        // we can ignore error as the returned object contains the error message we want to display
        return { [error.key]: error.message };
      }
      this.clearDefaultAndLimitCustomError(defaultControl, limitControl, error.key);
      return null;
    };
  }

  isDefaultLowerThanMaxRate(): ValidatorFn | undefined {
    return (control: AbstractControl): ValidationErrors | undefined => {
      if (!control.parent) {
        return undefined;
      }
      const defaultControl = control.parent.get('default');
      const limitControl = control.parent.get('limit');

      const error = {
        key: 'defaultLowerThanLimit',
        message: 'Default must be a lower rate than limit',
      };

      try {
        const defaultWindowedCount = WindowedCount.parse(defaultControl.value);
        const limitWindowedCount = WindowedCount.parse(limitControl.value);
        if (defaultWindowedCount.rate() > limitWindowedCount.rate()) {
          this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
          return { [error.key]: error.message };
        }
      } catch (e) {
        if (e instanceof WindowedCountFormatError) {
          // this check is useless if the format is incorrect
          return undefined;
        }
        this.applyErrorToDefaultAndLimit(control, defaultControl, limitControl, error);
        return { [error.key]: error.message };
      }
      this.clearDefaultAndLimitCustomError(defaultControl, limitControl, error.key);
      return undefined;
    };
  }

  private clearDefaultAndLimitCustomError(defaultControl: AbstractControl, limitControl: AbstractControl, errorKey: string) {
    // Remove custom error key only if multiples errors, else, set errors to null so field is valid again
    if (defaultControl.hasError(errorKey)) {
      if (defaultControl.errors && Object.keys(defaultControl.errors).length > 1) {
        delete defaultControl?.errors[errorKey];
      } else {
        defaultControl.setErrors(null);
      }
    }

    if (limitControl.hasError(errorKey)) {
      if (limitControl.errors && Object.keys(limitControl.errors).length > 1 && limitControl.hasError(errorKey)) {
        delete limitControl?.errors[errorKey];
      } else {
        limitControl.setErrors(null);
      }
    }

    defaultControl.markAsTouched();
    limitControl.markAsTouched();
  }

  /**
   * Default and Limit fields needs to be consistent together, that's why the validity of one impacts the validity of the other
   */
  private applyErrorToDefaultAndLimit(
    control: AbstractControl,
    defaultControl: AbstractControl,
    limitControl: AbstractControl,
    error: { key: string; message: string },
  ) {
    if (control === defaultControl && !limitControl.hasError(error.key)) {
      limitControl.setErrors({ ...limitControl.errors, [error.key]: error.message });
      defaultControl.markAsTouched();
      limitControl.markAsTouched();
    }
    if (control === limitControl && !defaultControl.hasError(error.key)) {
      defaultControl.setErrors({
        ...defaultControl.errors,
        [error.key]: error.message,
      });
      defaultControl.markAsTouched();
      limitControl.markAsTouched();
    }
  }
}
