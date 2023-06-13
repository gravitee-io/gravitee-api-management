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
import { FormControl, FormGroup } from '@angular/forms';
import { combineLatest, forkJoin, Observable, Subject } from 'rxjs';
import { GioFormJsonSchemaComponent, GioJsonSchema } from '@gravitee/ui-particles-angular';
import { takeUntil } from 'rxjs/operators';
import { mapValues, omitBy } from 'lodash';

import { ConnectorPluginsV2Service } from '../../../../../../services-ngx/connector-plugins-v2.service';
import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step4Security1PlansComponent } from '../step-4-security/step-4-security-1-plans.component';

@Component({
  selector: 'step-3-endpoints-2-config',
  template: require('./step-3-endpoints-2-config.component.html'),
  styles: [require('./step-3-endpoints-2-config.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step3Endpoints2ConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;
  public selectedEndpoints: { id: string; name: string }[];
  public endpointSchemas: Record<string, { config: GioJsonSchema; sharedConfig: GioJsonSchema }>;

  constructor(
    private readonly connectorPluginsV2Service: ConnectorPluginsV2Service,
    private readonly stepService: ApiCreationStepService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    forkJoin(
      currentStepPayload.selectedEndpoints.reduce((map: Record<string, Observable<[GioJsonSchema, GioJsonSchema]>>, { id }) => {
        return {
          ...map,
          [id]: combineLatest([
            this.connectorPluginsV2Service.getEndpointPluginSchema(id),
            this.connectorPluginsV2Service.getEndpointPluginSharedConfigurationSchema(id),
          ]),
        };
      }, {}),
    )
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((schemas: Record<string, [GioJsonSchema, GioJsonSchema]>) => {
        const displayableSchemas = mapValues(schemas, ([config, sharedConfig]) => ({
          config: GioFormJsonSchemaComponent.isDisplayable(config) ? config : undefined,
          sharedConfig: GioFormJsonSchemaComponent.isDisplayable(sharedConfig) ? sharedConfig : undefined,
        }));

        this.endpointSchemas = omitBy(displayableSchemas, ({ config, sharedConfig }) => {
          return !config && !sharedConfig;
        });

        this.selectedEndpoints = currentStepPayload.selectedEndpoints;

        this.formGroup = new FormGroup({
          ...(currentStepPayload.selectedEndpoints?.reduce(
            (map, { id, configuration, sharedConfiguration }) => ({
              ...map,
              [id + '-configuration']: new FormControl(configuration ?? {}),
              [id + '-sharedConfiguration']: new FormControl(sharedConfiguration ?? {}),
            }),
            {},
          ) ?? {}),
        });
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      selectedEndpoints: previousPayload.selectedEndpoints.map(({ id, name, icon, deployed }) => ({
        id,
        name,
        icon,
        configuration: this.formGroup.get(id + '-configuration')?.value,
        sharedConfiguration: this.formGroup.get(id + '-sharedConfiguration')?.value,
        deployed,
      })),
    }));

    this.stepService.goToNextStep({ groupNumber: 4, component: Step4Security1PlansComponent });
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }
}
