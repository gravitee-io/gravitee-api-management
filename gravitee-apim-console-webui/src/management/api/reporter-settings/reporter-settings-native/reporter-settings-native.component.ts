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
import { Component, computed, DestroyRef, inject, input, InputSignal, OnInit } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { map, switchMap } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatHint } from '@angular/material/form-field';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV4 } from '../../../../entities/management-api-v2';

@Component({
  selector: 'reporter-settings-native',
  templateUrl: './reporter-settings-native.component.html',
  imports: [MatCardModule, FormsModule, GioFormSlideToggleModule, ReactiveFormsModule, MatSlideToggle, MatHint],
})
export class ReporterSettingsNativeComponent implements OnInit {
  reporterSettingsForm: FormGroup<{ enabled: FormControl<boolean> }>;
  api: InputSignal<ApiV4> = input.required<ApiV4>();
  private destroyRef = inject(DestroyRef);

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    const formControlState = computed(() => {
      const api = this.api();
      return {
        value: api.analytics ? api.analytics.enabled : false,
        disabled: api.definitionVersion !== 'V4' || api.type !== 'NATIVE' || !this.permissionService.hasAnyMatching(['api-definition-u']),
      };
    });

    this.reporterSettingsForm = new FormGroup({
      enabled: new FormControl<boolean>(formControlState()),
    });

    this.reporterSettingsForm
      .get('enabled')!
      .valueChanges.pipe(
        map(value => {
          const currentApi = this.api();
          return {
            ...currentApi,
            analytics: { ...(currentApi.analytics ?? {}), enabled: value },
          };
        }),
        switchMap(updatedApi => this.apiService.update(updatedApi.id, updatedApi)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
