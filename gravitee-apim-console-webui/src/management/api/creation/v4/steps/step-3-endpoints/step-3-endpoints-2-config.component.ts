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
import { FormBuilder, FormGroup } from '@angular/forms';
import { forkJoin, Observable, Subject } from 'rxjs';
import { GioJsonSchema } from '@gravitee/ui-particles-angular';
import { takeUntil } from 'rxjs/operators';

import { EndpointService } from '../../../../../../services-ngx/endpoint.service';
import { ApiCreationStepService } from '../../services/api-creation-step.service';

@Component({
  selector: 'step-3-endpoints-2-config',
  template: require('./step-3-endpoints-2-config.component.html'),
  styles: [require('./step-3-endpoints-2-config.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step3Endpoints2ConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;
  public selectedEndpoints: { id: string; name: string }[];
  public endpointSchemas: Record<string, GioJsonSchema>;
  public endpointInitialValues: Record<string, any>;
  public endpointFormGroups: Record<string, FormGroup>;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly endpointService: EndpointService,
    private readonly stepService: ApiCreationStepService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;
    this.endpointInitialValues =
      currentStepPayload.endpoints?.reduce((map, { type, configuration }) => ({ ...map, [type]: configuration }), {}) || {};
    this.formGroup = this.formBuilder.group({});
    currentStepPayload.selectedEndpoints.forEach(({ id }) => {
      this.formGroup.addControl(id, this.formBuilder.group({}));
    });

    forkJoin(
      currentStepPayload.selectedEndpoints.reduce(
        (map: Record<string, Observable<GioJsonSchema>>, { id }) => ({
          ...map,
          [id]: this.endpointService.v4GetSchema(id),
        }),
        {},
      ),
    )
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((schemas: Record<string, GioJsonSchema>) => {
        this.endpointSchemas = schemas;
        this.selectedEndpoints = currentStepPayload.selectedEndpoints;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  public getEntryPointFormGroup(id: string): FormGroup {
    return this.formGroup.get(id) as FormGroup;
  }

  save(): void {
    const currentStepPayload = this.stepService.payload;
    const endpoints = currentStepPayload.selectedEndpoints.map(({ id }) => ({ type: id, configuration: this.formGroup.get(id).value }));
    this.stepService.validStepAndGoNext((previousPayload) => ({ ...previousPayload, endpoints }));
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }
}
