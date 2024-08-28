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
import { MatButtonHarness } from '@angular/material/button/testing';
import { GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';
import { MatButtonToggleGroupHarness } from '@angular/material/button-toggle/testing';

export class MenuLinkAddDialogHarness extends ComponentHarness {
  public static hostSelector = 'menu-link-add-dialog';

  protected getNameField = this.locatorFor(MatInputHarness.with({ selector: '#name' }));
  protected getTargetField = this.locatorFor(MatInputHarness.with({ selector: '#target' }));
  protected getTypeField = this.locatorFor(MatButtonToggleGroupHarness.with({ selector: '#type' }));
  protected getVisibilitySelect = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '#visibility' }));
  protected getSaveButton = this.locatorFor(MatButtonHarness.with({ text: 'Add Link' }));

  async saveButtonExists(): Promise<boolean> {
    return this.getSaveButton().then((btn) => (btn ? true : false));
  }

  async saveButtonEnabled(): Promise<boolean> {
    return this.getSaveButton()
      .then((btn) => btn.isDisabled())
      .then((disabled) => !disabled);
  }

  async clickSave(): Promise<void> {
    return this.getSaveButton().then((btn) => btn.click());
  }

  async nameFieldExists() {
    return this.getNameField()
      .then(() => true)
      .catch(() => false);
  }

  async typeFieldExists() {
    return this.getTypeField()
      .then(() => true)
      .catch(() => false);
  }

  async targetFieldExists() {
    return this.getTargetField()
      .then(() => true)
      .catch(() => false);
  }

  async visibilitySelectExists() {
    return this.getVisibilitySelect()
      .then(() => true)
      .catch(() => false);
  }

  async fillOutName(name: string) {
    return this.getNameField().then((input) => input.setValue(name));
  }
  async selectType(type: string) {
    return this.getTypeField()
      .then((group) => group.getToggles({ text: type }))
      .then((button) => button[0].toggle());
  }
  async fillOutTarget(target: string) {
    return this.getTargetField().then((input) => input.setValue(target));
  }
  async selectVisibility(visibility: string) {
    await this.getVisibilitySelect().then((select) => select.select(visibility));
  }
}
