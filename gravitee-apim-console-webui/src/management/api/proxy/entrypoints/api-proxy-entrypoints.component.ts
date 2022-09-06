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
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api } from '../../../../entities/api';
import { ApiService } from '../../../../services-ngx/api.service';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-proxy-entrypoints',
  template: require('./api-proxy-entrypoints.component.html'),
  styles: [require('./api-proxy-entrypoints.component.scss')],
})
export class ApiProxyEntrypointsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public virtualHostModeEnabled = false;

  public entrypointsForm: FormGroup;
  public initialEntrypointsFormValue: unknown;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => this.initForm(api)),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) =>
          this.apiService.update({ ...api, proxy: { ...api.proxy, virtual_hosts: [{ path: this.entrypointsForm.value.contextPath }] } }),
        ),
        tap((api) => this.initForm(api)),
      )
      .subscribe();
  }

  switchVirtualHostMode() {}

  private initForm(api: Api) {
    this.entrypointsForm = new FormGroup({
      contextPath: new FormControl(
        {
          value: api.proxy.virtual_hosts[0].path,
          disabled: !this.permissionService.hasAnyMatching(['api-definition-u', 'api-gateway_definition-u']),
        },
        [Validators.required, Validators.minLength(3), Validators.pattern(/^\/[/.a-zA-Z0-9-_]+$/)],
      ),
    });
    this.initialEntrypointsFormValue = this.entrypointsForm.getRawValue();
  }
}
