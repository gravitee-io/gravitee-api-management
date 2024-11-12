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
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, merge, Subject } from 'rxjs';
import { duration } from 'moment/moment';
import { ActivatedRoute } from '@angular/router';

import { isIso8601DateValid } from './iso-8601-date.validator';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { Analytics, ApiV4, SamplingTypeEnum } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ConsoleSettings } from '../../../../../entities/consoleSettings';
import { ConsoleSettingsService } from '../../../../../services-ngx/console-settings.service';

@Component({
  selector: 'api-runtime-logs-message-settings',
  templateUrl: './api-runtime-logs-message-settings.component.html',
  styleUrls: ['./api-runtime-logs-message-settings.component.scss'],
})
export class ApiRuntimeLogsMessageSettingsComponent implements OnInit, OnDestroy {
  @Input() public api: ApiV4;
  form: UntypedFormGroup;
  initialFormValue: unknown;
  settings: ConsoleSettings;
  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly consoleSettingsService: ConsoleSettingsService,
  ) {}

  public ngOnInit(): void {
    this.consoleSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings) => {
        this.settings = settings;
        this.initForm();
        this.handleEnabledChanges();
        this.handleTracingEnabledChanges();
        this.handleSamplingTypeChanges();
        this.handleLoggingModeChanges();
      });
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public save(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const formValues = this.form.getRawValue();
          const sampling = formValues.samplingType ? { type: formValues.samplingType, value: formValues.samplingValue?.toString() } : null;
          const analytics: Analytics = {
            enabled: formValues.enabled,
            logging: {
              mode: {
                entrypoint: formValues.entrypoint,
                endpoint: formValues.endpoint,
              },
              phase: {
                request: formValues.request,
                response: formValues.response,
              },
              content: {
                messagePayload: formValues.messageContent,
                messageHeaders: formValues.messageHeaders,
                messageMetadata: formValues.messageMetadata,
                headers: formValues.headers,
              },
              condition: formValues.requestCondition,
              messageCondition: formValues.messageCondition,
            },
            tracing: {
              enabled: formValues.tracingEnabled,
              verbose: formValues.tracingVerbose,
            },
            sampling: sampling,
          };
          return this.apiService.update(api.id, { ...api, analytics });
        }),
        tap((api: ApiV4) => {
          this.api = api;
          this.initialFormValue = this.form.getRawValue();
          this.form.markAsPristine();
        }),
        map(() => {
          this.snackBarService.success('Runtime logs settings successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm(): void {
    const analyticsEnabled = this.api.analytics?.enabled;
    const loggingModeDisabled = !(this.api?.analytics?.logging?.mode?.entrypoint || this.api?.analytics?.logging?.mode?.endpoint);
    const isReadOnly = this.api.definitionContext?.origin === 'KUBERNETES';

    this.form = new UntypedFormGroup({
      enabled: new UntypedFormControl({
        value: analyticsEnabled,
        disabled: isReadOnly,
      }),
      entrypoint: new UntypedFormControl({
        value: this.api?.analytics?.logging?.mode?.entrypoint ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      endpoint: new UntypedFormControl({
        value: this.api?.analytics?.logging?.mode?.endpoint ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingEnabled: new UntypedFormControl({
        value: this.api.analytics?.tracing?.enabled ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingVerbose: new UntypedFormControl({
        value: this.api.analytics?.tracing?.verbose ?? false,
        disabled: !analyticsEnabled || !this.api.analytics?.tracing?.enabled || isReadOnly,
      }),
      request: new UntypedFormControl({
        value: this.api?.analytics?.logging?.phase?.request ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      response: new UntypedFormControl({
        value: this.api?.analytics?.logging?.phase?.response ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageContent: new UntypedFormControl({
        value: this.api?.analytics?.logging?.content?.messagePayload ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageHeaders: new UntypedFormControl({
        value: this.api?.analytics?.logging?.content?.messageHeaders ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageMetadata: new UntypedFormControl({
        value: this.api?.analytics?.logging?.content?.messageMetadata ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      headers: new UntypedFormControl({
        value: this.api?.analytics?.logging?.content?.headers ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      requestCondition: new UntypedFormControl({
        value: this.api?.analytics?.logging?.condition,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      messageCondition: new UntypedFormControl({
        value: this.api?.analytics?.logging?.messageCondition,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      samplingType: new UntypedFormControl(
        {
          value: this.api?.analytics?.sampling?.type,
          disabled: !analyticsEnabled || isReadOnly,
        },
        Validators.required,
      ),
      samplingValue: new UntypedFormControl(
        {
          value: this.api?.analytics?.sampling?.value ?? this.getSamplingDefaultValue(this.api?.analytics?.sampling?.type),
          disabled: !analyticsEnabled || isReadOnly,
        },
        this.getSamplingValueValidators(this.api?.analytics?.sampling?.type),
      ),
    });
    this.initialFormValue = this.form.getRawValue();
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabled')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((enabled) => {
        if (!enabled) {
          Object.entries(this.form.controls)
            .filter(([key]) => key !== 'enabled')
            .forEach(([_, control]) => {
              control.disable();
            });
        } else {
          Object.entries(this.form.controls)
            .filter(([key]) => key !== 'enabled')
            .forEach(([key, control]) => {
              if (['request', 'response', 'messageContent', 'messageHeaders', 'messageMetadata', 'headers'].includes(key)) {
                const loggingModeDisabled = !(this.form.get('entrypoint').value || this.form.get('endpoint').value);
                if (!loggingModeDisabled) {
                  control.enable();
                }
              } else {
                control.enable();
              }
            });
        }
      });
  }

  private handleTracingEnabledChanges(): void {
    this.form
      .get('tracingEnabled')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((tracingEnabled) => {
        if (!tracingEnabled) {
          this.disableAndUncheck('tracingVerbose');
        } else {
          this.form.get('tracingVerbose').enable();
        }
      });
  }

  private handleSamplingTypeChanges(): void {
    this.form
      .get('samplingType')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        const samplingValueControl = this.form.get('samplingValue');
        samplingValueControl.setValue(this.getSamplingDefaultValue(value));
        samplingValueControl.setValidators(this.getSamplingValueValidators(value));
        samplingValueControl.updateValueAndValidity();
        samplingValueControl.parent.updateValueAndValidity();
      });
  }

  private handleLoggingModeChanges(): void {
    merge(this.form.get('entrypoint').valueChanges, this.form.get('endpoint').valueChanges)
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(() => {
        const formValues = this.form.getRawValue();
        const loggingModeDisabled = !(formValues.entrypoint || formValues.endpoint);
        if (loggingModeDisabled) {
          this.disableAndUncheck('request');
          this.disableAndUncheck('response');
          this.disableAndUncheck('messageContent');
          this.disableAndUncheck('messageHeaders');
          this.disableAndUncheck('messageMetadata');
          this.disableAndUncheck('headers');
        } else {
          this.form.get('request').enable();
          this.form.get('response').enable();
          this.form.get('messageContent').enable();
          this.form.get('messageHeaders').enable();
          this.form.get('messageMetadata').enable();
          this.form.get('headers').enable();
        }
      });
  }

  private disableAndUncheck(controlName: string): void {
    this.form.get(controlName).setValue(false);
    this.form.get(controlName).disable();
  }

  private getSamplingValueValidators(samplingType: SamplingTypeEnum): ValidatorFn[] {
    switch (samplingType) {
      case 'PROBABILITY':
        return [
          Validators.required,
          Validators.min(0.01),
          Validators.max(this.settings?.logging.messageSampling.probabilistic.limit ?? 0.5),
        ];
      case 'COUNT':
        return [Validators.required, Validators.min(this.settings?.logging.messageSampling.count.limit ?? 10)];
      case 'TEMPORAL':
        return [Validators.required, isIso8601DateValid(), this.isGreaterOrEqualThanLimitIso8601()];
      default:
        return [];
    }
  }

  private getSamplingDefaultValue(samplingType: SamplingTypeEnum): string | number | null {
    // If a value is already set for the selected sampling type, then choose the one from api.
    if (this.api?.analytics?.sampling?.type === samplingType) {
      return this?.api.analytics?.sampling?.value;
    }
    switch (samplingType) {
      case 'PROBABILITY':
        return this.settings?.logging.messageSampling.probabilistic.default ?? 0.01;
      case 'COUNT':
        return this.settings?.logging.messageSampling.count.default ?? 100;
      case 'TEMPORAL':
        return this.settings?.logging.messageSampling.temporal.default ?? 'PT1S';
      default:
        return null;
    }
  }

  isGreaterOrEqualThanLimitIso8601(): ValidatorFn | null {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      const limit = this.settings?.logging?.messageSampling?.temporal?.limit;

      try {
        const valueDuration = duration(value);
        const limitDuration = duration(limit);

        if (!!value && valueDuration.asSeconds() > 0 && valueDuration < limitDuration) {
          control.markAsTouched();
          return { minTemporal: `Temporal message sampling should be greater than ${limit}` };
        }
      } catch (e) {
        control.markAsTouched();
        return null;
      }
      return null;
    };
  }
}
