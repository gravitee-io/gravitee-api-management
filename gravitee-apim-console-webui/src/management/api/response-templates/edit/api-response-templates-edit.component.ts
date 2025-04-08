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
import { AbstractControl, UntypedFormControl, UntypedFormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Header } from '@gravitee/ui-particles-angular';
import { isEmpty, isNil, toString } from 'lodash';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, map, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { HttpUtil, StatusCode } from '../../../../shared/utils';
import { fromResponseTemplates, ResponseTemplate, toResponseTemplates } from '../response-templates.adapter';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { onlyApiV2V4Filter } from '../../../../util/apiFilter.operator';
import { gatewayErrorKeys } from '../../../../entities/gateway-error-keys/GatewayErrorKeys';

@Component({
  selector: 'api-proxy-response-templates-edit',
  templateUrl: './api-response-templates-edit.component.html',
  styleUrls: ['./api-response-templates-edit.component.scss'],
  standalone: false,
})
export class ApiResponseTemplatesEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public defaultKeys = ['DEFAULT', ...gatewayErrorKeys] as const;

  public apiId: string;
  public responseTemplateToEdit?: ResponseTemplate;
  public responseTemplatesForm: UntypedFormGroup;
  public initialResponseTemplatesFormValue: unknown;
  public isReadOnly = false;
  public mode: 'new' | 'edit' = 'new';

  public filteredStatusCodes$: Observable<StatusCode[]>;
  public selectedStatusCodes$: Observable<StatusCode>;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly route: Router,
    private readonly apiService: ApiV2Service,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        tap((api) => {
          this.apiId = api.id;

          const responseTemplates = toResponseTemplates(api.responseTemplates);

          this.responseTemplateToEdit = responseTemplates.find((rt) => rt.id === this.activatedRoute.snapshot.params.responseTemplateId);
          this.mode = !isNil(this.responseTemplateToEdit) ? 'edit' : 'new';

          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-response_templates-u']) || api.definitionContext?.origin === 'KUBERNETES';

          this.responseTemplatesForm = new UntypedFormGroup({
            key: new UntypedFormControl(
              {
                value: this.responseTemplateToEdit?.key ?? '',
                disabled: this.isReadOnly,
              },
              [Validators.required, checkAcceptHeader()],
            ),
            acceptHeader: new UntypedFormControl(
              {
                value: this.responseTemplateToEdit?.contentType ?? '*/*',
                disabled: this.isReadOnly,
              },
              [Validators.required, uniqAcceptHeaderValidator(responseTemplates, this.responseTemplateToEdit)],
            ),
            statusCode: new UntypedFormControl(
              {
                value: toString(this.responseTemplateToEdit?.statusCode ?? 400),
                disabled: this.isReadOnly,
              },
              [Validators.required, HttpUtil.statusCodeValidator()],
            ),
            headers: new UntypedFormControl({
              value: Object.entries(this.responseTemplateToEdit?.headers ?? {}).map(([key, value]) => ({ key, value })),
              disabled: this.isReadOnly,
            }),
            body: new UntypedFormControl({
              value: this.responseTemplateToEdit?.body ?? '',
              disabled: this.isReadOnly,
            }),
            propagateErrorKeyToLogs: new UntypedFormControl({
              value: this.responseTemplateToEdit?.propagateErrorKeyToLogs ?? false,
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
            startWith(this.responseTemplatesForm.get('statusCode')?.value),
            map((value) => HttpUtil.statusCodes.find((statusCode) => toString(statusCode.code) === toString(value))),

            takeUntil(this.unsubscribe$),
          );
        }),
        takeUntil(this.unsubscribe$),
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
      propagateErrorKeyToLogs: responseTemplateFormValue.propagateErrorKeyToLogs,
    };

    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2V4Filter(this.snackBarService),
        switchMap((api) => {
          const responseTemplates = toResponseTemplates(api.responseTemplates);

          // Find the response template to update or add the new one
          const responseTemplateToEditIndex =
            this.mode === 'edit' ? responseTemplates.findIndex((rt) => rt.id === this.responseTemplateToEdit?.id) : -1;

          responseTemplateToEditIndex !== -1
            ? responseTemplates.splice(responseTemplateToEditIndex, 1, responseTemplateToSave)
            : responseTemplates.push(responseTemplateToSave);

          return this.apiService.update(api.id, {
            ...api,
            responseTemplates: fromResponseTemplates(responseTemplates),
          });
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() =>
          this.route.navigate(['../'], {
            relativeTo: this.activatedRoute,
          }),
        ),
        takeUntil(this.unsubscribe$),
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
