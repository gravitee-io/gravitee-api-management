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
import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { EMPTY, forkJoin, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormControl, FormGroup } from '@angular/forms';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV4, ConnectorPlugin, Entrypoint, Listener, Qos, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-entrypoints-v4-edit',
  template: require('./api-entrypoints-v4-edit.component.html'),
  styles: [require('./api-entrypoints-v4-edit.component.scss')],
})
export class ApiEntrypointsV4EditComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiId: string;
  private entrypointId: string;
  private api: ApiV4;
  private availableEntrypoints: ConnectorPlugin[];
  private listener: Listener;
  public form: FormGroup;
  public entrypoint: Entrypoint;
  public entrypointName: string;
  public entrypointSchema: GioJsonSchema;
  public supportedQos: Qos[];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly snackBarService: SnackBarService,
  ) {
    this.apiId = this.ajsStateParams.apiId;
    this.entrypointId = this.ajsStateParams.entrypointId;
  }

  ngOnInit(): void {
    forkJoin([this.connectorPluginsV2Service.listAsyncEntrypointPlugins(), this.apiService.get(this.apiId)])
      .pipe(
        tap(([availableEntrypoints, api]) => {
          if (api.definitionVersion === 'V4') {
            this.api = api as ApiV4;
            this.availableEntrypoints = availableEntrypoints;

            this.api.listeners.forEach((listener) => {
              listener.entrypoints.forEach((entrypoint) => {
                if (entrypoint.type === this.entrypointId) {
                  this.entrypoint = entrypoint;
                  this.listener = listener;
                }
              });
            });

            if (this.entrypoint) {
              const matchingEntrypoint = this.availableEntrypoints.find((entrypoint) => entrypoint.id === this.entrypoint.type);
              if (matchingEntrypoint) {
                this.entrypointName = matchingEntrypoint.name;
                this.supportedQos = matchingEntrypoint.supportedQos;
              }
            }
          }
        }),
        switchMap((_) => this.connectorPluginsV2Service.getEntrypointPluginSchema(this.entrypoint.type)),
        tap((schema) => {
          this.entrypointSchema = schema;
          this.form = new FormGroup({});
          this.form.addControl(`${this.entrypoint.type}-config`, new FormControl(this.entrypoint.configuration));
          this.form.addControl(`${this.entrypoint.type}-qos`, new FormControl(this.entrypoint.qos));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onSaveEntrypointConfig() {
    const configurationValue = this.form.get(`${this.entrypoint.type}-config`).value;
    const qosValue = this.form.get(`${this.entrypoint.type}-qos`).value;

    this.apiService
      .get(this.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const listenerToUpdate: Listener = {
            ...api.listeners.find((listener) => listener.entrypoints.some((entrypoint) => entrypoint.type === this.entrypointId)),
          };
          const entrypointToUpdate = listenerToUpdate.entrypoints.find((entrypoint) => entrypoint.type === this.entrypointId);

          const updatedEntrypoint: Entrypoint = { ...entrypointToUpdate, configuration: configurationValue, qos: qosValue };
          const updatedListener: Listener = {
            ...listenerToUpdate,
            entrypoints: [...listenerToUpdate.entrypoints.filter((entrypoint) => entrypoint.type !== this.entrypointId), updatedEntrypoint],
          };
          const updateApi: UpdateApiV4 = {
            ...api,
            listeners: [...api.listeners.filter((listener) => listener.type !== listenerToUpdate.type), updatedListener],
          };

          return this.apiService.update(this.apiId, updateApi);
        }),
        tap(() => {
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.ajsState.go('management.apis.ng.entrypoints');
      });
  }
}
