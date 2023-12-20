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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { CorsUtil } from '../../../../shared/utils';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../../util/apiFilter.operator';

@Component({
  selector: 'api-proxy-cors',
  templateUrl: './api-proxy-cors.component.html',
  styleUrls: ['./api-proxy-cors.component.scss'],
})
export class ApiProxyCorsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public httpMethods = CorsUtil.httpMethods;
  public defaultHttpHeaders = CorsUtil.defaultHttpHeaders;
  public corsForm: UntypedFormGroup;
  public initialCorsFormValue: unknown;

  private allowAllOriginsConfirmDialog?: MatDialogRef<GioConfirmDialogComponent, boolean>;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV1V2Filter(this.snackBarService),
        tap((api) => {
          const cors = api.proxy?.cors ?? {
            enabled: false,
          };

          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u'])  || api.definitionContext?.origin === 'KUBERNETES' || api.definitionContext?.origin === 'AWS' || api.definitionContext?.origin === 'SOLACE';
          const isCorsDisabled = isReadOnly || !cors.enabled;

          this.corsForm = new UntypedFormGroup({
            enabled: new UntypedFormControl({
              value: cors.enabled,
              disabled: isReadOnly,
            }),
            allowOrigin: new UntypedFormControl(
              {
                value: cors.allowOrigin ?? [],
                disabled: isCorsDisabled,
              },
              [CorsUtil.allowOriginValidator()],
            ),
            allowMethods: new UntypedFormControl({
              value: cors.allowMethods ?? [],
              disabled: isCorsDisabled,
            }),
            allowHeaders: new UntypedFormControl({
              value: cors.allowHeaders ?? [],
              disabled: isCorsDisabled,
            }),
            allowCredentials: new UntypedFormControl({
              value: cors.allowCredentials ?? false,
              disabled: isCorsDisabled,
            }),
            maxAge: new UntypedFormControl({
              value: cors.maxAge ?? -1,
              disabled: isCorsDisabled,
            }),
            exposeHeaders: new UntypedFormControl({
              value: cors.exposeHeaders ?? [],
              disabled: isCorsDisabled,
            }),
            runPolicies: new UntypedFormControl({
              value: cors.runPolicies ?? false,
              disabled: isCorsDisabled,
            }),
          });
          this.initialCorsFormValue = this.corsForm.getRawValue();

          // Disable all Control if enabled is not checked
          const controlKeys = ['allowOrigin', 'allowMethods', 'allowHeaders', 'allowCredentials', 'maxAge', 'exposeHeaders', 'runPolicies'];
          this.corsForm.get('enabled').valueChanges.subscribe((checked) => {
            controlKeys.forEach((k) => {
              return checked ? this.corsForm.get(k).enable() : this.corsForm.get(k).disable();
            });
          });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  confirmAllowAllOrigins(): (tag: string, validationCb: (shouldAddTag: boolean) => void) => void {
    return (tag, validationCb) => {
      // Confirm allow all origins
      if ('*' === tag && !this.allowAllOriginsConfirmDialog) {
        this.allowAllOriginsConfirmDialog = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(
          GioConfirmDialogComponent,
          {
            width: '450px',
            data: {
              title: 'Are you sure?',
              content: 'Do you want to remove all cross-origin restrictions?',
              confirmButton: 'Yes, I want to allow all origins.',
            },
            role: 'alertdialog',
            id: 'allowAllOriginsConfirmDialog',
          },
        );

        this.allowAllOriginsConfirmDialog
          .afterClosed()
          .pipe(takeUntil(this.unsubscribe$))
          .subscribe((shouldAddTag) => {
            this.allowAllOriginsConfirmDialog = null;
            validationCb(shouldAddTag);
          });
      } else {
        validationCb(true);
      }
    };
  }

  onSubmit() {
    const corsFormValue = this.corsForm.getRawValue();

    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) =>
          this.apiService.update(api.id, {
            ...api,
            proxy: {
              ...api.proxy,
              cors: {
                ...api.proxy.cors,

                enabled: corsFormValue.enabled,
                allowOrigin: corsFormValue.allowOrigin,
                allowMethods: corsFormValue.allowMethods,
                allowHeaders: corsFormValue.allowHeaders,
                allowCredentials: corsFormValue.allowCredentials,
                maxAge: corsFormValue.maxAge,
                exposeHeaders: corsFormValue.exposeHeaders,
                runPolicies: corsFormValue.runPolicies,
              },
            },
          }),
        ),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
