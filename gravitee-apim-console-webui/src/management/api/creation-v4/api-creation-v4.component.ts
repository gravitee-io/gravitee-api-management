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

import { Component, HostBinding, Injector, OnDestroy, OnInit } from '@angular/core';
import { catchError, concatMap, map, switchMap, takeUntil, toArray } from 'rxjs/operators';
import { from, Observable, of, Subject, throwError } from 'rxjs';
import { isEmpty, isString } from 'lodash';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiCreationStep, ApiCreationStepperService } from './services/api-creation-stepper.service';
import { Step1ApiDetailsComponent } from './steps/step-1-api-details/step-1-api-details.component';
import { ApiCreationStepService } from './services/api-creation-step.service';
import { ApiCreationPayload } from './models/ApiCreationPayload';
import { MenuStepItem } from './components/api-creation-stepper-menu/api-creation-stepper-menu.component';
import { Step1MenuItemComponent } from './steps/step-1-menu-item/step-1-menu-item.component';
import { StepEntrypointMenuItemComponent } from './steps/step-connector-menu-item/step-entrypoint-menu-item.component';
import { StepEndpointMenuItemComponent } from './steps/step-connector-menu-item/step-endpoint-menu-item.component';
import { Step4MenuItemComponent } from './steps/step-4-menu-item/step-4-menu-item.component';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';
import { PlanV4, Api, CreateApiV4, EndpointGroupV4, Entrypoint, Listener, ListenerType } from '../../../entities/management-api-v2';
import { ApiReviewV2Service } from '../../../services-ngx/api-review-v2.service';

export interface Result {
  errorMessages: string[];
  apiCreationPayload: ApiCreationPayload;
  result?: {
    api?: Api;
    plans?: PlanV4[];
    deployed?: boolean;
  };
}

@Component({
  selector: 'api-creation-v4',
  templateUrl: './api-creation-v4.component.html',
  styleUrls: ['./api-creation-v4.component.scss'],
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
      menuItemComponent: Step4MenuItemComponent,
    },
    {
      groupNumber: 5,
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
        const hasInvalidStep = stepsGroup.find((step) => step.state === 'invalid');
        const groupState = hasInvalidStep ? 'invalid' : lastValidStep?.state ?? 'initial';

        return {
          ...group,
          ...(lastValidStep
            ? { state: groupState, payload: this.stepper.compileStepPayload(lastValidStep) }
            : { state: groupState, payload: {} }),
        };
      });
    }),
  );

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly injector: Injector,
    private readonly apiV2Service: ApiV2Service,
    private readonly apiPlanV2Service: ApiPlanV2Service,
    private readonly apiReviewV2Service: ApiReviewV2Service,
    private readonly snackBarService: SnackBarService,
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
        switchMap((p) => this.createApi$(p)),
        switchMap((previousResult) => this.createAndPublishPlans$(previousResult)),
        switchMap((previousResult) => this.startApi$(previousResult)),
        switchMap((previousResult) => this.askForReview$(previousResult)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(
        (finalResult) => {
          if (isEmpty(finalResult.errorMessages)) {
            this.snackBarService.success(`API ${finalResult.result.deployed ? 'deployed' : 'created'} successfully!`);
          } else {
            // When some non-blocking error happen
            this.snackBarService.error(finalResult.errorMessages.join('\n'));
          }
          if (finalResult.result?.api?.id) {
            this.router.navigate(['.', finalResult.result.api.id], { relativeTo: this.activatedRoute });
          }
        },
        (error) => {
          // When the error is blocking
          this.snackBarService.error(error.message ?? 'An error occurred while creating the API');
        },
      );
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goToStep(label: string) {
    this.stepper.goToStepLabel(label);
  }

  private createApi$(apiCreationPayload: ApiCreationPayload): Observable<Result> {
    this.isCreatingApi = true;

    // Get distinct listener types
    const listenersType = [...new Set(apiCreationPayload.selectedEntrypoints.map(({ supportedListenerType }) => supportedListenerType))];

    // Create one listener per supportedListenerType and add all supported entrypoints
    const listeners: Listener[] = listenersType.reduce((listeners, listenersType) => {
      const entrypoints: Entrypoint[] = apiCreationPayload.selectedEntrypoints
        .filter((e) => e.supportedListenerType === listenersType)
        .map(({ id, configuration, selectedQos }) => ({
          type: id,
          configuration: configuration,
          qos: selectedQos,
        }));

      const listenerConfig = {
        type: listenersType,
        ...this.getListenerSpecificConfig(listenersType, apiCreationPayload),
        entrypoints,
      };
      return [...listeners, listenerConfig];
    }, []);

    const newV4Api: CreateApiV4 = {
      definitionVersion: 'V4',
      name: apiCreationPayload.name,
      apiVersion: apiCreationPayload.version,
      description: apiCreationPayload.description ?? '',
      listeners: listeners,
      type: apiCreationPayload.type,
      endpointGroups: apiCreationPayload.selectedEndpoints.map(
        (endpoint) =>
          ({
            name: `Default ${endpoint.name} group`,
            type: endpoint.id,
            sharedConfiguration: endpoint.sharedConfiguration,
            endpoints: [
              {
                name: `Default ${endpoint.name}`,
                type: endpoint.id,
                weight: 1,
                inheritConfiguration: true,
                configuration: endpoint.configuration,
              },
            ],
          }) as EndpointGroupV4,
      ),
    };

    return this.apiV2Service.create(newV4Api).pipe(
      map((apiEntity) => ({ apiCreationPayload, result: { api: apiEntity }, errorMessages: [] })),
      catchError((err) => {
        // Blocking error - If thrown, stop observable chain
        return throwError({ message: err.error?.message ?? `Error occurred when creating API!` });
      }),
    );
  }

  private createAndPublishPlans$(previousResult: Result): Observable<Result> {
    if (isEmpty(previousResult.apiCreationPayload.plans)) {
      return of(previousResult);
    }

    const api = previousResult.result.api;

    // For each plan
    return from(previousResult.apiCreationPayload.plans).pipe(
      concatMap((plan) =>
        // Create it
        this.apiPlanV2Service.create(api.id, { ...plan, definitionVersion: 'V4' }).pipe(
          concatMap((plan) =>
            // If create success, publish it
            this.apiPlanV2Service.publish(api.id, plan.id).pipe(
              // If publish failed, return error message as result
              catchError((err) => {
                return of(`Error while publishing plan "${plan.name}": ${err.error?.message}.`);
              }),
            ),
          ),
          // If create failed, return error message as result
          catchError((err) => {
            return of(`Error while creating plan "${plan.name}": ${err.error?.message}.`);
          }),
        ),
      ),
      toArray(),
      map((results: (string | PlanV4)[]) => {
        return results.reduce((result, val) => {
          const isErrorMessage = isString(val);

          // If error message, add it to errorMessages
          if (isErrorMessage) {
            return {
              ...result,
              errorMessages: [...(result?.errorMessages ?? []), val],
            };
          }

          // If Plan, add it to result
          return {
            ...result,
            result: {
              ...result.result,
              plans: [...(result.result?.plans ?? []), val],
            },
          };
        }, previousResult);
      }),
    );
  }

  private startApi$(previousResult: Result): Observable<Result> {
    if (!previousResult.apiCreationPayload.deploy) {
      return of(previousResult);
    }

    return this.apiV2Service.start(previousResult.result.api.id).pipe(
      map(() => ({ ...previousResult, result: { ...previousResult.result, deployed: true } })),
      catchError((err) => {
        return of({
          ...previousResult,
          errorMessages: [...previousResult.errorMessages, `Error while starting API: ${err.error?.message}.`],
        });
      }),
    );
  }

  private askForReview$(previousResult: Result): Observable<Result> {
    if (!previousResult.apiCreationPayload.askForReview) {
      return of(previousResult);
    }

    return this.apiReviewV2Service.ask(previousResult.result.api.id).pipe(
      map(() => previousResult),
      catchError((err) => {
        return of({
          ...previousResult,
          errorMessages: [...previousResult.errorMessages, `Error while asking for review: ${err.error?.message}.`],
        });
      }),
    );
  }

  private getListenerSpecificConfig(listenerType: ListenerType, apiCreationPayload: ApiCreationPayload): object {
    switch (listenerType) {
      case 'HTTP':
        return { paths: apiCreationPayload.paths };
      case 'TCP':
        return { hosts: apiCreationPayload.hosts.map((host) => host.host) };
      case 'KAFKA':
        return { host: apiCreationPayload.host?.host, port: apiCreationPayload.port?.port };
      default:
        return {};
    }
  }
}
