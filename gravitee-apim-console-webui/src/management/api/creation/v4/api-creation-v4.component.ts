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

import { Component, HostBinding, Inject, Injector, OnDestroy, OnInit } from '@angular/core';
import { map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Observable, Subject } from 'rxjs';
import { StateService } from '@uirouter/angular';

import { ApiCreationStep, ApiCreationStepperService } from './services/api-creation-stepper.service';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { ApiCreationStepService } from './services/api-creation-step.service';
import { ApiCreationPayload } from './models/ApiCreationPayload';
import { MenuStepItem } from './components/api-creation-stepper-menu/api-creation-stepper-menu.component';
import { Step1MenuItemComponent } from './steps/step-1-menu-item/step-1-menu-item.component';
import { StepEntrypointMenuItemComponent } from './steps/step-connector-menu-item/step-entrypoint-menu-item.component';
import { StepEndpointMenuItemComponent } from './steps/step-connector-menu-item/step-endpoint-menu-item.component';

import { ApiV4Service } from '../../../../services-ngx/api-v4.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { EndpointGroup } from '../../../../entities/api-v4';

@Component({
  selector: 'api-creation-v4',
  template: require('./api-creation-v4.component.html'),
  styles: [require('./api-creation-v4.component.scss')],
})
export class ApiCreationV4Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public currentStep: ApiCreationStep & { injector: Injector; payload: ApiCreationPayload };

  public stepper = new ApiCreationStepperService([
    {
      groupNumber: 1,
      label: 'API details',
      menuItemComponent: Step1MenuItemComponent,
    },
    {
      groupNumber: 2,
      label: 'Entrypoints',
      menuItemComponent: StepEntrypointMenuItemComponent,
    },
    {
      groupNumber: 3,
      label: 'Endpoints',
      menuItemComponent: StepEndpointMenuItemComponent,
    },
    {
      groupNumber: 4,
      label: 'Security',
    },
    {
      groupNumber: 5,
      label: 'Documentation',
    },
    {
      groupNumber: 6,
      label: 'Summary',
    },
  ]);

  @HostBinding('class.creating-api')
  public isCreatingApi = false;

  menuSteps$: Observable<MenuStepItem[]> = this.stepper.steps$.pipe(
    map((steps) => {
      // For each group, get the last step valid if present. To have the last state & full payload
      return this.stepper.groups.map((group) => {
        const stepsGroup = steps.filter((step) => step.group.groupNumber === group.groupNumber);
        const lastValidStep = stepsGroup.reverse().find((step) => step.state === 'valid');

        return {
          ...group,
          ...(lastValidStep
            ? { state: lastValidStep?.state ?? 'initial', payload: this.stepper.compileStepPayload(lastValidStep) }
            : { state: 'initial', payload: {} }),
        };
      });
    }),
  );

  constructor(
    private readonly injector: Injector,
    private readonly apiV4Service: ApiV4Service,
    private readonly snackBarService: SnackBarService,
    @Inject(UIRouterState) readonly ajsState: StateService,
  ) {}

  ngOnInit(): void {
    this.stepper.goToNextStep({
      groupNumber: 1,
      component: Step1ApiDetailsComponent,
    });

    // When the stepper change, update the current step
    this.stepper.currentStep$.pipe(takeUntil(this.unsubscribe$)).subscribe((apiCreationStep) => {
      const apiCreationStepService = new ApiCreationStepService(this.stepper, apiCreationStep);

      this.currentStep = {
        ...apiCreationStep,
        payload: apiCreationStepService.payload,
        injector: Injector.create({
          providers: [{ provide: ApiCreationStepService, useValue: apiCreationStepService }],
          parent: this.injector,
        }),
      };
    });

    // When then stepper is finished, create the API
    this.stepper.finished$
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((p) => this.createApi(p)),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goToStep(label: string) {
    this.stepper.goToStepLabel(label);
  }

  private createApi(apiCreationPayload: ApiCreationPayload) {
    this.isCreatingApi = true;
    return this.apiV4Service
      .create({
        definitionVersion: '4.0.0',
        name: apiCreationPayload.name,
        apiVersion: apiCreationPayload.version,
        description: apiCreationPayload.description ?? '',
        listeners: apiCreationPayload.listeners,
        type: apiCreationPayload.type,
        endpointGroups: apiCreationPayload.selectedEndpoints.map(
          (endpoint) =>
            ({
              name: `Default ${endpoint.name} group`,
              type: endpoint.id,
              endpoints: [
                {
                  name: `Default ${endpoint.name}`,
                  type: endpoint.id,
                  weight: 1,
                  inheritConfiguration: false,
                  configuration: endpoint.configuration,
                },
              ],
            } as EndpointGroup),
        ),
      })
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(
          (api) => {
            this.snackBarService.success(`API ${apiCreationPayload.deploy ? 'deployed' : 'created'} successfully!`);
            this.ajsState.go('management.apis.create-v4-confirmation', { apiId: api.id });
          },
          (err) => {
            this.snackBarService.error(
              err.error?.message ?? `An error occurred while ${apiCreationPayload.deploy ? 'deploying' : 'creating'} the API.`,
            );
            return EMPTY;
          },
        ),
      );
  }
}
