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

import { NavigationItemSourceEditorHarness } from '../navigation-item-source-editor/navigation-item-source-editor.harness';

export class ImportNavigationDialogHarness extends ComponentHarness {
  static hostSelector = 'import-navigation-dialog';

  private readonly locateTitleInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="import-title-input"]' }));
  private readonly locateImportButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="import-navigation-button"]' }));
  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  private readonly locateSourceEditor = this.locatorFor(NavigationItemSourceEditorHarness);

  async setTitle(title: string): Promise<void> {
    const input = await this.locateTitleInput();
    return input.setValue(title);
  }

  async getSourceEditor(): Promise<NavigationItemSourceEditorHarness> {
    return this.locateSourceEditor();
  }

  async isImportButtonDisabled(): Promise<boolean> {
    const button = await this.locateImportButton();
    return button.isDisabled();
  }

  async clickImportButton(): Promise<void> {
    const button = await this.locateImportButton();
    return button.click();
  }

  async clickCancelButton(): Promise<void> {
    const button = await this.locateCancelButton();
    return button.click();
  }
}
