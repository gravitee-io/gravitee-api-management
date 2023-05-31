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
import { takeUntil, map } from 'rxjs/operators';
import { ConnectorsInfo, Flow as PSFlow, Plan as PSPlan } from '@gravitee/ui-policy-studio-angular';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiType, ApiV4 } from '../../../../entities/management-api-v2';
import { IconService } from '../../../../services-ngx/icon.service';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { ApiPlanV2Service } from '../../../../services-ngx/api-plan-v2.service';

@Component({
  selector: 'api-v4-policy-studio-design',
  template: require('./api-v4-policy-studio-design.component.html'),
  styles: [require('./api-v4-policy-studio-design.component.scss')],
})
export class ApiV4PolicyStudioDesignComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<boolean>();

  public apiType: ApiType;
  public entrypointsInfo: ConnectorsInfo[];
  public endpointsInfo: ConnectorsInfo[];
  public commonFlows: PSFlow[];
  public plans: PSPlan[];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly iconService: IconService,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiPlanV2Service: ApiPlanV2Service,
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.apiV2Service.get(this.ajsStateParams.apiId).pipe(map((api) => api as ApiV4)),
      this.connectorPluginsV2Service.listEntrypointPlugins(),
      this.connectorPluginsV2Service.listEndpointPlugins(),
      this.apiPlanV2Service
        .list(
          this.ajsStateParams.apiId,
          undefined,
          ['PUBLISHED'],
          1,
          // No pagination here. Policy Studio doesn't support it for now.
          9999,
        )
        .pipe(map((apiPlansResponse) => apiPlansResponse.data)),
    ])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([api, entrypoints, endpoints, plans]) => {
        this.apiType = api.type;

        this.entrypointsInfo = api.listeners.flatMap((listener) =>
          listener.entrypoints.map((entrypoint) => {
            const entrypointPlugin = entrypoints.find((entrypointPlugin) => entrypointPlugin.id === entrypoint.type);
            return {
              type: entrypoint.type,
              icon:
                entrypointPlugin && entrypointPlugin.icon
                  ? this.iconService.registerSvg(entrypoint.type, entrypointPlugin.icon)
                  : 'gio:language',
            };
          }),
        );

        this.endpointsInfo = api.endpointGroups.flatMap((endpointGroup) =>
          endpointGroup.endpoints.map((endpoint) => {
            const endpointPlugin = endpoints.find((endpointPlugin) => endpointPlugin.id === endpoint.type);
            return {
              type: endpoint.type,
              icon:
                endpointPlugin && endpointPlugin.icon ? this.iconService.registerSvg(endpoint.type, endpointPlugin.icon) : 'gio:language',
            };
          }),
        );

        this.commonFlows = api.flows;

        this.plans = plans.map((plan) => ({
          name: plan.name,
          flows: plan.flows,
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }
}
