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

import { Component, Inject } from '@angular/core';
import { StateService } from '@uirouter/core';

import { UIRouterState } from '../../../../../ajs-upgraded-providers';
import { ApiDetails } from '../models/api-details';

@Component({
  selector: 'api-creation-v4',
  template: require('./api-creation-v4.component.html'),
  styles: [require('./api-creation-v4.component.scss')],
})
export class ApiCreationV4Component {
  constructor(@Inject(UIRouterState) readonly ajsState: StateService) {}
  public currentStep = 1;

  private _apiDetails: ApiDetails;
  set apiDetails(val: ApiDetails) {
    this._apiDetails = val;
    this.currentStep++;
  }
  get apiDetails() {
    return this._apiDetails;
  }

  private _selectedEntrypoint: string[];
  set selectedEntrypoints(val: string[]) {
    this._selectedEntrypoint = val;
    this.currentStep++;
  }
  get selectedEntrypoints() {
    return this._selectedEntrypoint;
  }

  goToPreviousStep() {
    this.currentStep--;
  }

  exit() {
    this.ajsState.go('management.apis.new');
  }
}
