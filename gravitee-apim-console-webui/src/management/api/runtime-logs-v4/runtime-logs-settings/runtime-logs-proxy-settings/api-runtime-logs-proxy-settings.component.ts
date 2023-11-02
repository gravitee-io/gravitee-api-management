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
import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { EMPTY, merge, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

type DefaultConfiguration = {
  entrypoint: boolean;
  endpoint: boolean;
  request: boolean;
  response: boolean;
  headers: boolean;
  payload: boolean;
  condition: string;
};

@Component({
  selector: 'api-runtime-logs-proxy-settings',
  template: require('./api-runtime-logs-proxy-settings.component.html'),
  styles: [require('./api-runtime-logs-proxy-settings.component.scss')],
})
export class ApiRuntimeLogsProxySettingsComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  form: FormGroup;
  defaultConfiguration: DefaultConfiguration;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api: ApiV4) => {
          this.initForm(api);
          this.onEnabledChanges();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  discard() {
    if (!this.defaultConfiguration.endpoint && !this.defaultConfiguration.entrypoint) {
      this.clearAndDisableFormFields();
    }
  }

  submit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const configurationValues = this.form.getRawValue();
          const updatedApi = {
            ...api,
            analytics: {
              ...api.analytics,
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
            },
          };

          return this.apiService.update(api.id, updatedApi);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => {
          this.defaultConfiguration = this.form.getRawValue();
          this.form.reset(this.defaultConfiguration, { emitEvent: false });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm(api: ApiV4) {
    const enabled = api.analytics?.logging?.mode?.entrypoint || api.analytics?.logging?.mode?.endpoint;
    const isReadOnly = api.definitionContext?.origin === 'KUBERNETES';
    this.form = new FormGroup({
      entrypoint: new FormControl({ value: api.analytics?.logging?.mode?.entrypoint, disabled: isReadOnly }),
      endpoint: new FormControl({ value: api.analytics?.logging?.mode?.endpoint, disabled: isReadOnly }),
      request: new FormControl({ value: api.analytics?.logging?.phase?.request, disabled: !enabled || isReadOnly }),
      response: new FormControl({ value: api.analytics?.logging?.phase?.response, disabled: !enabled || isReadOnly }),
      headers: new FormControl({ value: api.analytics?.logging?.content?.headers, disabled: !enabled || isReadOnly }),
      payload: new FormControl({ value: api.analytics?.logging?.content?.payload, disabled: !enabled || isReadOnly }),
      condition: new FormControl({ value: api.analytics?.logging?.condition, disabled: !enabled || isReadOnly }),
    });

    this.defaultConfiguration = this.form.getRawValue();
  }

  private onEnabledChanges(): void {
    merge(this.form.get('entrypoint').valueChanges, this.form.get('endpoint').valueChanges)
      .pipe(
        tap(() => {
          if (this.form.get('entrypoint').value || this.form.get('endpoint').value) {
            this.enableFormFields();
          } else {
            this.clearAndDisableFormFields();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private enableFormFields() {
    this.form.get('request').enable();
    this.form.get('response').enable();
    this.form.get('headers').enable();
    this.form.get('payload').enable();
    this.form.get('condition').enable();
  }

  private clearAndDisableFormFields() {
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
