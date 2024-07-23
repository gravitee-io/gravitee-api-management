/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { BaseHarnessFilters, ComponentHarnessConstructor, ContentContainerComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatCardHarness } from '@angular/material/card/testing';
import { MatRadioButtonHarness } from '@angular/material/radio/testing';

export interface RadioCardHarnessFilters extends BaseHarnessFilters {
  title?: string;
  selected?: boolean;
}

export class RadioCardHarness extends ContentContainerComponentHarness {
  public static hostSelector = 'app-radio-card';
  protected locateTitle = this.locatorFor('[aria-label="Title"]');
  protected locateRadioBtn = this.locatorFor(MatRadioButtonHarness);
  protected locateDisabledCard = this.locatorFor(MatCardHarness.with({ selector: '.disabled' }));

  static with<T extends RadioCardHarness>(
    this: ComponentHarnessConstructor<T>,
    options: RadioCardHarnessFilters = {},
  ): HarnessPredicate<T> {
    return new HarnessPredicate(this, options).addOption('title', options.title, (harness, text) =>
      HarnessPredicate.stringMatches(harness.getTitle(), text),
    );
  }

  public async getTitle(): Promise<string> {
    return await this.locateTitle().then(div => div.text());
  }

  public async isSelected(): Promise<boolean> {
    return await this.locateRadioBtn().then(radioBtn => radioBtn.isChecked());
  }

  public async select(): Promise<void> {
    return await this.locateRadioBtn().then(radioBtn => radioBtn.check());
  }

  public async isDisabled(): Promise<boolean> {
    return await this.locateDisabledCard()
      .then(_ => true)
      .catch(_ => false);
  }
}
