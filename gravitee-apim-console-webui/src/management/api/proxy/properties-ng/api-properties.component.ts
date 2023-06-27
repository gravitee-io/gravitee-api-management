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
import '@gravitee/ui-components/wc/gv-properties';
import { StateParams } from '@uirouter/angularjs';

import { ApiPropertiesService } from './api-properties.service';
import { ChangePropertiesEvent } from './models/ChangePropertiesEvent';
import { SaveProviderEvent } from './models/SaveProviderEvent';

import { ApiDefinition, toApiDefinition } from '../../policy-studio/models/ApiDefinition';
import { ApiService } from '../../../../services-ngx/api.service';
import { AjsRootScope, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { Api } from '../../../../entities/api';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-properties',
  template: require('./api-properties.component.html'),
  styles: [require('./api-properties.component.scss')],
})
export class ApiPropertiesComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public provider: any;

  public api: Api;

  public initialApiDefinition: ApiDefinition;
  public apiDefinition: ApiDefinition;

  public isReadonly = false;
  public isDirty: boolean;

  public providers: any;

  public dynamicPropertySchema: any;

  constructor(
    private readonly apiService: ApiService,
    private readonly apiPropertiesService: ApiPropertiesService,
    private readonly permissionService: GioPermissionService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiService.get(this.ajsStateParams.apiId),
      this.apiPropertiesService.getProviders(),
      this.apiPropertiesService.getDynamicPropertySchema(),
    ])
      .pipe(
        tap(([api, providers, dynamicPropertySchema]) => {
          this.api = api;
          this.apiDefinition = this.toApiDefinition(api);
          this.initialApiDefinition = this.apiDefinition;
          this.provider = this.apiDefinition.services['dynamic-property'];
          this.providers = providers;
          this.dynamicPropertySchema = dynamicPropertySchema;
          this.isReadonly = this.apiDefinition.origin === 'kubernetes';
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onChange($event: ChangePropertiesEvent) {
    this.isDirty = true;
    this.apiDefinition = {
      ...this.apiDefinition,
      properties: $event.detail.properties,
    };
  }

  onSaveProvider($event: SaveProviderEvent) {
    this.isDirty = true;
    const { provider } = $event.detail;
    this.apiDefinition = {
      ...this.apiDefinition,
      services: {
        ...this.apiDefinition.services,
        'dynamic-property': provider,
      },
    };
  }

  onReset() {
    this.apiDefinition = this.initialApiDefinition;
    this.isDirty = false;
  }

  onSubmit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => this.apiService.update({ ...api, ...this.apiDefinition })),
        tap((api) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.apiDefinition = this.toApiDefinition(api);
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private toApiDefinition(api: Api): ApiDefinition {
    const apiDefinition = toApiDefinition(api);
    const plans = api.plans && this.permissionService.hasAnyMatching(['api-plan-r', 'api-plan-u']) ? api.plans : [];
    return { ...apiDefinition, plans };
  }
}
