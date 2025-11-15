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
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { GioBannerModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { Analytics, Api, ApiV4, SamplingTypeEnum } from '../../../../../../entities/management-api-v2';

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
  private apiId: string = inject(MAT_DIALOG_DATA);

  form: UntypedFormGroup | null = null;
  isLoading = true;
  private api!: ApiV4;
  analytics?: Analytics;

  constructor(public dialogRef: MatDialogRef<WebhookSettingsDialogComponent>) {}

  ngOnInit(): void {
    this.apiService
      .get(this.apiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((api) => {
        if (!this.isApiV4(api)) {
          this.snackBarService.error('Webhook logs settings are only available for v4 APIs.');
          this.dialogRef.close();
          return;
        }
        this.api = api;
        this.analytics = api.analytics;
        this.buildForm(this.analytics);
        this.isLoading = false;
      });
  }

  close(): void {
    this.dialogRef.close();
  }

  private isApiV4(api: Api): api is ApiV4 {
    return api?.definitionVersion === 'V4';
  }

  private buildForm(analytics?: Analytics): void {
    this.form = new UntypedFormGroup({
      enabled: new UntypedFormControl({ value: analytics?.enabled ?? false, disabled: true }),
      requestBody: new UntypedFormControl({ value: analytics?.logging?.content?.messagePayload ?? false, disabled: true }),
      requestHeaders: new UntypedFormControl({ value: analytics?.logging?.content?.messageHeaders ?? false, disabled: true }),
      responseBody: new UntypedFormControl({ value: analytics?.logging?.content?.payload ?? false, disabled: true }),
      responseHeaders: new UntypedFormControl({ value: analytics?.logging?.content?.headers ?? false, disabled: true }),
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
      return `Message count: ${parts?.count ?? '—'}    Time window: ${parts?.window ?? '—'}`;
    }
    return sampling?.value ?? '—';
  }

  get samplingNote(): string {
    const type = this.getSamplingDisplayType(this.analytics?.sampling?.type, this.analytics?.sampling?.value);
    switch (type) {
      case 'PROBABILITY':
        return 'Samples messages based on a specified probability.';
      case 'COUNT':
        return 'Samples one message for every number of specified messages.';
      case 'COUNT_PER_TIME_WINDOW':
        return 'Samples a fixed number of messages for every time window.';
      case 'TEMPORAL':
        return 'Samples messages based on time duration (ISO-8601).';
      default:
        return 'Sampling is not configured for this API.';
    }
  }

  get reporterSettingsLink(): string[] {
    return ['/management', 'apis', this.apiId, 'deployment', 'reporter-settings'];
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
