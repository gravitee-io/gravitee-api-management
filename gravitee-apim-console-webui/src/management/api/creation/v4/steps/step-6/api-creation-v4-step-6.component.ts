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
import { takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';
import { kebabCase } from 'lodash';
import { StateService } from '@uirouter/core';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiV4Service } from '../../../../../../services-ngx/api-v4.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { fakeNewApiEntity } from '../../../../../../entities/api-v4/NewApiEntity.fixture';
import { HttpListener } from '../../../../../../entities/api-v4';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { UIRouterState } from '../../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-creation-v4-step-6',
  template: require('./api-creation-v4-step-6.component.html'),
  styles: [require('./api-creation-v4-step-6.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class ApiCreationV4Step6Component implements OnInit {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public currentStepPayload: ApiCreationPayload;

  constructor(
    private readonly stepService: ApiCreationStepService,
    private readonly snackBarService: SnackBarService,
    private readonly apiV4Service: ApiV4Service,
    @Inject(UIRouterState) readonly ajsState: StateService,
  ) {}

  ngOnInit(): void {
    this.currentStepPayload = this.stepService.payload;
  }

  createApi(deploy: boolean) {
    const apiCreationPayload = this.stepService.payload;

    this.apiV4Service
      .create(
        // Note : WIP 🚧
        // Use the fakeNewApiEntity to create a new API temporarily
        // The real API creation will be done when we complete other api creation steps
        fakeNewApiEntity((api) => {
          const listener = api.listeners[0] as HttpListener;
          listener.paths = [{ path: `/fake/${kebabCase(apiCreationPayload.name + '-' + apiCreationPayload.version)}` }];
          return {
            ...api,
            name: apiCreationPayload.name,
          };
        }),
      )
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(
          (api) => {
            this.snackBarService.success(`API ${deploy ? 'deployed' : 'created'} successfully!`);
            this.ajsState.go('management.apis.create-v4-confirmation', { apiId: api.id });
          },
          (err) => {
            this.snackBarService.error(err.error?.message ?? `An error occurred while ${deploy ? 'deploying' : 'creating'} the API.`);
            return EMPTY;
          },
        ),
      )
      .subscribe();
  }

  deployApi(): void {
    // TODO: send info to correct endpoint to create and publish the new API
  }

  onChangeStepInfo(stepLabel: string) {
    this.stepService.goToStepLabel(stepLabel);
  }

  getEntrypointIconName(id: string): string {
    if (id.startsWith('sse')) {
      return 'cloud-server';
    }
    if (id.startsWith('http')) {
      return 'language';
    }
    if (id.startsWith('webhook')) {
      return 'webhook';
    }
    if (id.startsWith('websocket')) {
      return 'websocket';
    }
    return 'layers';
  }

  getEndpointIconName(id: string) {
    if (id.startsWith('kafka')) {
      return 'kafka';
    }
    if (id.startsWith('mock')) {
      return 'page';
    }
    if (id.startsWith('mqtt')) {
      return 'mqtt';
    }
    return 'layers';
  }
}
