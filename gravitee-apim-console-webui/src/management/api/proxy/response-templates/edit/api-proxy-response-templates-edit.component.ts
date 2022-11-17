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
import { AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Header } from '@gravitee/ui-particles-angular';
import { StateService } from '@uirouter/core';
import { isEmpty, isNil, toString } from 'lodash';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, map, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams, UIRouterState } from '../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { HttpUtil, StatusCode } from '../../../../../shared/utils';
import { fromResponseTemplates, ResponseTemplate, toResponseTemplates } from '../response-templates.adapter';

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
  public responseTemplateToEdit?: ResponseTemplate;
  public responseTemplatesForm: FormGroup;
  public initialResponseTemplatesFormValue: unknown;
  public isReadOnly = false;
  public mode: 'new' | 'edit' = 'new';

  public filteredStatusCodes$: Observable<StatusCode[]>;
  public selectedStatusCodes$: Observable<StatusCode>;

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

          const responseTemplates = toResponseTemplates(api.response_templates);

          this.responseTemplateToEdit = responseTemplates.find((rt) => rt.id === this.ajsStateParams.responseTemplateId);
          this.mode = !isNil(this.responseTemplateToEdit) ? 'edit' : 'new';

          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-response_templates-u']) || api.definition_context?.origin === 'kubernetes';

          this.responseTemplatesForm = new FormGroup({
            key: new FormControl(
              {
                value: this.responseTemplateToEdit?.key ?? '',
                disabled: this.isReadOnly,
              },
              [Validators.required, checkAcceptHeader()],
            ),
            acceptHeader: new FormControl(
              {
                value: this.responseTemplateToEdit?.contentType ?? '*/*',
                disabled: this.isReadOnly,
              },
              [Validators.required, uniqAcceptHeaderValidator(responseTemplates, this.responseTemplateToEdit)],
            ),
            statusCode: new FormControl(
              {
                value: toString(this.responseTemplateToEdit?.statusCode ?? 400),
                disabled: this.isReadOnly,
              },
              [Validators.required, HttpUtil.statusCodeValidator()],
            ),
            headers: new FormControl({
              value: Object.entries(this.responseTemplateToEdit?.headers ?? {}).map(([key, value]) => ({ key, value })),
              disabled: this.isReadOnly,
            }),
            body: new FormControl({
              value: this.responseTemplateToEdit?.body ?? '',
              disabled: this.isReadOnly,
            }),
          });
          this.initialResponseTemplatesFormValue = this.responseTemplatesForm.getRawValue();

          this.filteredStatusCodes$ = this.responseTemplatesForm.get('statusCode')?.valueChanges.pipe(
            startWith(''),
            map((value) => {
              if (isEmpty(value)) {
                return HttpUtil.statusCodes;
              }

              return HttpUtil.statusCodes.filter(
                (statusCode) =>
                  toString(statusCode.code).includes(toString(value)) ||
                  statusCode.label.toLowerCase().includes(toString(value).toLowerCase()),
              );
            }),
          );
          this.selectedStatusCodes$ = this.responseTemplatesForm.get('statusCode')?.valueChanges.pipe(
            takeUntil(this.unsubscribe$),
            startWith(this.responseTemplatesForm.get('statusCode')?.value),
            map((value) => HttpUtil.statusCodes.find((statusCode) => toString(statusCode.code) === toString(value))),
          );
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const responseTemplateFormValue = this.responseTemplatesForm.getRawValue();
    const headers = responseTemplateFormValue.headers as Header[] | undefined;

    const responseTemplateToSave: ResponseTemplate = {
      id: this.responseTemplateToEdit?.id,
      key: responseTemplateFormValue.key,
      contentType: responseTemplateFormValue.acceptHeader,
      statusCode: parseInt(responseTemplateFormValue.statusCode, 10),
      headers: !isEmpty(headers) ? Object.fromEntries(headers.map((h) => [h.key, h.value])) : undefined,
      body: responseTemplateFormValue.body,
    };

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          const responseTemplates = toResponseTemplates(api.response_templates);

          // Find the response template to update or add the new one
          const responseTemplateToEditIndex =
            this.mode === 'edit' ? responseTemplates.findIndex((rt) => rt.id === this.responseTemplateToEdit?.id) : -1;

          responseTemplateToEditIndex !== -1
            ? responseTemplates.splice(responseTemplateToEditIndex, 1, responseTemplateToSave)
            : responseTemplates.push(responseTemplateToSave);

          return this.apiService.update({
            ...api,
            response_templates: fromResponseTemplates(responseTemplates),
          });
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ajsState.go('management.apis.detail.proxy.responsetemplates.list', { apiId: this.apiId })),
      )
      .subscribe();
  }
}

// Template Key and Accept Header are unique
const uniqAcceptHeaderValidator = (responseTemplate: ResponseTemplate[], editingResponseTemplate: ResponseTemplate): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (isEmpty(value)) {
      // not validate if is empty. Required validator will do the job
      return null;
    }

    if (!control.parent) {
      return null;
    }

    const keyControl = control.parent.get('key');

    if (
      !responseTemplate
        // ignore the response template we are editing
        .filter((rt) => editingResponseTemplate?.id !== rt.id)
        .some((rt) => rt.key === keyControl.value && rt.contentType === value)
    ) {
      return null;
    }

    return { uniqAcceptHeader: `Response template with key '${keyControl.value}' and accept header '${value}' already exists.` };
  };
};

// Force uniqAcceptHeaderValidator to be called when key is changed
const checkAcceptHeader = (): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.parent) {
      return null;
    }

    const acceptHeaderControl = control.parent.get('acceptHeader');
    acceptHeaderControl.updateValueAndValidity();
    acceptHeaderControl.markAsTouched();

    return null;
  };
};
