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
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';

export class PublishNavigationItemDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'publish-navigation-item-dialog';

  private readonly locateConfirmButton = this.locatorFor(MatButtonHarness.with({ text: /Publish|Unpublish/ }));
  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  private readonly locateContent = this.locatorFor('.publish-navigation-item-dialog');
  private readonly locatePropagationCheckbox = this.locatorForOptional(
    MatCheckboxHarness.with({ selector: '[data-testid="propagate-publish-to-children-checkbox"]' }),
  );

  async confirm(): Promise<void> {
    const confirmButton = await this.locateConfirmButton();
    return confirmButton.click();
  }

  async cancel(): Promise<void> {
    const cancelButton = await this.locateCancelButton();
    return cancelButton.click();
  }

  async getContentText(): Promise<string> {
    const content = await this.locateContent();
    return content.text();
  }

  async isPropagationCheckboxVisible(): Promise<boolean> {
    const propagationCheckbox = await this.locatePropagationCheckbox();
    return propagationCheckbox !== null;
  }

  async isPropagationCheckboxChecked(): Promise<boolean> {
    const propagationCheckbox = await this.locatePropagationCheckbox();
    if (!propagationCheckbox) {
      throw new Error('Propagation checkbox is not visible');
    }
    return propagationCheckbox.isChecked();
  }

  async checkPropagationCheckbox(): Promise<void> {
    const propagationCheckbox = await this.locatePropagationCheckbox();
    if (!propagationCheckbox) {
      throw new Error('Propagation checkbox is not visible');
    }
    return propagationCheckbox.check();
  }
}
