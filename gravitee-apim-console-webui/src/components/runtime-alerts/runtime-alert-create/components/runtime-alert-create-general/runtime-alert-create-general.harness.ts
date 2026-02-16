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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class RuntimeAlertCreateGeneralHarness extends ComponentHarness {
  static readonly hostSelector = 'runtime-alert-create-general';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  private getEnabledSlide = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="enabled"]' }));
  private getRuleSelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="rule"]' }));
  private getSeveritySelect = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="severity"]' }));
  private getDescriptionInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="description"]' }));

  public async setName(name: string) {
    return this.getNameInput().then(input => input.setValue(name));
  }

  public async getRulesOptions(): Promise<string[]> {
    return this.getRuleSelect().then(async select => {
      await select.open();

      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectRule(rule: string) {
    return this.getRuleSelect().then(select => select.clickOptions({ text: rule }));
  }

  public async getSeverityOptions(): Promise<string[]> {
    return this.getSeveritySelect().then(async select => {
      await select.open();

      const options = await select.getOptions();
      return Promise.all(options.map(async o => o.getText()));
    });
  }

  public async selectSeverity(severity: string) {
    return this.getSeveritySelect().then(select => select.clickOptions({ text: severity }));
  }

  public async setDescription(name: string) {
    return this.getDescriptionInput().then(input => input.setValue(name));
  }

  public async toggleEnabled() {
    return this.getEnabledSlide().then(slide => slide.toggle());
  }
}
