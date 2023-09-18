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
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { get, isEmpty, isNil } from 'lodash';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { Proxy } from '../../../../entities/management-api-v2';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../../util/apiFilter.operator';
import { RestrictedDomainService } from '../../../../services-ngx/restricted-domain.service';

@Component({
  selector: 'api-proxy-entrypoints',
  template: require('./api-proxy-entrypoints.component.html'),
  styles: [require('./api-proxy-entrypoints.component.scss')],
})
export class ApiProxyEntrypointsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public virtualHostModeEnabled = false;
  public domainRestrictions: string[] = [];

  public apiProxy: Proxy;
  public isReadOnly = false;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly restrictedDomainService: RestrictedDomainService,
    private readonly matDialog: MatDialog,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.ajsStateParams.apiId).pipe(onlyApiV1V2Filter(this.snackBarService)),
      this.restrictedDomainService.get(),
    ])
      .pipe(
        tap(([api, restrictedDomains]) => {
          this.apiProxy = api.proxy;

          this.domainRestrictions = restrictedDomains.map((value) => value.domain) ?? [];

          // virtual host mode is enabled if there are domain restrictions or if there is more than one virtual host or if the first virtual host has a host
          this.virtualHostModeEnabled =
            !isEmpty(this.domainRestrictions) ||
            get(api, 'proxy.virtualHosts', []) > 1 ||
            !isNil(get(api, 'proxy.virtualHosts[0].host', null));

          this.isReadOnly =
            !this.permissionService.hasAnyMatching(['api-definition-u', 'api-gateway_definition-u']) ||
            api.definitionContext?.origin === 'KUBERNETES';
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit(apiProxy: Proxy) {
    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => this.apiService.update(api.id, { ...api, proxy: apiProxy })),
        onlyApiV2Filter(this.snackBarService),
        tap((api) => (this.apiProxy = api.proxy)),
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
          tap((response) => {
            if (response) {
              // Keep only the first virtual_host path
              this.onSubmit({
                ...this.apiProxy,
                virtualHosts: [
                  {
                    path: this.apiProxy.virtualHosts[0].path,
                  },
                ],
              });
              this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
            }
          }),
          takeUntil(this.unsubscribe$),
        )
        .subscribe();
      return;
    }

    this.virtualHostModeEnabled = !this.virtualHostModeEnabled;
  }
}
