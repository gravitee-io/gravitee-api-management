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
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import '@gravitee/ui-components/wc/gv-properties';
import { StateParams } from '@uirouter/angularjs';

import { ApiPropertiesOldService } from './api-properties-old.service';
import { ChangePropertiesEvent, Property } from './models/ChangePropertiesEvent';
import { SaveProviderEvent } from './models/SaveProviderEvent';

import { AjsRootScope, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV2, ApiV4, UpdateApiV2, UpdateApiV4 } from '../../../../entities/management-api-v2';

@Component({
  selector: 'api-properties',
  template: require('./api-properties-old.component.html'),
  styles: [require('./api-properties-old.component.scss')],
})
export class ApiPropertiesOldComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public provider: any;

  public api: ApiV2 | ApiV4;

  public initialApi: ApiV2 | ApiV4;

  public isReadonly = false;
  public isDirty: boolean;

  public providers: any;

  public dynamicPropertySchema: any;
  public properties: Property[];

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly apiPropertiesService: ApiPropertiesOldService,
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
          if (api.definitionVersion !== 'V1') {
            this.initApiData(api);
            this.providers = providers;
            this.dynamicPropertySchema = dynamicPropertySchema;
            this.isReadonly = this.api.definitionContext.origin === 'KUBERNETES';
          } else {
            throw new Error('Unexpected API type. This page is compatible only for API > V1');
          }
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
    this.properties = $event.detail.properties;
  }

  onSaveProvider($event: SaveProviderEvent) {
    this.isDirty = true;
    this.provider = $event.detail.provider;
  }

  onReset() {
    this.initApiData(this.initialApi);
    this.isDirty = false;
  }

  onSubmit() {
    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        switchMap((api) => {
          if (api.definitionVersion !== 'V1') {
            const updateApi: UpdateApiV2 | UpdateApiV4 = {
              ...api,
              properties: this.properties,
              services: {
                ...this.api.services,
                dynamicProperty: this.provider,
              },
            };
            return this.apiService.update(this.ajsStateParams.apiId, updateApi);
          } else {
            return EMPTY;
          }
        }),
        tap((api: ApiV2 | ApiV4) => {
          this.ajsRootScope.$broadcast('apiChangeSuccess', { api });
          this.initApiData(api);
          this.isDirty = false;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private initApiData(api: ApiV2 | ApiV4) {
    this.api = api;
    this.properties = this.api.properties;
    this.initialApi = this.api;
    this.provider = this.api.services?.dynamicProperty;
  }
}
