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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { CorsUtil } from '../../../shared/utils';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiV4, Cors, HttpListener } from '../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../services-ngx/connector-plugins-v2.service';

@Component({
  selector: 'api-cors',
  templateUrl: './api-cors.component.html',
  styleUrls: ['./api-cors.component.scss'],
})
export class ApiCorsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public httpMethods = CorsUtil.httpMethods.map((e) => e);
  public defaultHttpHeaders = CorsUtil.defaultHttpHeaders.map((e) => e);
  public corsForm: UntypedFormGroup;
  public initialCorsFormValue: unknown;
  public hasEntrypointsSupportingCors = false;
  public entrypointsNameSupportingCors: string;

  private allowAllOriginsConfirmDialog?: MatDialogRef<GioConfirmDialogComponent, boolean>;

  constructor(
    public readonly activatedRoute: ActivatedRoute,
    private readonly matDialog: MatDialog,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
  ) {}

  ngOnInit(): void {
    combineLatest([this.apiService.get(this.activatedRoute.snapshot.params.apiId), this.connectorPluginsV2Service.listEntrypointPlugins()])
      .pipe(
        tap(([api, entrypoints]) => {
          let cors: Cors;
          if (api.definitionVersion === 'V4') {
            cors = api.listeners
              .filter((listener) => listener.type === 'HTTP')
              .map((listener) => listener as HttpListener)
              .map((httpListener) => httpListener.cors)[0] ?? { enabled: false };

            const entrypointsSupportingCors = entrypoints.filter(
              (entrypoint) => entrypoint.availableFeatures.find((feature) => feature === 'CORS') != null,
            );

            this.entrypointsNameSupportingCors = entrypointsSupportingCors.map((entrypoint) => entrypoint.name).join(', ');

            this.hasEntrypointsSupportingCors = this.hasEntrypointSupportingCors(
              api,
              entrypointsSupportingCors.map((entrypoint) => entrypoint.id),
            );
          } else if (api.definitionVersion !== 'FEDERATED') {
            this.hasEntrypointsSupportingCors = true;
            cors = api.proxy?.cors ?? {
              enabled: false,
            };
          }

          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definitionContext?.origin === 'KUBERNETES';
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
        switchMap((api) => {
          let apiToUpdate;
          if (api.definitionVersion === 'V1') {
            this.snackBarService.error('API V1 are deprecated. Please upgrade your API to V2.');
          } else if (api.definitionVersion === 'V2') {
            apiToUpdate = {
              ...api,
              definitionVersion: 'V2',
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
            };
          } else {
            apiToUpdate = api;
            apiToUpdate.listeners
              .filter((listener) => listener.type === 'HTTP')
              .forEach((listener) => {
                listener.cors = {
                  ...(listener as HttpListener).cors,
                  enabled: corsFormValue.enabled,
                  allowOrigin: corsFormValue.allowOrigin,
                  allowMethods: corsFormValue.allowMethods,
                  allowHeaders: corsFormValue.allowHeaders,
                  allowCredentials: corsFormValue.allowCredentials,
                  maxAge: corsFormValue.maxAge,
                  exposeHeaders: corsFormValue.exposeHeaders,
                  runPolicies: corsFormValue.runPolicies,
                };
              });
          }
          return this.apiService.update(api.id, apiToUpdate);
        }),
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

  private hasEntrypointSupportingCors(api: ApiV4, entrypointsIdSupportingCors: string[]) {
    return api.listeners
      .filter((listener) => listener.type === 'HTTP')
      .flatMap((listener) => listener.entrypoints)
      .some((e) => entrypointsIdSupportingCors.includes(e.type));
  }
}
