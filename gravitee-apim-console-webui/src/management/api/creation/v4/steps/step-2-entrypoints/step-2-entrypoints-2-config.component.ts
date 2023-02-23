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
import { takeUntil, tap } from 'rxjs/operators';
import { isEmpty } from 'lodash';

import { EntrypointService } from '../../../../../../services-ngx/entrypoint.service';
import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { HttpListener, HttpListenerPath } from '../../../../../../entities/api-v4';
import { EnvironmentService } from '../../../../../../services-ngx/environment.service';
import { Step3Endpoints1ListComponent } from '../step-3-endpoints/step-3-endpoints-1-list.component';

@Component({
  selector: 'step-2-entrypoints-2-config',
  template: require('./step-2-entrypoints-2-config.component.html'),
  styles: [require('./step-2-entrypoints-2-config.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class Step2Entrypoints2ConfigComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;
  public selectedEntrypoints: { id: string; name: string; supportedListenerType: string }[];
  public entrypointSchemas: Record<string, GioJsonSchema>;
  public entrypointInitialValues: Record<string, any>;
  public entrypointFormGroups: Record<string, FormGroup>;
  public hasListeners: boolean;
  public enableVirtualHost: boolean;
  public domainRestrictions: string[] = [];

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly entrypointService: EntrypointService,
    private readonly stepService: ApiCreationStepService,
    private readonly environmentService: EnvironmentService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;
    const listener = (
      currentStepPayload.listeners && currentStepPayload.listeners.length > 0 ? currentStepPayload.listeners[0] : undefined
    ) as HttpListener;
    const paths = listener?.paths || [];
    this.environmentService
      .getCurrent()
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((environment) => {
          this.domainRestrictions = environment.domainRestrictions || [];
          this.enableVirtualHost =
            !isEmpty(this.domainRestrictions) || paths.find((path) => path.host !== undefined || path.overrideAccess !== undefined) != null;
        }),
      )
      .subscribe();

    this.hasListeners = currentStepPayload.selectedEntrypoints.find((entrypoint) => entrypoint.supportedListenerType === 'http') !== null;
    this.entrypointInitialValues =
      listener?.entrypoints?.reduce((map, { type, configuration }) => ({ ...map, [type]: configuration }), {}) || {};
    this.formGroup = this.formBuilder.group({});
    if (this.hasListeners) {
      this.formGroup.addControl('paths', this.formBuilder.control(paths));
    }
    currentStepPayload.selectedEntrypoints.forEach(({ id }) => {
      this.formGroup.addControl(id, this.formBuilder.group({}));
    });

    forkJoin(
      currentStepPayload.selectedEntrypoints.reduce(
        (map: Record<string, Observable<GioJsonSchema>>, { id }) => ({
          ...map,
          [id]: this.entrypointService.v4GetSchema(id),
        }),
        {},
      ),
    )
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((schemas: Record<string, GioJsonSchema>) => {
        this.entrypointSchemas = schemas;
        this.selectedEntrypoints = currentStepPayload.selectedEntrypoints;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    this.stepService.validStep((previousPayload) => {
      const entrypoints = previousPayload.selectedEntrypoints.map(({ id }) => ({ type: id, configuration: this.formGroup.get(id).value }));
      let paths = this.formGroup.get('paths').value as HttpListenerPath[];
      if (!this.enableVirtualHost) {
        // Remove host and overrideAccess from virualHost if is not necessary
        paths = paths.map(({ path }) => ({ path }));
      } else {
        // Clear private properties from gio-listeners-virtual-host component
        paths = paths.map(({ path, host, overrideAccess }) => ({ path, host, overrideAccess }));
      }

      const listeners: HttpListener[] = [
        {
          type: 'http',
          paths,
          entrypoints,
        },
      ];
      return { ...previousPayload, listeners };
    });
    this.stepService.goToNextStep({ groupNumber: 3, component: Step3Endpoints1ListComponent });
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }
}
