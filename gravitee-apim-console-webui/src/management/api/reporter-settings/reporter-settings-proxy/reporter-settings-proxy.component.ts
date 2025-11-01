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
import { Component, DestroyRef, inject, input, InputSignal, OnInit } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EMPTY, merge } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { GioBannerModule, GioFormSlideToggleModule, GioIconsModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

const BOOLEAN_CONFIG_KEYS = [
  'enabled',
  'entrypoint',
  'endpoint',
  'request',
  'response',
  'headers',
  'payload',
  'tracingEnabled',
  'tracingVerbose',
] as const;

type BooleanConfig = (typeof BOOLEAN_CONFIG_KEYS)[number];

const STRING_CONFIG_KEYS = ['condition', 'overrideContentTypeValidation'] as const;
type StringConfig = (typeof STRING_CONFIG_KEYS)[number];

type DefaultConfiguration = Record<BooleanConfig, boolean> & Record<StringConfig, string>;

@Component({
  selector: 'reporter-settings-proxy',
  templateUrl: './reporter-settings-proxy.component.html',
  styleUrls: ['./reporter-settings-proxy.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSnackBarModule,
    ReactiveFormsModule,

    GioBannerModule,
    GioIconsModule,
    GioSaveBarModule,
    GioFormSlideToggleModule,
    MatSlideToggle,
  ],
})
export class ReporterSettingsProxyComponent implements OnInit {
  api: InputSignal<ApiV4> = input.required<ApiV4>();
  form: FormGroup<Record<BooleanConfig, FormControl<boolean>> & Record<StringConfig, FormControl<string>>>;
  defaultConfiguration: DefaultConfiguration;
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.initForm(this.api());
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
                overrideContentTypeValidation: configurationValues.overrideContentTypeValidation,
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
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private initForm(api: ApiV4) {
    const analyticsEnabled = api.analytics?.enabled;
    const atLeastModeIsEnabled = api.analytics?.logging?.mode?.entrypoint || api.analytics?.logging?.mode?.endpoint;
    const isReadOnly = api.definitionContext?.origin === 'KUBERNETES';

    this.form = new FormGroup({
      enabled: new FormControl({ value: analyticsEnabled, disabled: isReadOnly }),
      entrypoint: new FormControl({ value: api.analytics?.logging?.mode?.entrypoint, disabled: !analyticsEnabled || isReadOnly }),
      tracingEnabled: new FormControl({
        value: api.analytics?.tracing?.enabled ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingVerbose: new FormControl({
        value: api.analytics?.tracing?.verbose ?? false,
        disabled: !analyticsEnabled || !api.analytics?.tracing?.enabled || isReadOnly,
      }),
      endpoint: new FormControl({ value: api.analytics?.logging?.mode?.endpoint, disabled: !analyticsEnabled || isReadOnly }),
      request: new FormControl({
        value: api.analytics?.logging?.phase?.request,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      response: new FormControl({
        value: api.analytics?.logging?.phase?.response,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      headers: new FormControl({
        value: api.analytics?.logging?.content?.headers,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      payload: new FormControl({
        value: api.analytics?.logging?.content?.payload,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      condition: new FormControl({
        value: api.analytics?.logging?.condition,
        disabled: !analyticsEnabled || !atLeastModeIsEnabled || isReadOnly,
      }),
      overrideContentTypeValidation: new FormControl({
        value: api.analytics?.logging?.overrideContentTypeValidation,
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
        takeUntilDestroyed(this.destroyRef),
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
        takeUntilDestroyed(this.destroyRef),
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
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private enableLoggingFormFields() {
    this.form.get('request').enable();
    this.form.get('response').enable();
    this.form.get('headers').enable();
    this.form.get('payload').enable();
    this.form.get('condition').enable();
    this.form.get('overrideContentTypeValidation').enable();
  }

  private clearAndDisableLoggingFormFields() {
    ['request', 'response', 'headers', 'payload'].forEach((key) => {
      this.form.get(key).setValue(false);
      this.form.get(key).disable();
    });
    STRING_CONFIG_KEYS.forEach((key) => {
      this.form.get(key).setValue('');
      this.form.get(key).disable();
    });
  }
}
