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

export class Step6SummaryHarness extends ComponentHarness {
  static hostSelector = 'step-6-summary';

  protected getStepById = (stepNumber: number) => this.locatorFor(`#step-${stepNumber}`);
  protected getButtonByStepId = (stepNumber: number) =>
    this.locatorFor(MatButtonHarness.with({ selector: `#step-${stepNumber} button`, text: 'Change' }));

  protected getButtonCreateMyApi = this.locatorFor(MatButtonHarness.with({ text: /Create my API/ }));
  protected getButtonDeployMyApi = this.locatorFor(MatButtonHarness.with({ text: /Deploy my API/ }));

  async getStepSummaryTextContent(stepNumber: number) {
    return this.getStepById(stepNumber)().then((el) => el.text());
  }

  async clickChangeButton(stepNumber: number) {
    const button: MatButtonHarness = await this.getButtonByStepId(stepNumber)();
    await button.click();
  }

  async clickCreateMyApiButton(): Promise<void> {
    const button: MatButtonHarness = await this.getButtonCreateMyApi();
    await button.click();
  }

  async clickDeployMyApiButton(): Promise<void> {
    const button: MatButtonHarness = await this.getButtonDeployMyApi();
    await button.click();
  }
}
