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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

import { RuntimeAlertCreateGeneralHarness } from './components/runtime-alert-create-general/runtime-alert-create-general.harness';
import { RuntimeAlertCreateTimeframeHarness } from './components/runtime-alert-create-timeframe/runtime-alert-create-timeframe.harness';
import { RuntimeAlertCreateConditionsHarness } from './components/runtime-alert-create-conditions/runtime-alert-create-conditions.harness';
import { RuntimeAlertCreateFiltersHarness } from './components/runtime-alert-create-filters/runtime-alert-create-filters.harness';

export class RuntimeAlertCreateHarness extends ComponentHarness {
  static readonly hostSelector = 'runtime-alert-create';

  public getGeneralFormHarness = this.locatorFor(RuntimeAlertCreateGeneralHarness);
  public getTimeframeFormHarness = this.locatorFor(RuntimeAlertCreateTimeframeHarness);
  public getConditionsFormHarness = this.locatorFor(RuntimeAlertCreateConditionsHarness);
  public getFiltersFormHarness = this.locatorFor(RuntimeAlertCreateFiltersHarness);
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  public async createClick() {
    return this.getSaveBar().then(saveBar => saveBar.clickSubmit());
  }

  public async isSubmitInvalid() {
    return this.getSaveBar().then(saveBar => saveBar.isSubmitButtonInvalid());
  }
}
