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
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map, switchMap, tap } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { MatHint } from '@angular/material/form-field';

import { ApiV4 } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'reporter-settings',
  imports: [MatCardModule, FormsModule, GioFormSlideToggleModule, MatSlideToggle, ReactiveFormsModule, MatHint],
  templateUrl: './reporter-settings.component.html',
  styleUrl: './reporter-settings.component.scss',
})
export class ReporterSettingsComponent implements OnInit {
  reporterSettingsForm: FormGroup<{ enabled: FormControl<boolean> }>;
  private destroyRef: DestroyRef = inject(DestroyRef);
  private api: ApiV4;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        map((params) => params.apiId),
        switchMap((apiId) =>
          this.apiService.get(apiId).pipe(
            tap((response: ApiV4) => {
              this.api = response;
            }),
            map((api: ApiV4) => ({
              value: api.analytics ? api.analytics.enabled : false,
              disabled:
                api.definitionVersion !== 'V4' || api.type !== 'NATIVE' || !this.permissionService.hasAnyMatching(['api-definition-u']),
            })),
          ),
        ),
        tap((formControlState) => {
          this.reporterSettingsForm = new FormGroup({
            enabled: new FormControl<boolean>(formControlState),
          });
        }),
        switchMap(() =>
          this.reporterSettingsForm.get('enabled')!.valueChanges.pipe(
            tap((value) => {
              if (!this.api.analytics) {
                this.api.analytics = { enabled: value };
              } else {
                this.api.analytics.enabled = value;
              }
            }),
            switchMap(() => this.apiService.update(this.api.id, this.api)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
