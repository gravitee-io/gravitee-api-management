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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { StateParams } from '@uirouter/angularjs';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4 } from '../../../../entities/management-api-v2';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-runtime-logs-settings',
  template: require('./api-runtime-logs-settings.component.html'),
  styles: [require('./api-runtime-logs-settings.component.scss')],
})
export class ApiRuntimeLogsSettingsComponent implements OnInit, OnDestroy {
  form: FormGroup;
  enabled = false;
  private unsubscribe$: Subject<void> = new Subject<void>();
  private api: ApiV4;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api: ApiV4) => {
          this.api = api;
          this.enabled = api?.analytics?.enabled ?? false;
          this.initForm();
          this.handleEnabledChanges();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  public save(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const formValues = this.form.getRawValue();
          const analytics = this.enabled
            ? {
                enabled: this.enabled,
                logging: {
                  mode: {
                    entrypoint: formValues.entrypoint,
                    endpoint: formValues.endpoint,
                  },
                  phase: {
                    request: formValues.request,
                    response: formValues.response,
                  },
                },
              }
            : { enabled: this.enabled };
          return this.apiService.update(api.id, { ...api, analytics });
        }),
        tap((api: ApiV4) => {
          this.api = api;
        }),
        map(() => {
          this.snackBarService.success(`Runtime logs settings successfully saved!`);
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm() {
    this.form = new FormGroup({
      enabled: new FormControl(this.enabled),
      entrypoint: new FormControl(this.api?.analytics?.logging?.mode?.entrypoint ?? false),
      endpoint: new FormControl(this.api?.analytics?.logging?.mode?.endpoint ?? false),
      request: new FormControl(this.api?.analytics?.logging?.phase?.request ?? false),
      response: new FormControl(this.api?.analytics?.logging?.phase?.response ?? false),
    });
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabled')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.enabled = value;
      });
  }
}
