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

import { UIRouterState } from '../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-creation-v4-confirmation',
  template: require('./api-creation-v4-confirmation.component.html'),
  styles: [require('./api-creation-v4-confirmation.component.scss')],
})
export class ApiCreationV4ConfirmationComponent {
  constructor(@Inject(UIRouterState) readonly ajsState: StateService) {}

  navigate(urlState: string) {
    return this.ajsState.go(urlState, { apiId: this.ajsState.params.apiId }, { reload: true });
  }
}
