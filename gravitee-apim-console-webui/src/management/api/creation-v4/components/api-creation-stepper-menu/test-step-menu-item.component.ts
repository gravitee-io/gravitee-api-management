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

import { MENU_ITEM_PAYLOAD } from './api-creation-stepper-menu.component';

import { ApiCreationPayload } from '../../models/ApiCreationPayload';

@Component({
  selector: 'test-step-menu-item',
  template: `Payload Json : <br />
    {{ menuItemPayload | json }}`,
  standalone: false,
})
export class TestStepMenuItemComponent {
  constructor(@Inject(MENU_ITEM_PAYLOAD) public readonly menuItemPayload: ApiCreationPayload) {}
}
