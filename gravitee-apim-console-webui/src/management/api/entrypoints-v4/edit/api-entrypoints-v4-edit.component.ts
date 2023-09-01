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
import { EMPTY, forkJoin, of, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { ApiV4, ConnectorPlugin, Entrypoint, Listener, Qos, UpdateApiV4 } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { IconService } from '../../../../services-ngx/icon.service';

type DlqElement = { name: string; type: string; icon: string };
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
  public supportDlq: boolean;
  public enabledDlq: boolean;
  public dlqElements: { name: string; elements: DlqElement[] }[] = [];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly iconService: IconService,
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

                this.supportDlq = matchingEntrypoint.availableFeatures?.includes('DLQ');
                if (this.supportDlq) {
                  this.enabledDlq = !!this.entrypoint.dlq?.endpoint;
                }
              }
            }
          }
        }),
        switchMap((_) => this.connectorPluginsV2Service.getEntrypointPluginSchema(this.entrypoint.type)),
        tap((schema) => {
          this.entrypointSchema = schema;
        }),
        switchMap((_) => (this.supportDlq ? this.connectorPluginsV2Service.listEndpointPlugins() : of(null))),
        tap((plugins: ConnectorPlugin[] | null) => {
          this.form = new FormGroup({});
          this.form.addControl(`${this.entrypoint.type}-config`, new FormControl(this.entrypoint.configuration));
          this.form.addControl(`${this.entrypoint.type}-qos`, new FormControl(this.entrypoint.qos));

          if (this.supportDlq && plugins) {
            this.dlqElements = this.getEligibleApiEndpointsAndEndpointGroupsForDlq(plugins);

            this.form.addControl('enabledDlq', new FormControl(this.enabledDlq));
            if (this.enabledDlq) {
              const selectedDlqElement = this.findSelectedElement(this.dlqElements, this.entrypoint.dlq?.endpoint);
              this.form.addControl('dlqElement', new FormControl(selectedDlqElement, Validators.required));
            }

            this.handleEnabledChanges();
          }
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  private getEligibleApiEndpointsAndEndpointGroupsForDlq(plugins: ConnectorPlugin[]) {
    const dlqElements: { name: string; elements: DlqElement[] }[] = [];
    const availableGroups: DlqElement[] = [];
    const availableEndpoints: DlqElement[] = [];

    const eligibleEndpointTypesForDlq = this.getEligibleEndpointTypesForDlq(plugins);

    this.api.endpointGroups
      .slice(1) // skip default group
      .forEach((endpointGroup) => {
        const eligibleEndpointType = eligibleEndpointTypesForDlq.find((value) => value.id === endpointGroup.type);
        if (eligibleEndpointType) {
          availableGroups.push({
            name: endpointGroup.name,
            type: eligibleEndpointType.name,
            icon: eligibleEndpointType.icon,
          });
          endpointGroup.endpoints.forEach((endpoint) => {
            availableEndpoints.push({
              name: endpoint.name,
              type: eligibleEndpointType.name,
              icon: eligibleEndpointType.icon,
            });
          });
        }
      });
    if (availableEndpoints.length > 0) {
      dlqElements.push({ name: 'Endpoints', elements: availableEndpoints });
    }
    if (availableGroups.length > 0) {
      dlqElements.push({ name: 'Endpoint Groups', elements: availableGroups });
    }

    return dlqElements;
  }

  private getEligibleEndpointTypesForDlq(plugins: ConnectorPlugin[]) {
    return plugins
      .filter((plugin) => plugin.supportedModes.includes('PUBLISH') && plugin.supportedApiType === 'MESSAGE')
      .map((plugin) => {
        return { id: plugin.id, name: plugin.name, icon: this.iconService.registerSvg(plugin.id, plugin.icon) };
      });
  }

  private handleEnabledChanges(): void {
    this.form
      .get('enabledDlq')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((value) => {
        this.enabledDlq = value;
        if (this.enabledDlq) {
          this.form.addControl('dlqElement', new FormControl(null, Validators.required));
        } else {
          this.form.removeControl('dlqElement');
        }
      });
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

          if (this.supportDlq) {
            if (this.enabledDlq) {
              const dlqEndpoint: DlqElement = this.form.get('dlqElement').value;
              updatedEntrypoint.dlq = { endpoint: dlqEndpoint.name };
            } else {
              updatedEntrypoint.dlq = null;
            }
          }

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

  private findSelectedElement(dlqElements: { name: string; elements: DlqElement[] }[], endpoint: string | undefined) {
    let selectedElement: DlqElement | undefined;
    dlqElements.forEach((dlqElement) => {
      if (!selectedElement) {
        selectedElement = dlqElement.elements.find((element) => element.name === endpoint);
      }
    });
    return selectedElement;
  }
}
