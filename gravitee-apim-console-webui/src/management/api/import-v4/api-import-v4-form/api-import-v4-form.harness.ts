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
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { DivHarness } from '@gravitee/ui-particles-angular/testing';
import { GioFormFilePickerInputHarness, GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';

export class ApiImportV4FormHarness extends ComponentHarness {
  static hostSelector = 'api-import-v4-form';

  private readonly getRoot = this.locatorForOptional(DivHarness.with({ selector: '[data-testid="api-import-v4-form-root"]' }));
  private readonly getOptionsStep = this.locatorForOptional(DivHarness.with({ selector: '.options-step' }));

  private readonly getFormatSelectGroup = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '[formControlName="format"]' }));
  private readonly getSourceSelectGroup = this.locatorFor(GioFormSelectionInlineHarness.with({ selector: '[formControlName="source"]' }));
  private readonly getFilePicker = this.locatorFor(GioFormFilePickerInputHarness.with({ ancestor: 'api-import-file-picker' }));
  private readonly getRemoteUrlInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="remoteUrl"]' }));
  private readonly getAuthorizationHeaderInput = this.locatorFor(
    MatInputHarness.with({ selector: '[formControlName="authorizationHeader"]' }),
  );
  private readonly getDocumentationToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[formControlName="withDocumentation"]' }),
  );
  private readonly getOasValidationToggleOptional = this.locatorForOptional(
    MatSlideToggleHarness.with({ selector: '[formControlName="withOASValidationPolicy"]' }),
  );

  public async hasRoot(): Promise<boolean> {
    return (await this.getRoot()) !== null;
  }

  public async hasOptionsStep(): Promise<boolean> {
    return (await this.getOptionsStep()) !== null;
  }

  public async selectFormat(format: string): Promise<void> {
    const group = await this.getFormatSelectGroup();
    return group.select(format);
  }

  public async selectSource(source: string): Promise<void> {
    const group = await this.getSourceSelectGroup();
    return group.select(source);
  }

  public async setRemoteUrl(url: string): Promise<void> {
    const input = await this.getRemoteUrlInput();
    return input.setValue(url);
  }

  public async setAuthorizationHeader(value: string): Promise<void> {
    const input = await this.getAuthorizationHeaderInput();
    return input.setValue(value);
  }

  public async pickFiles(files: File[]): Promise<void> {
    const picker = await this.getFilePicker();
    return picker.dropFiles(files);
  }

  /** Clicks the enabled primary "Next" in the active stepper footer (format → file source → options). */
  public async clickNext(): Promise<void> {
    for (const ancestor of ['.select-api-format__actions', '.configure-file-source__actions', '.options-step__actions']) {
      const btn = await this.locatorForOptional(MatButtonHarness.with({ ancestor, text: 'Next' }))();
      if (btn && !(await btn.isDisabled())) {
        await btn.click();
        return;
      }
    }
    throw new Error('No enabled Next button found in api-import-v4-form stepper');
  }

  public async clickBack(): Promise<void> {
    const btn = await this.locatorFor(MatButtonHarness.with({ ancestor: '.configure-file-source__actions', text: 'Back' }))();
    return btn.click();
  }

  public async clickPrevious(): Promise<void> {
    for (const ancestor of ['.options-step__actions', '.review-import-step__actions']) {
      const btn = await this.locatorForOptional(MatButtonHarness.with({ ancestor, text: 'Previous' }))();
      if (btn && !(await btn.isDisabled())) {
        await btn.click();
        return;
      }
    }
    throw new Error('No enabled Previous button found');
  }

  /** Clicks the review step primary action ("Import API" or "Update API"). */
  public async clickImport(): Promise<void> {
    const btn = await this.locatorFor(
      MatButtonHarness.with({ ancestor: '.review-import-step__actions', text: /^(Import API|Update API)$/ }),
    )();
    return btn.click();
  }

  public async isImportButtonDisabled(): Promise<boolean> {
    const btn = await this.locatorFor(
      MatButtonHarness.with({ ancestor: '.review-import-step__actions', text: /^(Import API|Update API)$/ }),
    )();
    return btn.isDisabled();
  }

  public async isConfigureSourceNextDisabled(): Promise<boolean> {
    const btn = await this.locatorForOptional(MatButtonHarness.with({ ancestor: '.configure-file-source__actions', text: 'Next' }))();
    return btn ? btn.isDisabled() : true;
  }

  public async isDocumentationImportSelected(): Promise<boolean> {
    return (await this.getDocumentationToggle()).isChecked();
  }

  public async isDocumentationImportDisabled(): Promise<boolean> {
    return (await this.getDocumentationToggle()).isDisabled();
  }

  public async toggleDocumentationImport(): Promise<void> {
    return (await this.getDocumentationToggle()).toggle();
  }

  public async isOasValidationPolicyImportPresent(): Promise<boolean> {
    return (await this.getOasValidationToggleOptional()) !== null;
  }

  public async isOasValidationPolicyImportSelected(): Promise<boolean> {
    const toggle = await this.getOasValidationToggleOptional();
    if (!toggle) {
      throw new Error('OpenAPI validation toggle is not in the DOM');
    }
    return toggle.isChecked();
  }

  public async isOasValidationPolicyImportDisabled(): Promise<boolean> {
    const toggle = await this.getOasValidationToggleOptional();
    if (!toggle) {
      throw new Error('OpenAPI validation toggle is not in the DOM');
    }
    return toggle.isDisabled();
  }

  public async toggleOasValidationPolicyImport(): Promise<void> {
    const toggle = await this.getOasValidationToggleOptional();
    if (!toggle) {
      throw new Error('OpenAPI validation toggle is not in the DOM');
    }
    return toggle.toggle();
  }
}
