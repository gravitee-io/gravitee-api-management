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
import { StateParams, StateService } from '@uirouter/angularjs';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Observable, of, Subject } from 'rxjs';

import { PolicyStudioService } from './policy-studio.service';
import { ApiDefinition, toApiDefinition } from './models/ApiDefinition';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiService } from '../../../services-ngx/api.service';
import { Api } from '../../../entities/api';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { GioLicenseService } from '../../../shared/components/gio-license/gio-license.service';

interface MenuItem {
  label: string;
  uiSref: Observable<string>;
  license?: any;
  notAllowed$?: Observable<boolean>;
}

@Component({
  selector: 'gio-policy-studio-layout',
  template: require('./gio-policy-studio-layout.component.html'),
  styles: [require('./gio-policy-studio-layout.component.scss')],
})
export class GioPolicyStudioLayoutComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();
  policyStudioMenu: MenuItem[] = [
    { label: 'Design', uiSref: of('.design') },
    { label: 'Configuration', uiSref: of('.config') },
  ];
  activeLink = this.policyStudioMenu[0];
  apiDefinition: ApiDefinition;
  isDirty: boolean;

  constructor(
    readonly policyStudioService: PolicyStudioService,
    readonly gioLicenseService: GioLicenseService,
    readonly snackBarService: SnackBarService,
    readonly apiService: ApiService,
    readonly permissionService: GioPermissionService,
    @Inject(UIRouterState) readonly ajsStateService: StateService,
    @Inject(UIRouterStateParams) readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    const debugLicense = { feature: 'apim-debug-mode', utmMedium: 'feature_debugmode_v2' };
    const notAllowed$ = this.gioLicenseService.notAllowed(debugLicense.feature);
    this.policyStudioMenu.push({
      label: 'Debug',
      uiSref: notAllowed$.pipe(map((notAllowed) => (notAllowed ? null : '.debug'))),
      license: debugLicense,
      notAllowed$,
    });

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        tap((api) => {
          this.policyStudioService.setApiDefinition(this.toApiDefinition(api));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
    this.policyStudioService.getApiDefinitionToSave$().pipe(takeUntil(this.unsubscribe$)).subscribe(this.onDefinitionChange.bind(this));
  }

  onDefinitionChange(apiDefinition: ApiDefinition) {
    this.isDirty = true;
    this.apiDefinition = apiDefinition;
  }

  onSubmit() {
    return this.apiService
      .get(this.apiDefinition.id)
      .pipe(
        switchMap((api) => this.apiService.update({ ...api, ...this.apiDefinition })),
        tap((api) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.policyStudioService.setApiDefinition(this.toApiDefinition(api));
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onReset() {
    this.policyStudioService.reset();
    this.ajsStateService.reload();
  }

  private toApiDefinition(api: Api): ApiDefinition {
    const apiDefinition = toApiDefinition(api);
    const plans = api.plans && this.permissionService.hasAnyMatching(['api-plan-r', 'api-plan-u']) ? api.plans : [];
    return { ...apiDefinition, plans };
  }

  ngOnDestroy(): void {
    this.policyStudioService.reset();
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
