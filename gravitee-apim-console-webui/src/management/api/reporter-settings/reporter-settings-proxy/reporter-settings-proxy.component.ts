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
import { Component, computed, DestroyRef, inject, input, InputSignal, OnInit, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { EMPTY, merge } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
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

import { ApiRedactionRulesComponent } from './api-redaction-rules/api-redaction-rules.component';
import { ApiPayloadMaskingRulesComponent } from './api-payload-masking-rules/api-payload-masking-rules.component';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, PayloadMaskingRule, RedactionRule } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

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
    ApiRedactionRulesComponent,
    ApiPayloadMaskingRulesComponent,
  ],
})
export class ReporterSettingsProxyComponent implements OnInit {
  api: InputSignal<ApiV4> = input.required<ApiV4>();
  form: UntypedFormGroup;
  defaultConfiguration: DefaultConfiguration;
  private destroyRef = inject(DestroyRef);

  protected pendingRedactionRules = signal<RedactionRule[] | null>(null);
  protected pendingPayloadMaskingRules = signal<PayloadMaskingRule[] | null>(null);
  protected resetTriggerCounter = signal(0);
  protected savedApiState = signal<ApiV4 | null>(null);
  protected effectiveApi = computed(() => this.savedApiState() ?? this.api());

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.initForm(this.api());
  }

  submit() {
    const pendingRules = this.pendingRedactionRules();
    const pendingPayloadRules = this.pendingPayloadMaskingRules();
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const configurationValues = this.form.getRawValue();
          const existingTracing = api.analytics?.tracing;
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
                ...existingTracing,
                enabled: configurationValues.tracingEnabled,
                verbose: configurationValues.tracingVerbose,
                ...(configurationValues.tracingEnabled && (pendingRules !== null || existingTracing?.redaction != null)
                  ? {
                      redaction: {
                        defaultReplacement: existingTracing?.redaction?.defaultReplacement,
                        rules: pendingRules ?? existingTracing?.redaction?.rules ?? [],
                      },
                    }
                  : {}),
                ...(configurationValues.tracingEnabled && (pendingPayloadRules !== null || existingTracing?.payloadMasking != null)
                  ? {
                      payloadMasking: {
                        defaultReplacement: existingTracing?.payloadMasking?.defaultReplacement,
                        rules: pendingPayloadRules ?? existingTracing?.payloadMasking?.rules ?? [],
                      },
                    }
                  : {}),
              },
            },
          };

          return this.apiService.update(api.id, updatedApi).pipe(map(serverApi => serverApi as ApiV4));
        }),
        tap((savedApi: ApiV4) => {
          this.snackBarService.success('Configuration successfully saved!');
          this.pendingRedactionRules.set(null);
          this.pendingPayloadMaskingRules.set(null);
          this.savedApiState.set(savedApi);
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

  onRedactionRulesChange(rules: RedactionRule[]): void {
    this.pendingRedactionRules.set(rules);
    this.form.markAsDirty();
  }

  onDiscardRedactionRules(): void {
    this.pendingRedactionRules.set(null);
    this.pendingPayloadMaskingRules.set(null);
    this.resetTriggerCounter.update(counter => counter + 1);
  }

  onPayloadMaskingRulesChange(rules: PayloadMaskingRule[]): void {
    this.pendingPayloadMaskingRules.set(rules);
    this.form.markAsDirty();
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
            if (this.form.get('tracingEnabled').value) {
              this.form.get('tracingVerbose').enable();
            }
          } else {
            this.form.get('entrypoint').disable();
            this.form.get('endpoint').disable();
            this.form.get('request').disable();
            this.form.get('response').disable();
            this.form.get('headers').disable();
            this.form.get('payload').disable();
            this.form.get('condition').disable();
            this.form.get('tracingEnabled').disable();
            this.form.get('tracingVerbose').disable();
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
