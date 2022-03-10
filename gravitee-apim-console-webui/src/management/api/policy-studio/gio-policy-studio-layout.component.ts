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
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { PolicyStudioService } from './policy-studio.service';
import { PolicyStudioPropertiesService } from './properties/policy-studio-properties.service';
import { ApiDefinition, toApiDefinition } from './models/ApiDefinition';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { ApiService } from '../../../services-ngx/api.service';
import { Api } from '../../../entities/api';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'gio-policy-studio-layout',
  template: require('./gio-policy-studio-layout.component.html'),
  styles: [require('./gio-policy-studio-layout.component.scss')],
})
export class GioPolicyStudioLayoutComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();
  policyStudioMenu = [
    { label: 'Design', uiSref: 'design' },
    { label: 'Configuration', uiSref: 'config' },
    { label: 'Properties', uiSref: 'properties' },
    { label: 'Resources', uiSref: 'resources' },
    { label: 'Debug', uiSref: 'debug' },
  ];
  activeLink = this.policyStudioMenu[0];
  apiDefinition: ApiDefinition;
  isDirty: boolean;

  constructor(
    readonly policyStudioService: PolicyStudioService,
    readonly policyStudioPropertiesService: PolicyStudioPropertiesService,
    readonly snackBarService: SnackBarService,
    readonly apiService: ApiService,
    readonly permissionService: GioPermissionService,
    @Inject(UIRouterState) readonly ajsStateService: StateService,
    @Inject(UIRouterStateParams) readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((api) => {
          this.policyStudioService.emitApiDefinition(this.toApiDefinition(api));
        }),
      )
      .subscribe();
    this.policyStudioService.getApiDefinition$().pipe(takeUntil(this.unsubscribe$)).subscribe(this.onDefinitionChange.bind(this));
  }

  onDefinitionChange(apiDefinition: ApiDefinition) {
    if (this.apiDefinition) {
      this.isDirty = true;
    }
    this.apiDefinition = apiDefinition;
  }

  onSubmit() {
    return this.apiService
      .get(this.apiDefinition.id)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => this.apiService.update({ ...api, ...this.apiDefinition })),
        tap((api) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.policyStudioService.emitApiDefinition(this.toApiDefinition(api));
          this.isDirty = false;
        }),
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
