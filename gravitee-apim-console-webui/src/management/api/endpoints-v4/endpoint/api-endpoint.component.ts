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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiV2Service } from '../../../../services-ngx/api-v2.service';
import { ApiV4, ConnectorPlugin, EndpointGroupV4, EndpointV4 } from '../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../services-ngx/connector-plugins-v2.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { IconService } from '../../../../services-ngx/icon.service';
import { isEndpointNameUnique, isEndpointNameUniqueAndDoesNotMatchDefaultValue } from '../api-endpoint-v4-unique-name';

@Component({
  selector: 'api-endpoint',
  template: require('./api-endpoint.component.html'),
  styles: [require('./api-endpoint.component.scss')],
})
export class ApiEndpointComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  private groupIndex: number;
  private endpointIndex: number;
  public endpointGroup: EndpointGroupV4;
  public formGroup: FormGroup;
  public endpointSchema: { config: GioJsonSchema; sharedConfig: GioJsonSchema };
  public connectorPlugin: ConnectorPlugin;
  public isLoading = false;
  private api: ApiV4;
  private endpoint: EndpointV4;
  private mode: 'edit' | 'create';

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly iconService: IconService,
  ) {}

  public ngOnInit(): void {
    this.isLoading = true;
    const apiId = this.activatedRoute.snapshot.params.apiId;
    this.groupIndex = +this.activatedRoute.snapshot.params.groupIndex;
    this.mode = this.activatedRoute.snapshot.params.endpointIndex !== undefined ? 'edit' : 'create';

    this.apiService
      .get(apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          this.api = api;
          this.endpointGroup = api.endpointGroups[this.groupIndex];
          return combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema(this.endpointGroup.type),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema(this.endpointGroup.type),
            this.connectorPluginsV2Service.getEndpointPlugin(this.endpointGroup.type),
          ]);
        }),
        tap(([config, sharedConfig, connectorPlugin]) => {
          this.endpointSchema = {
            config: GioFormJsonSchemaComponent.isDisplayable(config) ? config : null,
            sharedConfig: GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : null,
          };
          this.connectorPlugin = { ...connectorPlugin, icon: this.iconService.registerSvg(connectorPlugin.id, connectorPlugin.icon) };
          this.initForm();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => (this.isLoading = false));
  }

  public ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public onSave() {
    const inheritConfiguration = this.formGroup.get('inheritConfiguration').value;

    const updatedEndpoint: EndpointV4 = {
      type: this.endpointGroup.type,
      name: this.formGroup.get('name').value.trim(),
      weight: this.formGroup.get('weight').value,
      configuration: this.formGroup.get('configuration').value,
      sharedConfigurationOverride: inheritConfiguration ? {} : this.formGroup.get('sharedConfigurationOverride').value,
      inheritConfiguration,
    };

    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        switchMap((api: ApiV4) => {
          const endpointGroups = api.endpointGroups.map((group, i) => {
            if (i === this.groupIndex) {
              return {
                ...group,
                endpoints:
                  this.endpointIndex !== undefined
                    ? group.endpoints.map((endpoint, j) => (j === this.endpointIndex ? updatedEndpoint : endpoint))
                    : [...group.endpoints, updatedEndpoint],
              };
            }
            return group;
          });
          return this.apiService.update(api.id, { ...api, endpointGroups });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(() => {
          this.snackBarService.success(`Endpoint successfully created!`);
          this.router.navigate(['../../'], { relativeTo: this.activatedRoute });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  public onInheritConfigurationChange() {
    if (this.formGroup.get('inheritConfiguration').value) {
      this.formGroup.get('sharedConfigurationOverride').disable();
    } else {
      this.formGroup.get('sharedConfigurationOverride').enable();
    }
  }

  private initForm() {
    let name = null;
    let inheritConfiguration = true;
    let configuration = null;
    let sharedConfigurationOverride = this.endpointGroup.sharedConfiguration;
    let weight = null;

    if (this.mode === 'edit') {
      this.endpointIndex = +this.activatedRoute.snapshot.params.endpointIndex;
      this.endpoint = this.endpointGroup.endpoints[this.endpointIndex];

      name = this.endpoint.name;
      weight = this.endpoint.weight;
      inheritConfiguration = this.endpoint.inheritConfiguration;
      configuration = this.endpoint.configuration;
      if (!inheritConfiguration) {
        sharedConfigurationOverride = this.endpoint.sharedConfigurationOverride;
      }
    }

    this.formGroup = new FormGroup({
      name: new FormControl(name, [
        Validators.required,
        this.mode === 'edit'
          ? isEndpointNameUniqueAndDoesNotMatchDefaultValue(this.api, this.endpoint.name)
          : isEndpointNameUnique(this.api),
      ]),
      weight: new FormControl(weight, Validators.required),
      inheritConfiguration: new FormControl(inheritConfiguration),
      configuration: new FormControl(configuration),
      sharedConfigurationOverride: new FormControl({ value: sharedConfigurationOverride, disabled: inheritConfiguration }),
    });
  }
}
