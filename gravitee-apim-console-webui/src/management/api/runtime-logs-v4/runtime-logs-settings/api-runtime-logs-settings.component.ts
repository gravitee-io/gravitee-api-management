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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, merge, Subject, Subscription } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Analytics, ApiV4, SamplingTypeEnum } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-runtime-logs-settings',
  template: require('./api-runtime-logs-settings.component.html'),
  styles: [require('./api-runtime-logs-settings.component.scss')],
})
export class ApiRuntimeLogsSettingsComponent implements OnInit, OnDestroy {
  form: FormGroup;
  enabled = false;
  samplingType: SamplingTypeEnum;
  hasLoggingModeEnabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();
  private api: ApiV4;
  private samplingTypeSubscription$: Subscription;
  private loggingModeSubscription$: Subscription;
  private enabledFormControl: FormControl;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api: ApiV4) => {
          this.api = api;
          this.enabled = api?.analytics?.enabled ?? false;
          this.samplingType = this.enabled ? api?.analytics?.sampling?.type : undefined;
          this.enabledFormControl = new FormControl(this.enabled);
          this.hasLoggingModeEnabled = api?.analytics?.logging?.mode?.entrypoint || api?.analytics?.logging?.mode?.endpoint;
          this.initForm();
          this.handleEnabledChanges();
          this.handleSamplingTypeChanges();
          this.handleLoggingModeChanges();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public save(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const formValues = this.form.getRawValue();
          const sampling = formValues.samplingType ? { type: formValues.samplingType, value: formValues.samplingValue?.toString() } : null;
          const analytics: Analytics = this.enabled
            ? {
                enabled: this.enabled,
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
                    payload: formValues.requestPayload,
                    headers: formValues.requestHeaders,
                  },
                  condition: formValues.requestCondition,
                  messageCondition: formValues.messageCondition,
                },
                sampling: sampling,
              }
            : { enabled: this.enabled };
          return this.apiService.update(api.id, { ...api, analytics });
        }),
        tap((api: ApiV4) => {
          this.api = api;
        }),
        map(() => {
          this.snackBarService.success(`Runtime logs settings successfully saved!`);
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
    this.form = this.enabled
      ? new FormGroup({
          enabled: this.enabledFormControl,
          entrypoint: new FormControl(this.api?.analytics?.logging?.mode?.entrypoint ?? false),
          endpoint: new FormControl(this.api?.analytics?.logging?.mode?.endpoint ?? false),
          request: new FormControl(this.api?.analytics?.logging?.phase?.request ?? false),
          response: new FormControl(this.api?.analytics?.logging?.phase?.response ?? false),
          messageContent: new FormControl({
            value: this.api?.analytics?.logging?.content?.messagePayload ?? false,
            disabled: !this.hasLoggingModeEnabled,
          }),
          messageHeaders: new FormControl({
            value: this.api?.analytics?.logging?.content?.messageHeaders ?? false,
            disabled: !this.hasLoggingModeEnabled,
          }),
          messageMetadata: new FormControl({
            value: this.api?.analytics?.logging?.content?.messageMetadata ?? false,
            disabled: !this.hasLoggingModeEnabled,
          }),
          requestPayload: new FormControl(this.api?.analytics?.logging?.content?.payload ?? false),
          requestHeaders: new FormControl(this.api?.analytics?.logging?.content?.headers ?? false),
          requestCondition: new FormControl(this.api?.analytics?.logging?.condition),
          messageCondition: new FormControl(this.api?.analytics?.logging?.messageCondition),
          samplingType: new FormControl(this.samplingType, Validators.required),
          samplingValue: new FormControl(
            this.api?.analytics?.sampling?.value ?? this.getSamplingDefaultValue(this.samplingType),
            this.getSamplingValueValidators(this.samplingType),
          ),
        })
      : new FormGroup({
          enabled: this.enabledFormControl,
        });
    this.form.updateValueAndValidity();
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabled')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.enabled = value;
        this.samplingType = this.enabled ? 'PROBABILITY' : undefined;
        this.initForm();
        this.handleSamplingTypeChanges();
        this.handleLoggingModeChanges();
      });
  }

  private handleSamplingTypeChanges(): void {
    if (this.enabled) {
      this.samplingTypeSubscription$ = this.form
        .get('samplingType')
        .valueChanges.pipe(takeUntil(this.unsubscribe$))
        .subscribe((value) => {
          this.samplingType = value;
          const samplingValueControl = this.form.get('samplingValue');
          samplingValueControl.setValue(this.getSamplingDefaultValue(value));
          samplingValueControl.setValidators(this.getSamplingValueValidators(value));
          samplingValueControl.updateValueAndValidity();
        });
    } else {
      this.samplingTypeSubscription$?.unsubscribe();
    }
  }

  private handleLoggingModeChanges(): void {
    if (this.enabled) {
      this.loggingModeSubscription$ = merge(this.form.get('entrypoint').valueChanges, this.form.get('endpoint').valueChanges)
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe(() => {
          const formValues = this.form.getRawValue();
          this.hasLoggingModeEnabled = formValues.entrypoint || formValues.endpoint;
          if (this.hasLoggingModeEnabled) {
            this.form.get('messageContent').enable();
            this.form.get('messageHeaders').enable();
            this.form.get('messageMetadata').enable();
          } else {
            this.form.get('messageContent').setValue(false);
            this.form.get('messageContent').disable();
            this.form.get('messageHeaders').setValue(false);
            this.form.get('messageHeaders').disable();
            this.form.get('messageMetadata').setValue(false);
            this.form.get('messageMetadata').disable();
          }
        });
    } else {
      this.loggingModeSubscription$?.unsubscribe();
    }
  }

  private getSamplingValueValidators(samplingType: SamplingTypeEnum): ValidatorFn[] {
    switch (samplingType) {
      case 'PROBABILITY':
        return [Validators.required, Validators.min(0.01), Validators.max(0.5)];
      case 'COUNT':
        return [Validators.required, Validators.min(0)];
      case 'TEMPORAL':
        return [Validators.required];
      default:
        return [];
    }
  }

  private getSamplingDefaultValue(samplingType: SamplingTypeEnum): string | number | null {
    switch (samplingType) {
      case 'PROBABILITY':
        return 0.01;
      case 'COUNT':
        return 100;
      case 'TEMPORAL':
        return 'PT1S';
      default:
        return null;
    }
  }
}
