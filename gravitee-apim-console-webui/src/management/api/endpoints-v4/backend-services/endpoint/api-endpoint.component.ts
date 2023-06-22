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
import { StateService } from '@uirouter/core';
import { switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, Subject } from 'rxjs';
import { GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { UIRouterState, UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../../services-ngx/api-v2.service';
import { ApiV4, EndpointGroupV4 } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';

@Component({
  selector: 'api-endpoint',
  template: require('./api-endpoint.component.html'),
  styles: [require('./api-endpoint.component.scss')],
})
export class ApiEndpointComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public endpointGroup: EndpointGroupV4;
  public formGroup: FormGroup;
  public endpointSchema: { config: GioJsonSchema; sharedConfig: GioJsonSchema };
  public isLoading = false;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
  ) {}

  public ngOnInit(): void {
    this.isLoading = true;
    const apiId = this.ajsStateParams.apiId;
    const groupName = this.ajsStateParams.groupName;

    this.apiService
      .get(apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api: ApiV4) => {
          this.endpointGroup = api.endpointGroups.find((group) => group.name === groupName);
          return combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema(this.endpointGroup.type),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema(this.endpointGroup.type),
          ]);
        }),
        tap(([config, sharedConfig]) => {
          this.endpointSchema = { config, sharedConfig };
          this.formGroup = new FormGroup({
            name: new FormControl(null, Validators.required),
            configuration: new FormControl(GioFormJsonSchemaComponent.isDisplayable(config) ? config : {}),
            sharedConfiguration: new FormControl(GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : {}),
          });
        }),
      )
      .subscribe(() => (this.isLoading = false));
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
