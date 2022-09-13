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
import { StateService } from '@uirouter/core';
import { merge } from 'lodash';
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-proxy-response-templates-edit',
  template: require('./api-proxy-response-templates-edit.component.html'),
  styles: [require('./api-proxy-response-templates-edit.component.scss')],
})
export class ApiProxyResponseTemplatesEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public defaultKeys = [
    'DEFAULT',
    'API_KEY_MISSING',
    'API_KEY_INVALID',
    'QUOTA_TOO_MANY_REQUESTS',
    'RATE_LIMIT_TOO_MANY_REQUESTS',
    'REQUEST_CONTENT_LIMIT_TOO_LARGE',
    'REQUEST_CONTENT_LIMIT_LENGTH_REQUIRED',
    'REQUEST_TIMEOUT',
    'REQUEST_VALIDATION_INVALID',
    'RESOURCE_FILTERING_FORBIDDEN',
    'RESOURCE_FILTERING_METHOD_NOT_ALLOWED',
    'RBAC_FORBIDDEN',
    'RBAC_INVALID_USER_ROLES',
    'RBAC_NO_USER_ROLE',
    'OAUTH2_MISSING_SERVER',
    'OAUTH2_MISSING_HEADER',
    'OAUTH2_MISSING_ACCESS_TOKEN',
    'OAUTH2_INVALID_ACCESS_TOKEN',
    'OAUTH2_INVALID_SERVER_RESPONSE',
    'OAUTH2_INSUFFICIENT_SCOPE',
    'OAUTH2_SERVER_UNAVAILABLE',
    'HTTP_SIGNATURE_INVALID_SIGNATURE',
    'JWT_MISSING_TOKEN',
    'JWT_INVALID_TOKEN',
    'JSON_INVALID_PAYLOAD',
    'JSON_INVALID_FORMAT',
    'JSON_INVALID_RESPONSE_PAYLOAD',
    'JSON_INVALID_RESPONSE_FORMAT',
    'GATEWAY_INVALID_REQUEST',
    'GATEWAY_INVALID_RESPONSE',
    'GATEWAY_OAUTH2_ACCESS_DENIED',
    'GATEWAY_OAUTH2_SERVER_ERROR',
    'GATEWAY_OAUTH2_INVALID_CLIENT',
    'GATEWAY_MISSING_SECURITY_PROVIDER',
    'GATEWAY_POLICY_INTERNAL_ERROR',
    'GATEWAY_PLAN_UNRESOLVABLE',
    'GATEWAY_CLIENT_CONNECTION_ERROR',
    'GATEWAY_CLIENT_CONNECTION_TIMEOUT',
  ];

  public apiId: string;
  public responseTemplatesForm: FormGroup;
  public initialResponseTemplatesFormValue: unknown;
  public isReadOnly = false;
  public mode: 'new' | 'edit' = 'new';

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.apiId = api.id;
          this.mode = this.ajsStateParams.key ? 'edit' : 'new'; // TODO : for next commit

          this.isReadOnly = !this.permissionService.hasAnyMatching(['api-response_templates-u']);

          this.responseTemplatesForm = new FormGroup({
            key: new FormControl({
              value: '',
              disabled: this.isReadOnly,
            }),
            acceptHeader: new FormControl({
              value: '*/*',
              disabled: this.isReadOnly,
            }),
            status: new FormControl({
              value: 200, // TODO : for next commit
              disabled: this.isReadOnly,
            }),
          });
          this.initialResponseTemplatesFormValue = this.responseTemplatesForm.getRawValue();
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const responseTemplate = this.responseTemplatesForm.getRawValue();
    const responseTemplateToMerge = { [responseTemplate.key]: { [responseTemplate.acceptHeader]: { status: responseTemplate.status } } };

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) =>
          this.apiService.update({
            ...api,
            response_templates: merge(api.response_templates, responseTemplateToMerge),
          }),
        ),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ajsState.go('management.apis.detail.proxy.ng-responsetemplates', { apiId: this.apiId })),
      )
      .subscribe();
  }
}
