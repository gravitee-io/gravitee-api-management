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

import { ActivatedRoute } from '@angular/router';
import { duration } from 'moment/moment';
import { EMPTY, merge } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import {
  AbstractControl,
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Component, DestroyRef, inject, input, InputSignal, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioBannerModule, GioFormSlideToggleModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { isIso8601DateValid } from './iso-8601-date.validator';
import { isWindowedCountValidFormat } from './windowed-count-format.validator';
import { WindowedCount } from './windowed-count';

import { PortalConfiguration } from '../../../../entities/portal/portalSettings';
import { Analytics, ApiV4, SamplingTypeEnum } from '../../../../entities/management-api-v2';
import { PortalConfigurationService } from '../../../../services-ngx/portal-configuration.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';

@Component({
  selector: 'reporter-settings-message',
  templateUrl: './reporter-settings-message.component.html',
  styleUrls: ['./reporter-settings-message.component.scss'],
  imports: [
    CommonModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatTooltipModule,
    ReactiveFormsModule,

    GioBannerModule,
    GioFormSlideToggleModule,
    GioIconsModule,
    GioSaveBarModule,
  ],
})
export class ReporterSettingsMessageComponent implements OnInit {
  form: UntypedFormGroup;
  initialFormValue: unknown;
  settings: PortalConfiguration;
  api: InputSignal<ApiV4> = input.required<ApiV4>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly portalConfigService: PortalConfigurationService,
  ) {}

  public ngOnInit(): void {
    this.portalConfigService
      .get()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(settings => {
        this.settings = settings;
        this.initForm();
        this.handleEnabledChanges();
        this.handleTracingEnabledChanges();
        this.handleSamplingTypeChanges();
        this.handleLoggingModeChanges();
      });
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
        tap(() => {
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
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private initForm(): void {
    const api = this.api();
    const analyticsEnabled = api.analytics?.enabled;
    const loggingModeDisabled = !(api?.analytics?.logging?.mode?.entrypoint || api?.analytics?.logging?.mode?.endpoint);
    const isReadOnly = api.definitionContext?.origin === 'KUBERNETES';

    this.form = new UntypedFormGroup({
      enabled: new UntypedFormControl({
        value: analyticsEnabled,
        disabled: isReadOnly,
      }),
      entrypoint: new UntypedFormControl({
        value: api?.analytics?.logging?.mode?.entrypoint ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      endpoint: new UntypedFormControl({
        value: api?.analytics?.logging?.mode?.endpoint ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingEnabled: new UntypedFormControl({
        value: api.analytics?.tracing?.enabled ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingVerbose: new UntypedFormControl({
        value: api.analytics?.tracing?.verbose ?? false,
        disabled: !analyticsEnabled || !api.analytics?.tracing?.enabled || isReadOnly,
      }),
      request: new UntypedFormControl({
        value: api?.analytics?.logging?.phase?.request ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      response: new UntypedFormControl({
        value: api?.analytics?.logging?.phase?.response ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageContent: new UntypedFormControl({
        value: api?.analytics?.logging?.content?.messagePayload ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageHeaders: new UntypedFormControl({
        value: api?.analytics?.logging?.content?.messageHeaders ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      messageMetadata: new UntypedFormControl({
        value: api?.analytics?.logging?.content?.messageMetadata ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      headers: new UntypedFormControl({
        value: api?.analytics?.logging?.content?.headers ?? false,
        disabled: !analyticsEnabled || loggingModeDisabled || isReadOnly,
      }),
      requestCondition: new UntypedFormControl({
        value: api?.analytics?.logging?.condition,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      messageCondition: new UntypedFormControl({
        value: api?.analytics?.logging?.messageCondition,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      samplingType: new UntypedFormControl(
        {
          value: api?.analytics?.sampling?.type,
          disabled: !analyticsEnabled || isReadOnly,
        },
        Validators.required,
      ),
      samplingValue: new UntypedFormControl(
        {
          value: api?.analytics?.sampling?.value ?? this.getSamplingDefaultValue(api?.analytics?.sampling?.type),
          disabled: !analyticsEnabled || isReadOnly,
        },
        this.getSamplingValueValidators(api?.analytics?.sampling?.type),
      ),
    });
    this.initialFormValue = this.form.getRawValue();
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(enabled => {
        if (enabled) {
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
        } else {
          Object.entries(this.form.controls)
            .filter(([key]) => key !== 'enabled')
            .forEach(([_, control]) => {
              control.disable();
            });
        }
      });
  }

  private handleTracingEnabledChanges(): void {
    this.form
      .get('tracingEnabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(tracingEnabled => {
        if (tracingEnabled) {
          this.form.get('tracingVerbose').enable();
        } else {
          this.disableAndUncheck('tracingVerbose');
        }
      });
  }

  private handleSamplingTypeChanges(): void {
    this.form
      .get('samplingType')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        const samplingValueControl = this.form.get('samplingValue');
        samplingValueControl.setValue(this.getSamplingDefaultValue(value));
        samplingValueControl.setValidators(this.getSamplingValueValidators(value));
        samplingValueControl.updateValueAndValidity();
        samplingValueControl.parent.updateValueAndValidity();
      });
  }

  private handleLoggingModeChanges(): void {
    merge(this.form.get('entrypoint').valueChanges, this.form.get('endpoint').valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
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
      case 'WINDOWED_COUNT':
        return [Validators.required, isWindowedCountValidFormat(), this.isGreaterThanMaxRate()];
      default:
        return [];
    }
  }

  private getSamplingDefaultValue(samplingType: SamplingTypeEnum): string | number | null {
    // If a value is already set for the selected sampling type, then choose the one from api.
    if (this.api()?.analytics?.sampling?.type === samplingType) {
      return this?.api().analytics?.sampling?.value;
    }
    switch (samplingType) {
      case 'PROBABILITY':
        return this.settings?.logging.messageSampling.probabilistic.default ?? 0.01;
      case 'COUNT':
        return this.settings?.logging.messageSampling.count.default ?? 100;
      case 'TEMPORAL':
        return this.settings?.logging.messageSampling.temporal.default ?? 'PT1S';
      case 'WINDOWED_COUNT':
        return this.settings?.logging.messageSampling.windowedCount.default ?? '1/PT10S';
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
        // ignore it because the previous validation should have already failed
        return undefined;
      }
      return undefined;
    };
  }

  isGreaterThanMaxRate = (): ValidatorFn | null => {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      const limit = this.settings?.logging?.messageSampling?.windowedCount?.limit;
      try {
        const valueWindowedCount = WindowedCount.parse(value);
        const limitWindowedCount = WindowedCount.parse(limit);
        if (valueWindowedCount.rate() > limitWindowedCount.rate()) {
          control.markAsTouched();
          return { maxRate: `Windowed Count rate sampling should be less than ${limitWindowedCount.encode()}` };
        }
      } catch (error) {
        control.markAsTouched();
        // ignore it because the previous validation should have already failed
        return undefined;
      }
      return undefined;
    };
  };
}
