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
import { groupBy } from 'lodash';
import { StateService } from '@uirouter/angular';

import { ApiCreationStep, ApiCreationStepperService } from './services/api-creation-stepper.service';
import { Step2Entrypoints1List } from './steps/step-2-entrypoints/step-2-entrypoints-1-list.component';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { Step6SummaryComponent } from './steps/step-6-summary/step-6-summary.component';
import { ApiCreationStepService } from './services/api-creation-step.service';
import { ApiCreationPayload } from './models/ApiCreationPayload';
import { MenuStepItem } from './components/api-creation-stepper-menu/api-creation-stepper-menu.component';
import { Step1MenuItemComponent } from './steps/step-1-menu-item/step-1-menu-item.component';
import { Step3Endpoints1ListComponent } from './steps/step-3-endpoints/step-3-endpoints-1-list.component';
import { Step4SecurityComponent } from './steps/step-4-security/step-4-security.component';
import { Step5DocumentationComponent } from './steps/step-5-documentation/step-5-documentation.component';
import { StepEntrypointMenuItemComponent } from './steps/step-connector-menu-item/step-entrypoint-menu-item.component';
import { StepEndpointMenuItemComponent } from './steps/step-connector-menu-item/step-endpoint-menu-item.component';

import { ApiV4Service } from '../../../../services-ngx/api-v4.service';
import { fakeNewApiEntity } from '../../../../entities/api-v4';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { UIRouterState } from '../../../../ajs-upgraded-providers';

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
      label: 'API details',
      component: Step1ApiDetailsComponent,
      menuItemComponent: Step1MenuItemComponent,
    },
    {
      label: 'Entrypoints',
      component: Step2Entrypoints1List,
      menuItemComponent: StepEntrypointMenuItemComponent,
    },
    {
      label: 'Endpoints',
      component: Step3Endpoints1ListComponent,
      menuItemComponent: StepEndpointMenuItemComponent,
    },
    {
      label: 'Security',
      component: Step4SecurityComponent,
    },
    {
      label: 'Documentation',
      component: Step5DocumentationComponent,
    },
    {
      label: 'Summary',
      component: Step6SummaryComponent,
    },
  ]);

  @HostBinding('class.creating-api')
  public isCreatingApi = false;

  menuSteps$: Observable<MenuStepItem[]> = this.stepper.steps$.pipe(
    map((steps) => {
      // Get the last step valid or last step of each label. To have last full payload of each label.
      const lastStepOfEachLabel = Object.entries(groupBy(steps, 'label')).map(([_, steps]) => {
        const lastValidStep = steps.reverse().find((step) => step.state === 'valid');
        return lastValidStep || steps[steps.length - 1];
      });

      return lastStepOfEachLabel.map((step) => ({
        ...step,
        payload: this.stepper.compileStepPayload(step),
      }));
    }),
  );

  constructor(
    private readonly injector: Injector,
    private readonly apiV4Service: ApiV4Service,
    private readonly snackBarService: SnackBarService,
    @Inject(UIRouterState) readonly ajsState: StateService,
  ) {}

  ngOnInit(): void {
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
      .create(
        // Note : WIP 🚧
        // Use the fakeNewApiEntity to create a new API temporarily
        // The real API creation will be done when we complete other api creation steps
        fakeNewApiEntity((api) => {
          return {
            ...api,
            name: apiCreationPayload.name,
            apiVersion: apiCreationPayload.version,
            description: apiCreationPayload.description ?? '',
            listeners: apiCreationPayload.listeners,
          };
        }),
      )
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
