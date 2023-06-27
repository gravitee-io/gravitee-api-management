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
import { combineLatest, Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-resources';
import { StateParams } from '@uirouter/angularjs';

import { ApiResourcesService } from './api-resources.service';

import { ResourceListItem } from '../../../../entities/resource/resourceListItem';
import { ApiService } from '../../../../services-ngx/api.service';
import { AjsRootScope, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api, UpdateApi } from '../../../../entities/api';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiDefinition, toApiDefinition } from '../../policy-studio/models/ApiDefinition';

@Component({
  selector: 'api-resources',
  template: require('./api-resources.component.html'),
  styles: [require('./api-resources.component.scss')],
})
export class ApiResourcesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public apiDefinition: ApiDefinition;
  public initialApiDefinition: ApiDefinition;

  public isReadonly = false;
  public isDirty = false;

  public resourceTypes: ResourceListItem[];

  constructor(
    private readonly apiService: ApiService,
    private readonly apiResourcesService: ApiResourcesService,
    private readonly permissionService: GioPermissionService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.ajsStateParams.apiId),
      this.apiResourcesService.listResources({ expandSchema: true, expandIcon: true }),
    ])
      .pipe(
        tap(([api, resourceTypes]) => {
          this.apiDefinition = this.toApiDefinition(api);
          this.initialApiDefinition = this.apiDefinition;
          this.isReadonly = this.apiDefinition.origin === 'kubernetes' ? true : null;
          this.resourceTypes = resourceTypes;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: any) {
    this.apiDefinition.resources = $event.detail.resources;
    this.isDirty = true;
  }

  private toApiDefinition(api: Api): ApiDefinition {
    const apiDefinition = toApiDefinition(api);
    const plans = api.plans && this.permissionService.hasAnyMatching(['api-plan-r', 'api-plan-u']) ? api.plans : [];
    return { ...apiDefinition, plans };
  }

  onReset() {
    this.apiDefinition = this.initialApiDefinition;
    this.isDirty = false;
  }

  onSubmit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          const updateApi: UpdateApi & { id: string } = { ...api, ...this.apiDefinition };
          return this.apiService.update(updateApi);
        }),
        tap((api) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.apiDefinition = this.toApiDefinition(api);
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
