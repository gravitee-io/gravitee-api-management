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
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { get, isNil } from 'lodash';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { ApiV1, ApiV2, PathV4, Proxy, UpdateApiV2, VirtualHost } from '../../../entities/management-api-v2';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../util/apiFilter.operator';
import { RestrictedDomainService } from '../../../services-ngx/restricted-domain.service';

@Component({
  selector: 'api-entrypoints',
  templateUrl: './api-entrypoints.component.html',
  styleUrls: ['./api-entrypoints.component.scss'],
  standalone: false,
})
export class ApiEntrypointsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public virtualHostModeEnabled = false;
  public domainRestrictions: string[] = [];
  public formGroup: UntypedFormGroup;
  public pathsFormControl: UntypedFormControl;

  public apiProxy: Proxy;
  public isReadOnly = false;
  public api: ApiV1 | ApiV2;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly restrictedDomainService: RestrictedDomainService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly changeDetector: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.activatedRoute.snapshot.params.apiId).pipe(onlyApiV1V2Filter(this.snackBarService)),
      this.restrictedDomainService.get(),
    ])
      .pipe(
        tap(([api, restrictedDomains]) => {
          this.api = api;
          this.domainRestrictions = restrictedDomains.map(value => value.domain) ?? [];

          this.initForm(api);
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
    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap(api => {
          const formValue: PathV4[] = this.pathsFormControl.value;
          const virtualHosts: VirtualHost[] = formValue.map(p => {
            return { host: p.host, path: p.path, overrideEntrypoint: p.overrideAccess };
          });
          const apiUpdate: UpdateApiV2 = {
            ...(this.api as ApiV2),
            proxy: {
              ...this.api.proxy,
              virtualHosts,
            },
          };

          return this.apiService.update(api.id, apiUpdate);
        }),
        onlyApiV2Filter(this.snackBarService),
        tap(api => (this.apiProxy = api.proxy)),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  switchEntrypointMode() {
    if (this.virtualHostModeEnabled) {
      this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          width: '500px',
          data: {
            title: 'Switch to context-path mode',
            content: `By moving back to context-path you will loose all virtual-hosts. Are you sure to continue?`,
            confirmButton: 'Switch',
          },
          role: 'alertdialog',
          id: 'switchContextPathConfirmDialog',
        })
        .afterClosed()
        .pipe(
          tap(response => {
            if (response) {
              // Keep only the path
              const currentValue = this.formGroup.getRawValue().paths;
              this.formGroup.get('paths').setValue(currentValue.map(({ path }) => ({ path })));
              this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
              this.changeDetector.detectChanges();
            }
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
      return;
    }

    this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
  }

  onReset() {
    this.formGroup.reset();
    this.initForm(this.api);
  }

  private getApiPaths(api: ApiV1 | ApiV2): PathV4[] {
    if (api.proxy.virtualHosts.length > 0) {
      return api.proxy.virtualHosts.map(p => {
        const path: PathV4 = { path: p.path, host: p.host, overrideAccess: p.overrideEntrypoint };
        return path;
      });
    }
    return [{ path: api.contextPath }];
  }

  private initForm(api: ApiV1 | ApiV2) {
    this.apiProxy = api.proxy;
    this.formGroup = new UntypedFormGroup({});

    this.isReadOnly =
      !this.permissionService.hasAllMatching(['api-definition-u', 'api-gateway_definition-u']) ||
      api.definitionContext?.origin === 'KUBERNETES' ||
      api.definitionVersion === 'V1';

    const paths: PathV4[] = this.getApiPaths(api);
    this.pathsFormControl = this.formBuilder.control({ value: paths, disabled: this.isReadOnly }, Validators.required);
    this.formGroup.addControl('paths', this.pathsFormControl);

    // virtual host mode is enabled if there is more than one virtual host or if the first virtual host has a host
    this.virtualHostModeEnabled = !isNil(get(api, 'proxy.virtualHosts[0].host', null));
  }
}
