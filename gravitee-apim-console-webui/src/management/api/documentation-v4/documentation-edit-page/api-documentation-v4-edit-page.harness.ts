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
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

import { ApiDocumentationV4VisibilityHarness } from '../components/api-documentation-v4-visibility/api-documentation-v4-visibility.harness';

export class ApiDocumentationV4EditPageHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-edit-page';

  private nextButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Next' }));
  private deleteButtonLocator = this.locatorFor(MatButtonHarness.with({ text: 'Delete page' }));
  private nameInputLocator = this.locatorFor(MatInputHarness);
  private visibilityHarness = this.locatorFor(ApiDocumentationV4VisibilityHarness);
  private selectAccessGroupsHarness = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="accessControlGroups"]' }));
  private toggleExcludeGroups = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="excludeGroups"]' }));
  private sourceSelectionInlineHarness = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '.stepper__content__source' }));
  private httpUrlLocator = this.locatorFor(MatInputHarness.with({ selector: '[id*="url"]' }));

  async getNextButton() {
    return this.nextButtonLocator();
  }

  async getDeleteButton() {
    return this.deleteButtonLocator().catch((_) => undefined);
  }

  async getName(): Promise<string> {
    return this.nameInputLocator().then((input) => input.getValue());
  }

  async setName(name: string) {
    return this.nameInputLocator().then((input) => input.setValue(name));
  }

  async nameIsDisabled(): Promise<boolean> {
    return this.nameInputLocator().then((input) => input.isDisabled());
  }

  async checkVisibility(visibility: 'PRIVATE' | 'PUBLIC') {
    const visibilityHarness = await this.visibilityHarness();
    await visibilityHarness.select(visibility);
  }

  async getVisibility() {
    return this.visibilityHarness().then((harness) => harness.getValue());
  }

  async visibilityIsDisabled(): Promise<boolean> {
    return this.visibilityHarness().then((harness) => harness.formIsDisabled());
  }

  async getAccessControlGroups(): Promise<MatSelectHarness | null> {
    return this.selectAccessGroupsHarness().catch((_) => null);
  }
  async getExcludeGroups(): Promise<MatSlideToggleHarness | null> {
    return this.toggleExcludeGroups().catch((_) => null);
  }

  async getSourceSelectionInlineHarness() {
    return await this.sourceSelectionInlineHarness();
  }

  async getHttpUrlHarness() {
    return await this.httpUrlLocator();
  }

  async getSourceOptions() {
    return Promise.all(await this.sourceSelectionInlineHarness().then(async (radioGroup) => await radioGroup.getSelectionCards()));
  }

  async selectSource(value: string) {
    return this.sourceSelectionInlineHarness().then((radioGroup) => radioGroup.select(value));
  }
}
