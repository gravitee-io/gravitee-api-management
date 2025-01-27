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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';

export class DashboardFiltersBarHarness extends ComponentHarness {
  static readonly hostSelector: string = 'app-dashboard-filters-bar';

  private refreshButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=refresh-button]' }));
  public applyButtonLocator = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=apply-button]' }));

  public fromInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="from"]' }));
  public toInputLocator = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="to"]' }));

  async setFromDate(date: string) {
    return this.fromInputLocator().then((input: MatInputHarness) => input.setValue(date));
  }

  async setToDate(date: string) {
    return this.toInputLocator().then((input: MatInputHarness) => input.setValue(date));
  }

  async refreshClick(): Promise<void> {
    return (await this.refreshButtonLocator()).click();
  }

  async applyClick(): Promise<void> {
    return (await this.applyButtonLocator()).click();
  }

  public matSelectLocator = this.locatorFor(MatSelectHarness);
}
