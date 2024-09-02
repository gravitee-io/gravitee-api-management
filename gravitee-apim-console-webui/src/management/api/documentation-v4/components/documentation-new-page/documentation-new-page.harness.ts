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
import { GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';

export class DocumentationNewPageHarness extends ComponentHarness {
  public static hostSelector = 'documentation-new-page';

  private nextButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Next' }));
  private previousButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Previous' }));
  private sourceTypeSelectionInlineHarness = this.locatorFor(
    GioFormSelectionInlineHarness.with({ selector: '.stepper__content__source__type' }),
  );
  private sourceSelectionInlineHarness = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '.stepper__content__source' }));
  private httpUrlLocator = this.locatorFor(MatInputHarness.with({ selector: '[id*="url"]' }));

  async getNextButton() {
    return this.nextButtonLocator();
  }

  async getPreviousButton() {
    return this.previousButtonLocator();
  }

  async getHttpUrlHarness() {
    return await this.httpUrlLocator();
  }

  async getSourceTypeOptions() {
    return Promise.all(await this.sourceTypeSelectionInlineHarness().then(async (radioGroup) => await radioGroup.getSelectionCards()));
  }

  async selectSourceType(value: string) {
    return this.sourceTypeSelectionInlineHarness().then((radioGroup) => radioGroup.select(value));
  }

  async getSourceOptions() {
    return Promise.all(await this.sourceSelectionInlineHarness().then(async (radioGroup) => await radioGroup.getSelectionCards()));
  }

  async selectSource(value: string) {
    return this.sourceSelectionInlineHarness().then((radioGroup) => radioGroup.select(value));
  }
}
