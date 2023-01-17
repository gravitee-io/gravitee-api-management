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

import { API_CREATION_PAYLOAD } from '../../models/ApiCreationStepperService';
import { ApiCreationPayload } from '../../models/ApiCreationPayload';

@Component({
  selector: 'api-creation-v4-step-6',
  template: require('./api-creation-v4-step-6.component.html'),
  styles: [require('./api-creation-v4-step-6.component.scss'), require('../../api-creation-v4.component.scss')],
})
export class ApiCreationV4Step6Component {
  constructor(@Inject(API_CREATION_PAYLOAD) readonly currentStepPayload: ApiCreationPayload) {}

  createApi(): void {
    // TODO: send info to correct endpoint to create, not publish, the new API
  }

  deployApi(): void {
    // TODO: send info to correct endpoint to create and publish the new API
  }
}
