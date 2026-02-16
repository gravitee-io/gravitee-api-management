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
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiRuntimeLogsMoreFiltersFormHarness } from './components';

export class ApiRuntimeLogsMoreFiltersHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-more-filters';

  public getClearAllButton = this.locatorFor(MatButtonHarness.with({ text: 'Clear all' }));
  public getApplyButton = this.locatorFor(MatButtonHarness.with({ text: 'Show results' }));
  public getCloseButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Close"]' }));
  public getForm = this.locatorFor(ApiRuntimeLogsMoreFiltersFormHarness);

  async getPeriodSelectInput() {
    return this.getForm().then(form => form.getPeriodSelectInput());
  }
  async getFromInput() {
    return this.getForm().then(form => form.getFromInput());
  }
  async getToInput() {
    return this.getForm().then(form => form.getToInput());
  }

  async getStatusesChips() {
    return this.getForm().then(form => form.getStatusesChips());
  }

  async getApplicationField() {
    return this.getForm().then(form => form.getApplicationField());
  }

  async getApplicationAutocomplete() {
    return this.getForm().then(form => form.getApplicationAutocomplete());
  }
}
