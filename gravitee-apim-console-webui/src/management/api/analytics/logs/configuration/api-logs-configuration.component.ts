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
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';
import { ActivatedRoute } from '@angular/router';

import { CONTENT_MODES, DEFAULT_LOGGING, LOGGING_MODES, SCOPE_MODES } from './api-logs-configuration';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV1, ApiV2 } from '../../../../../entities/management-api-v2';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../../../util/apiFilter.operator';

interface LoggingConfiguration {
  enabled: boolean;
  mode: 'CLIENT' | 'PROXY' | 'CLIENT_PROXY' | 'NONE';
  content: 'NONE' | 'HEADERS' | 'PAYLOADS' | 'HEADERS_PAYLOADS';
  scope: 'NONE' | 'REQUEST' | 'RESPONSE' | 'REQUEST_RESPONSE';
  condition: string;
}

@Component({
  selector: 'api-log-configuration',
  templateUrl: './api-logs-configuration.component.html',
  styleUrls: ['./api-logs-configuration.component.scss'],
  standalone: false,
})
export class ApiLogsConfigurationComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private api: ApiV1 | ApiV2;
  private defaultLogging = DEFAULT_LOGGING;
  private defaultConfiguration: LoggingConfiguration;

  public logsConfigurationForm: UntypedFormGroup;
  public mode: UntypedFormControl;
  public scope: UntypedFormControl;
  public content: UntypedFormControl;
  public loggingModes = LOGGING_MODES;
  public contentModes = CONTENT_MODES;
  public scopeModes = SCOPE_MODES;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public ngOnInit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV1V2Filter(this.snackBarService),
        tap((api) => {
          this.api = api;
          this.initForm(api);
          this.cdr.detectChanges();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public onEnabledChanges(): void {
    this.logsConfigurationForm
      .get('enabled')
      .valueChanges.pipe(
        tap((enabled) => {
          if (enabled === null || enabled === true) {
            this.logsConfigurationForm.get('mode').enable();
            this.logsConfigurationForm.get('content').enable();
            this.logsConfigurationForm.get('scope').enable();
            this.logsConfigurationForm.get('condition').enable();
          } else {
            this.logsConfigurationForm.get('mode').patchValue('NONE');
            this.logsConfigurationForm.get('content').patchValue('NONE');
            this.logsConfigurationForm.get('scope').patchValue('NONE');
            this.logsConfigurationForm.get('condition').patchValue('');
            this.logsConfigurationForm.get('mode').disable();
            this.logsConfigurationForm.get('content').disable();
            this.logsConfigurationForm.get('scope').disable();
            this.logsConfigurationForm.get('condition').disable();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public onSubmit() {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          const configurationValues = this.logsConfigurationForm.getRawValue();
          const updatedApi = {
            ...api,
            proxy: {
              ...api.proxy,
              logging: {
                ...api.proxy.logging,
                scope: configurationValues.scope,
                content: configurationValues.content,
                mode: configurationValues.enabled ? configurationValues.mode : 'NONE',
                condition: configurationValues.condition,
              },
            },
          };

          if (isEmpty(configurationValues.condition?.trim())) {
            delete updatedApi.proxy.logging.condition;
          }

          return this.apiService.update(api.id, updatedApi);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initForm(api: ApiV2 | ApiV1) {
    const { mode, content, scope } = { ...this.defaultLogging, ...this.api.proxy.logging };
    const isReadOnly = !this.permissionService.hasAnyMatching(['api-log-u']) || api.definitionContext?.origin === 'KUBERNETES';
    const enabled = !!api.proxy.logging && api.proxy.logging.mode !== 'NONE';
    this.mode = new UntypedFormControl({
      value: mode !== 'NONE' ? mode : 'CLIENT_PROXY',
      disabled: !enabled || isReadOnly,
    });
    this.content = new UntypedFormControl({
      value: content !== 'NONE' ? content : 'HEADERS_PAYLOADS',
      disabled: !enabled || isReadOnly,
    });
    this.scope = new UntypedFormControl({
      value: scope !== 'NONE' ? scope : 'REQUEST_RESPONSE',
      disabled: !enabled || isReadOnly,
    });
    this.logsConfigurationForm = new UntypedFormGroup({
      enabled: new UntypedFormControl({ value: enabled, disabled: isReadOnly }),
      condition: new UntypedFormControl({ value: api.proxy.logging?.condition, disabled: !enabled || isReadOnly }),
      mode: this.mode,
      content: this.content,
      scope: this.scope,
    });
    this.defaultConfiguration = this.logsConfigurationForm.getRawValue();

    this.onEnabledChanges();
  }
}
