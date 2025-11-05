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
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { ReactiveFormsModule, UntypedFormControl, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { EMPTY } from 'rxjs';

import { ApiV2Service } from '../../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { Analytics, ApiV4, SamplingTypeEnum } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'webhook-settings-dialog',
  templateUrl: './webhook-settings-dialog.component.html',
  styleUrls: ['./webhook-settings-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule,
    GioBannerModule,
  ],
})
export class WebhookSettingsDialogComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  // To Do: understand why we are using ApiV2Service and if it needs to be changed
  private apiService = inject(ApiV2Service);
  private snackBarService = inject(SnackBarService);
  private apiId: string = inject(MAT_DIALOG_DATA);

  form: UntypedFormGroup;
  initialFormValue: unknown;
  isLoading = true;
  private api: ApiV4;

  constructor(public dialogRef: MatDialogRef<WebhookSettingsDialogComponent>) {}

  ngOnInit(): void {
    this.apiService
      .get(this.apiId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((api: ApiV4) => {
        this.api = api;
        this.initForm();
        this.handleEnabledChanges();
        this.handleSamplingTypeChanges();
        this.isLoading = false;
      });
  }

  public save(): void {
    if (this.form.invalid) {
      return;
    }

    this.apiService
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const formValues = this.form.getRawValue();
          let samplingValue: string;
          let samplingType: SamplingTypeEnum;

          if (formValues.samplingType === 'COUNT_PER_TIME_WINDOW') {
            // TODO: Backend support pending for COUNT_PER_TIME_WINDOW
            // Will save as COUNT type with "count/window" value format (e.g., "10/60")
            // Backend needs to validate and parse this format when implemented
            samplingType = 'COUNT';
            samplingValue = `${formValues.messageCount}/${formValues.timeWindow}`;
          } else {
            samplingType = formValues.samplingType as SamplingTypeEnum;
            samplingValue = formValues.samplingValue?.toString();
          }

          const sampling = samplingType ? { type: samplingType, value: samplingValue } : null;

          const analytics: Analytics = {
            ...api.analytics,
            enabled: formValues.enabled,
            logging: {
              ...api.analytics?.logging,
              content: {
                ...api.analytics?.logging?.content,
                messagePayload: formValues.requestBody,
                messageHeaders: formValues.requestHeaders,
                headers: formValues.responseHeaders,
                payload: formValues.responseBody,
              },
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
          this.snackBarService.success('Webhook logs settings successfully saved!');
          this.dialogRef.close({ saved: true });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private initForm(): void {
    const analyticsEnabled = this.api.analytics?.enabled;
    const isReadOnly = this.api.definitionContext?.origin === 'KUBERNETES';

    // TODO: Backend support pending for COUNT_PER_TIME_WINDOW
    // Determine UI sampling type: if COUNT with "/" value, show as COUNT_PER_TIME_WINDOW
    // Once backend supports it, this will correctly detect and display the saved values
    let samplingType: string = this.api.analytics?.sampling?.type ?? 'COUNT';
    if (samplingType === 'COUNT' && this.api.analytics?.sampling?.value?.includes('/')) {
      samplingType = 'COUNT_PER_TIME_WINDOW';
    }

    this.form = new UntypedFormGroup({
      enabled: new UntypedFormControl({
        value: analyticsEnabled,
        disabled: isReadOnly,
      }),
      requestBody: new UntypedFormControl({
        value: this.api.analytics?.logging?.content?.messagePayload ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      requestHeaders: new UntypedFormControl({
        value: this.api.analytics?.logging?.content?.messageHeaders ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      responseBody: new UntypedFormControl({
        value: this.api.analytics?.logging?.content?.payload ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      responseHeaders: new UntypedFormControl({
        value: this.api.analytics?.logging?.content?.headers ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      samplingType: new UntypedFormControl(
        {
          value: samplingType,
          disabled: !analyticsEnabled || isReadOnly,
        },
        Validators.required,
      ),
      samplingValue: new UntypedFormControl(
        {
          value: this.getSamplingDefaultValue(samplingType),
          disabled: !analyticsEnabled || isReadOnly,
        },
        this.getSamplingValueValidators(samplingType),
      ),
      messageCount: new UntypedFormControl(
        {
          value: this.getMessageCount(samplingType),
          disabled: !analyticsEnabled || isReadOnly,
        },
        [Validators.required, Validators.min(1)],
      ),
      timeWindow: new UntypedFormControl(
        {
          value: this.getTimeWindow(samplingType),
          disabled: !analyticsEnabled || isReadOnly,
        },
        [Validators.required, Validators.min(1)],
      ),
    });

    this.initialFormValue = this.form.getRawValue();
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
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
            .forEach(([_, control]) => {
              control.enable();
            });
        }
      });
  }

  private handleSamplingTypeChanges(): void {
    this.form
      .get('samplingType')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        const samplingValueControl = this.form.get('samplingValue');
        const messageCountControl = this.form.get('messageCount');
        const timeWindowControl = this.form.get('timeWindow');

        if (value !== 'COUNT_PER_TIME_WINDOW') {
          samplingValueControl.setValue(this.getSamplingDefaultValue(value));
          samplingValueControl.setValidators(this.getSamplingValueValidators(value));
          samplingValueControl.updateValueAndValidity();
          samplingValueControl.parent.updateValueAndValidity();
        } else {
          messageCountControl.setValue(this.getMessageCount(value));
          timeWindowControl.setValue(this.getTimeWindow(value));
          messageCountControl.updateValueAndValidity();
          timeWindowControl.updateValueAndValidity();
        }
      });
  }

  private getSamplingValueValidators(samplingType: string): ValidatorFn[] {
    switch (samplingType) {
      case 'PROBABILITY':
        return [Validators.required, Validators.min(0.01), Validators.max(0.5)];
      case 'COUNT':
        return [Validators.required, Validators.min(10)];
      case 'TEMPORAL':
        return [Validators.required];
      default:
        return [];
    }
  }

  private getSamplingDefaultValue(samplingType: string): string | number {
    // If a value is already set for the selected sampling type, use it
    // But only if it's not COUNT_PER_TIME_WINDOW (which uses separate messageCount/timeWindow fields)
    if (
      (this.api?.analytics?.sampling?.type as string) === samplingType &&
      this.api?.analytics?.sampling?.value &&
      samplingType !== 'COUNT_PER_TIME_WINDOW'
    ) {
      return this.api.analytics.sampling.value;
    }

    switch (samplingType) {
      case 'PROBABILITY':
        return '0.01';
      case 'COUNT':
        return '100';
      case 'TEMPORAL':
        return 'PT1S';
      case 'COUNT_PER_TIME_WINDOW':
        return '';
      default:
        return '';
    }
  }

  private getMessageCount(samplingType: string): number {
    // Only parse saved value if it's for COUNT_PER_TIME_WINDOW
    if (
      samplingType === 'COUNT_PER_TIME_WINDOW' &&
      (this.api?.analytics?.sampling?.type as string) === 'COUNT' &&
      this.api?.analytics?.sampling?.value?.includes('/')
    ) {
      const parts = this.api.analytics.sampling.value.split('/');
      return parseInt(parts[0], 10) || 1;
    }
    return 1;
  }

  private getTimeWindow(samplingType: string): number {
    // Only parse saved value if it's for COUNT_PER_TIME_WINDOW
    if (
      samplingType === 'COUNT_PER_TIME_WINDOW' &&
      (this.api?.analytics?.sampling?.type as string) === 'COUNT' &&
      this.api?.analytics?.sampling?.value?.includes('/')
    ) {
      const parts = this.api.analytics.sampling.value.split('/');
      return parseInt(parts[1], 10) || 1;
    }
    return 1;
  }
}
