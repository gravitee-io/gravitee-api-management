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
import { Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { EMPTY, merge, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

type DefaultConfiguration = {
  entrypoint: boolean;
  endpoint: boolean;
  request: boolean;
  response: boolean;
  headers: boolean;
  payload: boolean;
  condition: string;
  tracingEnabled: boolean;
  tracingVerbose: boolean;
};

@Component({
  selector: 'api-runtime-logs-proxy-settings',
  templateUrl: './api-runtime-logs-proxy-settings.component.html',
  styleUrls: ['./api-runtime-logs-proxy-settings.component.scss'],
})
export class ApiRuntimeLogsProxySettingsComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  form: UntypedFormGroup;
  defaultConfiguration: DefaultConfiguration;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api: ApiV4) => {
          this.initForm(api);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  submit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const configurationValues = this.form.getRawValue();
          const updatedApi = {
            ...api,
            analytics: {
              ...api.analytics,
              enabled: configurationValues.enabled,
              logging: {
                condition: configurationValues.condition,
                mode: {
                  entrypoint: configurationValues.entrypoint,
                  endpoint: configurationValues.endpoint,
                },
                phase: {
                  request: configurationValues.request,
                  response: configurationValues.response,
                },
                content: {
                  headers: configurationValues.headers,
                  payload: configurationValues.payload,
                },
              },
              tracing: {
                enabled: configurationValues.tracingEnabled,
                verbose: configurationValues.tracingVerbose,
              },
            },
          };

          return this.apiService.update(api.id, updatedApi);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => {
          this.defaultConfiguration = this.form.getRawValue();
          this.form.reset(this.defaultConfiguration, { emitEvent: false });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm(api: ApiV4) {
    const analyticsEnabled = api.analytics?.enabled;
    const atLeastModeIsEnabled = api.analytics?.logging?.mode?.entrypoint || api.analytics?.logging?.mode?.endpoint;
    const isReadOnly = api.definitionContext?.origin === 'KUBERNETES';

    this.form = new UntypedFormGroup({
      enabled: new UntypedFormControl({ value: analyticsEnabled, disabled: isReadOnly }),
      entrypoint: new UntypedFormControl({ value: api.analytics?.logging?.mode?.entrypoint, disabled: !analyticsEnabled || isReadOnly }),
      tracingEnabled: new UntypedFormControl({
        value: api.analytics?.tracing?.enabled ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingVerbose: new UntypedFormControl({
        value: api.analytics?.tracing?.verbose ?? false,
        disabled: !analyticsEnabled || !api.analytics?.tracing?.enabled || isReadOnly,
      }),
      endpoint: new UntypedFormControl({ value: api.analytics?.logging?.mode?.endpoint, disabled: !analyticsEnabled || isReadOnly }),
      request: new UntypedFormControl({
        value: api.analytics?.logging?.phase?.request,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      response: new UntypedFormControl({
        value: api.analytics?.logging?.phase?.response,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      headers: new UntypedFormControl({
        value: api.analytics?.logging?.content?.headers,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      payload: new UntypedFormControl({
        value: api.analytics?.logging?.content?.payload,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      condition: new UntypedFormControl({
        value: api.analytics?.logging?.condition,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
    });

    this.defaultConfiguration = this.form.getRawValue();

    if (!analyticsEnabled || !this.form.get('tracingEnabled').value || isReadOnly) {
      this.form.get('tracingVerbose').disable();
    } else {
      this.form.get('tracingVerbose').enable();
    }

    this.handleTracingEnabledChanges();

    this.form
      .get('enabled')
      .valueChanges.pipe(
        tap((enabled: boolean) => {
          if (enabled) {
            this.form.get('entrypoint').enable();
            this.form.get('endpoint').enable();
            this.form.get('tracingEnabled').enable();
          } else {
            this.form.get('entrypoint').disable();
            this.form.get('endpoint').disable();
            this.form.get('request').disable();
            this.form.get('response').disable();
            this.form.get('headers').disable();
            this.form.get('payload').disable();
            this.form.get('condition').disable();
            this.form.get('tracingEnabled').disable();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();

    merge(this.form.get('entrypoint').valueChanges, this.form.get('endpoint').valueChanges)
      .pipe(
        tap(() => {
          if (this.form.get('entrypoint').value || this.form.get('endpoint').value) {
            this.enableLoggingFormFields();
          } else {
            this.clearAndDisableLoggingFormFields();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private handleTracingEnabledChanges(): void {
    this.form
      .get('tracingEnabled')
      .valueChanges.pipe(
        tap((tracingEnabled: boolean) => {
          if (!tracingEnabled) {
            this.form.get('tracingVerbose').disable();
          } else {
            this.form.get('tracingVerbose').enable();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private enableLoggingFormFields() {
    this.form.get('request').enable();
    this.form.get('response').enable();
    this.form.get('headers').enable();
    this.form.get('payload').enable();
    this.form.get('condition').enable();
  }

  private clearAndDisableLoggingFormFields() {
    this.form.get('request').setValue(false);
    this.form.get('request').disable();
    this.form.get('response').setValue(false);
    this.form.get('response').disable();
    this.form.get('headers').setValue(false);
    this.form.get('headers').disable();
    this.form.get('payload').setValue(false);
    this.form.get('payload').disable();
    this.form.get('condition').setValue('');
    this.form.get('condition').disable();
  }
}
