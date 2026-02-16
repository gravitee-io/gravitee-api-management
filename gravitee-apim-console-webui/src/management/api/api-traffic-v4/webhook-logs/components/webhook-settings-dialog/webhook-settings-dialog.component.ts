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

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { GioBannerModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError, finalize, tap } from 'rxjs/operators';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { Analytics, Api, ApiV4, Entrypoint, SamplingTypeEnum } from '../../../../../../entities/management-api-v2';
import { isApiV4 } from '../../../../../../util';
import { WindowedCount } from '../../../../reporter-settings/reporter-settings-message/windowed-count';

export interface WebhookSettingsDialogData {
  api: ApiV4;
}

type WebhookSettingsFormControls = {
  enabled: FormControl<boolean>;
  requestBody: FormControl<boolean>;
  requestHeaders: FormControl<boolean>;
  responseBody: FormControl<boolean>;
  responseHeaders: FormControl<boolean>;
};

type WebhookSettingsFormValue = {
  enabled: boolean;
  requestBody: boolean;
  requestHeaders: boolean;
  responseBody: boolean;
  responseHeaders: boolean;
};

type SamplingDisplayType = SamplingTypeEnum | undefined;

@Component({
  selector: 'webhook-settings-dialog',
  templateUrl: './webhook-settings-dialog.component.html',
  styleUrls: ['./webhook-settings-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    GioBannerModule,
    GioFormSlideToggleModule,
  ],
})
export class WebhookSettingsDialogComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiService = inject(ApiV2Service);
  private readonly snackBarService = inject(SnackBarService);
  private readonly data = inject<WebhookSettingsDialogData>(MAT_DIALOG_DATA);

  form!: FormGroup<WebhookSettingsFormControls>;
  initialFormValue!: WebhookSettingsFormValue;
  isLoading = true;
  isSaving = false;
  private api: ApiV4;
  analytics?: Analytics;
  webhookEntrypoint: Entrypoint | undefined = undefined;

  private readonly loggingControlNames: ReadonlyArray<keyof Omit<WebhookSettingsFormControls, 'enabled'>> = [
    'requestBody',
    'requestHeaders',
    'responseBody',
    'responseHeaders',
  ];

  constructor(public dialogRef: MatDialogRef<WebhookSettingsDialogComponent>) {
    this.api = this.data.api;
  }

  ngOnInit(): void {
    this.analytics = this.api.analytics;
    this.webhookEntrypoint = this.findWebhookEntrypoint(this.api);
    this.buildForm(this.webhookEntrypoint);
    this.handleEnabledChanges();
    this.isLoading = false;
  }

  /**
   * Finds the webhook entrypoint from the API's listeners.
   * The webhook entrypoint configuration contains logging settings at:
   * entrypoint.configuration.logging.{enabled, request.{headers, payload}, response.{headers, payload}}
   */
  private findWebhookEntrypoint(api: ApiV4): Entrypoint | undefined {
    return api.listeners?.flatMap(listener => listener.entrypoints ?? []).find(entrypoint => entrypoint.type === 'webhook') ?? undefined;
  }

  close(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (!this.form || !this.api || this.isSaving) {
      return;
    }

    this.isSaving = true;
    const formValues = this.form.getRawValue();
    const updatedApi = this.buildApiWithEntrypointLogging({ ...this.api }, formValues);

    this.apiService
      .update(this.api.id, updatedApi)
      .pipe(
        tap((updatedApiResponse: Api) => {
          if (isApiV4(updatedApiResponse)) {
            this.api = updatedApiResponse;
            this.analytics = updatedApiResponse.analytics;
            this.webhookEntrypoint = this.findWebhookEntrypoint(updatedApiResponse);
          }
          if (this.form) {
            this.initialFormValue = this.form.getRawValue();
            this.form.markAsPristine();
          }
        }),
        tap(() => {
          this.snackBarService.success('Webhook logs settings successfully saved!');
          this.dialogRef.close({ saved: true });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message || 'Failed to save webhook logs settings');
          return EMPTY;
        }),
        finalize(() => {
          this.isSaving = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private buildApiWithEntrypointLogging(api: ApiV4, formValues: WebhookSettingsFormValue): ApiV4 {
    if (!this.webhookEntrypoint) {
      return api;
    }

    const updatedListeners = api.listeners.map(listener => {
      const entrypoints = listener.entrypoints ?? [];
      const webhookEntrypointIndex = entrypoints.findIndex(ep => ep.type === 'webhook');

      if (webhookEntrypointIndex === -1) {
        return listener;
      }

      const updatedEntrypoints = [...entrypoints];
      const webhookEntrypoint = updatedEntrypoints[webhookEntrypointIndex];

      const loggingConfig = {
        enabled: formValues.enabled,
        request: {
          headers: formValues.requestHeaders,
          payload: formValues.requestBody,
        },
        response: {
          headers: formValues.responseHeaders,
          payload: formValues.responseBody,
        },
      };

      updatedEntrypoints[webhookEntrypointIndex] = {
        ...webhookEntrypoint,
        configuration: {
          ...webhookEntrypoint.configuration,
          logging: loggingConfig,
        },
      };

      return {
        ...listener,
        entrypoints: updatedEntrypoints,
      };
    });

    return {
      ...api,
      listeners: updatedListeners,
    };
  }

  private buildForm(webhookEntrypoint?: Entrypoint): void {
    const entrypointLogging = webhookEntrypoint?.configuration?.logging;
    const enabledValue = entrypointLogging?.enabled ?? false;

    const requestHeadersEnabled = entrypointLogging?.request?.headers ?? false;
    const requestPayloadEnabled = entrypointLogging?.request?.payload ?? false;
    const responseHeadersEnabled = entrypointLogging?.response?.headers ?? false;
    const responsePayloadEnabled = entrypointLogging?.response?.payload ?? false;

    const togglesEnabled = enabledValue;

    this.form = new FormGroup<WebhookSettingsFormControls>({
      enabled: new FormControl<boolean>({ value: enabledValue, disabled: false }, { nonNullable: true }),
      requestBody: new FormControl<boolean>({ value: requestPayloadEnabled, disabled: !togglesEnabled }, { nonNullable: true }),
      requestHeaders: new FormControl<boolean>({ value: requestHeadersEnabled, disabled: !togglesEnabled }, { nonNullable: true }),
      responseBody: new FormControl<boolean>({ value: responsePayloadEnabled, disabled: !togglesEnabled }, { nonNullable: true }),
      responseHeaders: new FormControl<boolean>({ value: responseHeadersEnabled, disabled: !togglesEnabled }, { nonNullable: true }),
    });

    this.initialFormValue = this.form.getRawValue();
  }

  private handleEnabledChanges(): void {
    const form = this.form;
    if (!form) {
      return;
    }

    form.controls.enabled.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(enabled => {
      if (!enabled) {
        // Turn everything off & disable when top toggle is off
        form.patchValue(
          {
            requestBody: false,
            requestHeaders: false,
            responseBody: false,
            responseHeaders: false,
          },
          { emitEvent: false },
        );
        this.setLoggingControlsEnabled(false);
      } else {
        // Re-enable all content toggles when top toggle is on
        this.setLoggingControlsEnabled(true);
      }
    });
  }

  private setLoggingControlsEnabled(enabled: boolean): void {
    if (!this.form) {
      return;
    }

    this.loggingControlNames.forEach(controlName => {
      const control = this.form!.controls[controlName];
      if (enabled) {
        control.enable({ emitEvent: false });
      } else {
        control.disable({ emitEvent: false });
      }
    });
  }

  get samplingModeLabel(): string {
    const type = this.samplingDisplayType;
    switch (type) {
      case 'PROBABILITY':
        return 'Probabilistic';
      case 'COUNT':
        return 'Count';
      case 'WINDOWED_COUNT':
        return 'Windowed Count';
      case 'TEMPORAL':
        return 'Temporal';
      default:
        return 'Not configured';
    }
  }

  get samplingSettingsSummary(): string {
    const sampling = this.sampling;
    const type = this.samplingDisplayType;
    if (!type) {
      return '—';
    }
    if (type === 'WINDOWED_COUNT') {
      if (!sampling?.value) {
        return '—';
      }
      try {
        const windowedCount = WindowedCount.parse(sampling.value);
        return `Message count: ${windowedCount.count} Time window: ${windowedCount.window.toISOString()}`;
      } catch {
        return '—';
      }
    }
    if (type === 'COUNT') {
      return `Message count: ${sampling?.value ?? '—'}`;
    }
    return sampling?.value ?? '—';
  }

  get reporterSettingsLink(): string[] {
    return ['/management', 'apis', this.api.id, 'deployment', 'reporter-settings'];
  }

  hasFormChanged(): boolean {
    if (!this.form || !this.initialFormValue) {
      return false;
    }
    const currentValue = this.form.getRawValue();

    return Object.keys(this.initialFormValue).some(key => currentValue[key] !== this.initialFormValue[key]);
  }

  private get sampling(): Analytics['sampling'] | undefined {
    return this.analytics?.sampling;
  }

  private get samplingDisplayType(): SamplingDisplayType {
    return this.sampling?.type;
  }
}
