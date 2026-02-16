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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatIconHarness } from '@angular/material/icon/testing';

export class StepperMenuStepHarness extends ComponentHarness {
  static hostSelector = 'stepper-menu-step';

  /**
   * Gets a `HarnessPredicate` that can be used to search for a `StepperMenuStepHarness` that meets
   * certain criteria.
   *
   * @param options Options for filtering which input instances are considered a match.
   * @return a `HarnessPredicate` configured with the given options.
   */
  public static with(options: BaseHarnessFilters = {}): HarnessPredicate<StepperMenuStepHarness> {
    return new HarnessPredicate(StepperMenuStepHarness, options);
  }

  protected getIconElement = this.locatorForOptional(MatIconHarness);
  protected getHeaderElement = this.locatorFor('.stepper-menu-step__header__info');
  protected getStepNumberElement = this.locatorFor('.stepper-menu-step__header__info__number');
  protected getStepTitleElement = this.locatorFor('.stepper-menu-step__header__info__title');
  protected getInvalidStepTitleElement = this.locatorForAll('.stepper-menu-step__header__info__title-invalid');

  async getStepNumber(): Promise<string> {
    return this.getStepNumberElement().then(elt => elt.text());
  }
  async getStepTitle(): Promise<string> {
    return this.getStepTitleElement().then(elt => elt.text());
  }
  async hasStepIcon(): Promise<boolean> {
    return this.getIconElement().then(icon => icon != null);
  }
  async getStepIconName(): Promise<string> {
    return this.getIconElement().then(icon => {
      return icon.getName();
    });
  }

  async getStepContent(): Promise<string> {
    return this.locatorFor('.stepper-menu-step__content')().then(elt => elt.text());
  }

  async isInvalidStep(): Promise<boolean> {
    const invalidElements = await this.getInvalidStepTitleElement();
    return invalidElements.length > 0;
  }

  async clickOnStepHeader() {
    return this.getHeaderElement().then(elt => elt.click());
  }
}
