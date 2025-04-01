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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { combineLatest, Subject } from 'rxjs';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { MonacoEditorLanguageConfig } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { CorsUtil } from '../../../../../shared/utils';
import { ApiV2 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { onlyApiV2Filter } from '../../../../../util/apiFilter.operator';

@Component({
  selector: 'api-dynamic-properties',
  templateUrl: './api-dynamic-properties.component.html',
  styleUrls: ['./api-dynamic-properties.component.scss'],
  standalone: false,
})
export class ApiDynamicPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public transformationJOLTExample = `[
  {
    "key": 1,
      "value": "https://north-europe.company.com/"
  },
  {
    "key": 2,
    "value": "https://north-europe.company.com/"
  },
  {
    "key": 3,
    "value": "https://south-asia.company.com/"
  }
]`;
  private specificationDefaultValue = '[\n    {\n        "operation":"default",\n        "spec":{}\n    }\n]';
  public specificationLanguageConfig: MonacoEditorLanguageConfig = { language: 'json', schemas: [] };

  public form: UntypedFormGroup;
  public initialFormValue: unknown;

  public httpMethods = CorsUtil.httpMethods;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([this.apiService.get(this.activatedRoute.snapshot.params.apiId)])
      .pipe(
        tap(([api]) => {
          if (api.definitionVersion !== 'V2') {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
          const isReadonly = api.definitionContext?.origin === 'KUBERNETES';
          const dynamicProperty = api.services?.dynamicProperty;

          this.form = new UntypedFormGroup({
            enabled: new UntypedFormControl({
              value: dynamicProperty?.enabled ?? false,
              disabled: isReadonly,
            }),
            schedule: new UntypedFormControl(
              {
                value: dynamicProperty?.schedule ?? '0 */5 * * * *',
                disabled: isReadonly,
              },
              [Validators.required],
            ),
            provider: new UntypedFormControl({
              value: dynamicProperty?.provider ?? 'HTTP', // Only http is supported for now.
              disabled: isReadonly,
            }),
            method: new UntypedFormControl({
              value: dynamicProperty?.configuration?.method ?? 'GET',
              disabled: isReadonly,
            }),
            url: new UntypedFormControl(
              {
                value: dynamicProperty?.configuration?.url ?? '',
                disabled: isReadonly,
              },
              [Validators.required],
            ),
            headers: new UntypedFormControl({
              value: dynamicProperty?.configuration?.headers ?? undefined,
              disabled: isReadonly,
            }),
            useSystemProxy: new UntypedFormControl({
              value: dynamicProperty?.configuration?.useSystemProxy ?? undefined,
              disabled: isReadonly,
            }),
            body: new UntypedFormControl({
              value: dynamicProperty?.configuration?.body ?? '',
              disabled: isReadonly,
            }),
            specification: new UntypedFormControl({
              value: dynamicProperty?.configuration?.specification ?? this.specificationDefaultValue,
              disabled: isReadonly,
            }),
          });
          this.initialFormValue = this.form.value;

          this.form
            .get('enabled')
            .valueChanges.pipe(startWith(this.form.get('enabled').value), takeUntil(this.unsubscribe$))
            .subscribe((enabled) => {
              const controlNames = ['schedule', 'provider', 'method', 'url', 'headers', 'useSystemProxy', 'body', 'specification'];

              if (enabled) {
                controlNames.forEach((controlName) => {
                  this.form.get(controlName).enable({ emitEvent: false });
                });
              } else {
                controlNames.forEach((controlName) => {
                  this.form.get(controlName).disable({ emitEvent: false });
                });
              }
            });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onSave() {
    const dynamicPropertyFormValue = this.form.getRawValue();

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api: ApiV2) => {
          return this.apiService.update(this.activatedRoute.snapshot.params.apiId, {
            ...api,
            services: {
              ...api.services,
              dynamicProperty: {
                enabled: dynamicPropertyFormValue.enabled,
                provider: 'HTTP',
                schedule: dynamicPropertyFormValue.schedule,
                configuration: {
                  method: dynamicPropertyFormValue.method,
                  url: dynamicPropertyFormValue.url,
                  headers: dynamicPropertyFormValue.headers,
                  useSystemProxy: dynamicPropertyFormValue.useSystemProxy,
                  body: dynamicPropertyFormValue.body,
                  specification: dynamicPropertyFormValue.specification,
                },
              },
            },
          });
        }),
      )
      .subscribe({
        error: ({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred while updating dynamic properties.');
        },
        next: () => {
          this.snackBarService.success('Dynamic properties updated.');
          this.ngOnInit();
        },
      });
  }
}
