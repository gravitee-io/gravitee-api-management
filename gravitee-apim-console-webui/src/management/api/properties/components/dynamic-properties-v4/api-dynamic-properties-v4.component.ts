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
import { FormControl, FormGroup } from '@angular/forms';
import { distinctUntilChanged, switchMap, takeUntil, tap } from 'rxjs/operators';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { ActivatedRoute } from '@angular/router';

import { CorsUtil } from '../../../../../shared/utils';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { onlyApiV4Filter } from '../../../../../util/apiFilter.operator';
import { ApiV4 } from '../../../../../entities/management-api-v2';
import { ApiServicePluginsV2Service } from '../../../../../services-ngx/apiservice-plugins-v2.service';

export type DynamicPropertiesType = {
  enabled: boolean;
  configuration: unknown;
};

@Component({
  selector: 'api-dynamic-properties-v4',
  templateUrl: './api-dynamic-properties-v4.component.html',
  styleUrls: ['./api-dynamic-properties-v4.component.scss'],
  standalone: false,
})
export class ApiDynamicPropertiesV4Component implements OnInit, OnDestroy {
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
  public readonly HTTP_DYNAMIC_PROPERTIES = 'http-dynamic-properties';
  public form = new FormGroup({
    enabled: new FormControl(false),
    configuration: new FormControl<unknown>(null),
  });
  public initialFormValue: DynamicPropertiesType;
  public httpMethods = CorsUtil.httpMethods;
  public schema: GioJsonSchema;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiServicePluginsV2Service: ApiServicePluginsV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiV2Service.get(this.activatedRoute.snapshot.params.apiId),
      this.apiServicePluginsV2Service.getApiServicePluginSchema(this.HTTP_DYNAMIC_PROPERTIES),
    ])
      .pipe(
        tap(([api, schema]) => {
          if (api.definitionVersion !== 'V4') {
            throw new Error('Unexpected API type. This page is compatible only for API > V2');
          }
          this.schema = schema;
          const isReadonly = api.definitionContext?.origin === 'KUBERNETES';
          const dynamicProperty = api.services?.dynamicProperty;

          this.form.setValue({
            enabled: dynamicProperty?.enabled ?? false,
            configuration: dynamicProperty?.configuration ?? schema,
          });

          if (isReadonly) {
            this.form.disable({ emitEvent: false });
          }

          if (!dynamicProperty?.enabled) {
            this.form.controls.configuration.disable();
          }

          this.initialFormValue = this.form.getRawValue();

          this.form.controls.enabled.valueChanges.pipe(distinctUntilChanged(), takeUntil(this.unsubscribe$)).subscribe(enabled => {
            if (enabled) {
              this.form.controls.configuration.enable();
            } else {
              this.form.controls.configuration.disable();
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

    this.apiV2Service
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV4Filter(this.snackBarService),
        switchMap((api: ApiV4) => {
          return this.apiV2Service.update(this.activatedRoute.snapshot.params.apiId, {
            ...api,
            services: {
              ...api.services,
              dynamicProperty: {
                enabled: dynamicPropertyFormValue.enabled,
                type: this.HTTP_DYNAMIC_PROPERTIES,
                configuration: this.form.getRawValue().configuration,
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
          this.form.markAsPristine();
          this.initialFormValue = this.form.getRawValue();
        },
      });
  }
}
