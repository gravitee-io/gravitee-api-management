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
import { ComponentHarness } from '@angular/cdk/testing';

import { ApiCreationV4FormSelectionListHarness } from '../api-creation-v4-form-selection-list.harness';

export class ApiCreationV4Step3Harness extends ComponentHarness {
  static hostSelector = 'api-creation-v4-step-3';
  private readonly form = this.locatorFor(ApiCreationV4FormSelectionListHarness);

  async getEndpointsForm(): Promise<ApiCreationV4FormSelectionListHarness> {
    return this.form();
  }

  async clickPrevious() {
    return this.form().then((f) => f.clickButtonByText('Previous'));
  }

  async clickValidateEndpoints() {
    return this.form().then((f) => f.clickButtonByText('Select my endpoints'));
  }
}
