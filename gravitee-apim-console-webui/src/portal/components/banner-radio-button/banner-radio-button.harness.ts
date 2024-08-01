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
import { ComponentHarness, ComponentHarnessConstructor, HarnessPredicate } from '@angular/cdk/testing';
import { MatRadioButtonHarness, RadioButtonHarnessFilters } from '@angular/material/radio/testing';
interface BannerRadioButtonHarnessFilters extends RadioButtonHarnessFilters {
  title?: string;
}
export class BannerRadioButtonHarness extends ComponentHarness {
  static readonly hostSelector = 'banner-radio-button';

  private locateRadioButton = this.locatorFor(MatRadioButtonHarness);
  private locateTitle = this.locatorFor('h3');

  static with<T extends BannerRadioButtonHarness>(
    this: ComponentHarnessConstructor<T>,
    options: BannerRadioButtonHarnessFilters = {},
  ): HarnessPredicate<T> {
    return new HarnessPredicate(this, options).addOption('title', options.title, (harness, text) =>
      HarnessPredicate.stringMatches(harness.getTitle(), text),
    );
  }

  public async getTitle(): Promise<string> {
    return await this.locateTitle().then((el) => el.text());
  }

  public async getRadioButton(): Promise<MatRadioButtonHarness> {
    return await this.locateRadioButton();
  }
}
