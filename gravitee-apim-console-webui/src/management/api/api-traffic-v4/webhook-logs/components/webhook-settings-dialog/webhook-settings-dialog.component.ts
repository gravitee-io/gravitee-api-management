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
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { GioBannerModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { Analytics, Api, ApiV4, SamplingTypeEnum } from '../../../../../../entities/management-api-v2';
import { isApiV4 } from '../../../../../../util';

export interface WebhookSettingsDialogData {
  api: ApiV4;
}

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
    GioBannerModule,
    GioFormSlideToggleModule,
  ],
})
export class WebhookSettingsDialogComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private apiService = inject(ApiV2Service);
  private snackBarService = inject(SnackBarService);
  private readonly data = inject<WebhookSettingsDialogData>(MAT_DIALOG_DATA);

  form: FormGroup<{
    enabled: FormControl<boolean>;
    requestBody: FormControl<boolean>;
    requestHeaders: FormControl<boolean>;
    responseBody: FormControl<boolean>;
    responseHeaders: FormControl<boolean>;
  }> | null = null;
  isLoading = true;
  initialFormValue: Record<string, unknown> = {};
  private api: ApiV4;
  analytics?: Analytics;

  constructor(public dialogRef: MatDialogRef<WebhookSettingsDialogComponent>) {
    this.api = this.data.api;
  }

  ngOnInit(): void {
    // TODO: Backend Integration Verification
    // - Verify that ApiV2Service.get() and ApiV2Service.update() work correctly with webhook analytics settings
    // - Ensure backend properly handles analytics.enabled and analytics.logging.content fields
    // - Verify that API update triggers appropriate events (e.g., NEED_REDEPLOY banner)
    // - Test that settings persist correctly after save
    // - Consider adding loading indicators during save operation
    // - Verify error handling covers all edge cases (network errors, validation errors, etc.)

    this.analytics = this.api.analytics;
    this.buildForm(this.analytics);
    this.handleEnabledChanges();
    this.isLoading = false;
  }

  close(): void {
    this.dialogRef.close();
  }

  save(): void {
    if (!this.form || !this.api) {
      return;
    }

    const formValues = this.form.getRawValue();
    const analytics: Analytics = {
      ...this.api.analytics,
      enabled: formValues.enabled,
      logging: {
        ...this.api.analytics?.logging,
        content: {
          ...this.api.analytics?.logging?.content,
          messagePayload: formValues.requestBody,
          messageHeaders: formValues.requestHeaders,
          payload: formValues.responseBody,
          headers: formValues.responseHeaders,
        },
      },
    };

    this.apiService
      .update(this.api.id, { ...this.api, analytics })
      .pipe(
        tap((updatedApi: Api) => {
          if (isApiV4(updatedApi)) {
            this.api = updatedApi;
            this.analytics = updatedApi.analytics;
          }
          this.initialFormValue = this.form!.getRawValue();
          this.form!.markAsPristine();
        }),
        map(() => {
          this.snackBarService.success('Webhook logs settings successfully saved!');
          this.dialogRef.close({ saved: true });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message || 'Failed to save webhook logs settings');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private buildForm(analytics?: Analytics): void {
    const enabledValue = analytics?.enabled ?? false;
    const requestBodyValue = analytics?.logging?.content?.messagePayload ?? false;
    const requestHeadersValue = analytics?.logging?.content?.messageHeaders ?? false;
    const responseBodyValue = analytics?.logging?.content?.payload ?? false;
    const responseHeadersValue = analytics?.logging?.content?.headers ?? false;

    // Top toggle is always editable and reflects the value set at API creation
    // Content data toggles should be disabled if analytics is disabled
    const contentDataDisabled = !enabledValue;

    this.form = new FormGroup({
      enabled: new FormControl<boolean>({ value: enabledValue, disabled: false }, { nonNullable: true }),
      requestBody: new FormControl<boolean>({ value: requestBodyValue, disabled: contentDataDisabled }, { nonNullable: true }),
      requestHeaders: new FormControl<boolean>({ value: requestHeadersValue, disabled: contentDataDisabled }, { nonNullable: true }),
      responseBody: new FormControl<boolean>({ value: responseBodyValue, disabled: contentDataDisabled }, { nonNullable: true }),
      responseHeaders: new FormControl<boolean>({ value: responseHeadersValue, disabled: contentDataDisabled }, { nonNullable: true }),
    });

    this.initialFormValue = this.form.getRawValue();
  }

  private handleEnabledChanges(): void {
    if (!this.form) {
      return;
    }
    const contentDataControls = ['requestBody', 'requestHeaders', 'responseBody', 'responseHeaders'] as const;

    this.form.controls.enabled.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((enabled) => {
      if (!enabled) {
        // When analytics is disabled, set all content data toggles to false and disable them
        this.form!.patchValue(
          {
            requestBody: false,
            requestHeaders: false,
            responseBody: false,
            responseHeaders: false,
          },
          { emitEvent: false },
        );
        contentDataControls.forEach((controlName) => {
          this.form!.get(controlName)?.disable({ emitEvent: false });
        });
      } else {
        // When analytics is enabled, enable all content data toggles
        contentDataControls.forEach((controlName) => {
          this.form!.get(controlName)?.enable({ emitEvent: false });
        });
      }
    });
  }

  get samplingModeLabel(): string {
    const sampling = this.analytics?.sampling;
    const type = this.getSamplingDisplayType(sampling?.type, sampling?.value);
    switch (type) {
      case 'PROBABILITY':
        return 'Probabilistic';
      case 'COUNT':
        return 'Count';
      case 'COUNT_PER_TIME_WINDOW':
        return 'Count per time window';
      case 'TEMPORAL':
        return 'Temporal';
      default:
        return 'Not configured';
    }
  }

  get samplingSettingsSummary(): string {
    const sampling = this.analytics?.sampling;
    const type = this.getSamplingDisplayType(sampling?.type, sampling?.value);
    if (!type) {
      return '—';
    }
    if (type === 'COUNT_PER_TIME_WINDOW') {
      const parts = this.getCountPerWindowParts(sampling?.value);
      return `Message count: ${parts?.count ?? '—'} Time window: ${parts?.window ?? '—'}`;
    }
    if (type === 'COUNT') {
      return `Message count: ${sampling?.value ?? '—'}`;
    }
    return sampling?.value ?? '—';
  }

  get samplingCountPerWindowParts(): { count?: number; window?: number } | null {
    const sampling = this.analytics?.sampling;
    const type = this.getSamplingDisplayType(sampling?.type, sampling?.value);
    if (type === 'COUNT_PER_TIME_WINDOW') {
      return this.getCountPerWindowParts(sampling?.value);
    }
    return null;
  }

  get reporterSettingsLink(): string[] {
    return ['/management', 'apis', this.api.id, 'deployment', 'reporter-settings'];
  }

  hasFormChanged(): boolean {
    if (!this.form) {
      return false;
    }
    const currentValue = this.form.getRawValue();
    return Object.keys(this.initialFormValue).some((key) => currentValue[key] !== this.initialFormValue[key]);
  }

  private getSamplingDisplayType(type: SamplingTypeEnum | undefined | null, value?: string | null): SamplingDisplayType {
    if (type === 'COUNT' && value?.includes('/')) {
      return 'COUNT_PER_TIME_WINDOW';
    }
    return type ?? null;
  }

  private getCountPerWindowParts(value?: string | null): { count?: number; window?: number } | null {
    if (!value?.includes('/')) {
      return null;
    }
    const [countRaw, windowRaw] = value.split('/');
    const count = Number(countRaw);
    const window = Number(windowRaw);
    return { count: Number.isFinite(count) ? count : undefined, window: Number.isFinite(window) ? window : undefined };
  }
}

type SamplingDisplayType = SamplingTypeEnum | 'COUNT_PER_TIME_WINDOW' | null;
