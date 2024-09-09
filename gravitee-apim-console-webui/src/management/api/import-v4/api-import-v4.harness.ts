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
import { GioFormFilePickerInputHarness, GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class ApiImportV4Harness extends ComponentHarness {
  static hostSelector = 'api-api-import-v4';

  private getFormatSelectGroup = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '[formControlName="format"]' }));
  private getSourceSelectGroup = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '[formControlName="source"]' }));
  private getFilePicker = this.locatorFor(GioFormFilePickerInputHarness);
  private getSaveButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Import API"]' }));
  private getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[aria-label="Cancel"]' }));
  private getFormatErrorBanner = this.locatorForOptional(DivHarness.with({ selector: '.banner' }));
  private getImportDocumentationToggle = this.locatorFor(MatSlideToggleHarness.with({ selector: '[formControlName="withDocumentation"]' }));
  private getImportOASValidationPolicyToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[formControlName="withOASValidationPolicy"]' }),
  );

  public async save() {
    return this.getSaveButton().then((btn) => btn.click());
  }

  public async isSaveDisabled() {
    return this.getSaveButton().then((btn) => btn.isDisabled());
  }

  public async cancel() {
    return this.getCancelButton().then((btn) => btn.click());
  }

  public async selectFormat(format: string) {
    return this.getFormatSelectGroup().then((group) => group.select(format));
  }

  public async selectSource(source: string) {
    return this.getSourceSelectGroup().then((group) => group.select(source));
  }

  public async pickFiles(files: File[]) {
    return this.getFilePicker().then((filePicker) => filePicker.dropFiles(files));
  }

  public async isFormatErrorBannerDisplayed() {
    return this.getFormatErrorBanner().then((banner) => banner !== null);
  }

  public async isDocumentationImportSelected() {
    return this.getImportDocumentationToggle().then((toggle) => toggle.isChecked());
  }

  public async isDocumentationImportDisabled() {
    return this.getImportDocumentationToggle().then((toggle) => toggle.isDisabled());
  }

  public async toggleDocumentationImport() {
    return this.getImportDocumentationToggle().then((toggle) => toggle.toggle());
  }

  public async isOASValidationPolicyImportSelected() {
    return this.getImportOASValidationPolicyToggle().then((toggle) => toggle.isChecked());
  }

  public async isOASValidationPolicyImportDisabled() {
    return this.getImportOASValidationPolicyToggle().then((toggle) => toggle.isDisabled());
  }

  public async toggleOASValidationPolicyImport() {
    return this.getImportOASValidationPolicyToggle().then((toggle) => toggle.toggle());
  }
}
