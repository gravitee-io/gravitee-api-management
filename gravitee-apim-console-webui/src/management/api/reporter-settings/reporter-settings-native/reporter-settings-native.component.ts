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
import { tap } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatHint } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV4 } from '../../../../entities/management-api-v2';

type NativeReporterFormType = {
  enabled: FormControl<boolean>;
  tracingEnabled: FormControl<boolean>;
  tracingVerbose: FormControl<boolean>;
};

@Component({
  selector: 'reporter-settings-native',
  templateUrl: './reporter-settings-native.component.html',
  styleUrls: ['./reporter-settings-native.component.scss'],
  imports: [MatCardModule, FormsModule, GioFormSlideToggleModule, GioSaveBarModule, ReactiveFormsModule, MatSlideToggle, MatHint, MatIcon],
})
export class ReporterSettingsNativeComponent implements OnInit {
  reporterSettingsForm: FormGroup<NativeReporterFormType>;
  defaultConfiguration: Record<string, unknown>;
  api: InputSignal<ApiV4> = input.required<ApiV4>();
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    const api = this.api();
    const isReadOnly =
      api.definitionVersion !== 'V4' || api.type !== 'NATIVE' || !this.permissionService.hasAnyMatching(['api-definition-u']);
    const analyticsEnabled = api.analytics?.enabled ?? false;

    this.reporterSettingsForm = new FormGroup<NativeReporterFormType>({
      enabled: new FormControl<boolean>({ value: analyticsEnabled, disabled: isReadOnly }),
      tracingEnabled: new FormControl<boolean>({
        value: api.analytics?.tracing?.enabled ?? false,
        disabled: !analyticsEnabled || isReadOnly,
      }),
      tracingVerbose: new FormControl<boolean>({
        value: api.analytics?.tracing?.verbose ?? false,
        disabled: !analyticsEnabled || !api.analytics?.tracing?.enabled || isReadOnly,
      }),
    });

    this.defaultConfiguration = this.reporterSettingsForm.getRawValue();

    this.reporterSettingsForm
      .get('enabled')!
      .valueChanges.pipe(
        tap((enabled: boolean) => {
          if (enabled) {
            this.reporterSettingsForm.get('tracingEnabled')!.enable();
          } else {
            this.reporterSettingsForm.get('tracingEnabled')!.disable();
            this.reporterSettingsForm.get('tracingVerbose')!.disable();
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.reporterSettingsForm
      .get('tracingEnabled')!
      .valueChanges.pipe(
        tap((tracingEnabled: boolean) => {
          if (tracingEnabled) {
            this.reporterSettingsForm.get('tracingVerbose')!.enable();
          } else {
            this.reporterSettingsForm.get('tracingVerbose')!.disable();
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  submit(): void {
    const values = this.reporterSettingsForm.getRawValue();
    const currentApi = this.api();
    const updatedApi: ApiV4 = {
      ...currentApi,
      analytics: {
        ...(currentApi.analytics ?? {}),
        enabled: values.enabled,
        tracing: {
          enabled: values.tracingEnabled,
          verbose: values.tracingVerbose,
        },
      },
    };

    this.apiService
      .update(updatedApi.id, updatedApi)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.defaultConfiguration = this.reporterSettingsForm.getRawValue();
      });
  }
}
