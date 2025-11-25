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

export class WebhookSettingsDialogHarness extends ComponentHarness {
  static hostSelector = 'webhook-settings-dialog';

  private readonly titleLocator = this.locatorForOptional('[mat-dialog-title]');
  private readonly contentLocator = this.locatorForOptional('mat-dialog-content');
  private readonly cancelButtonLocator = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="webhook-settings-dialog-cancel"]' }),
  );
  private readonly saveButtonLocator = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="webhook-settings-dialog-save"]' }),
  );

  async getTitleText(): Promise<string | null> {
    const title = await this.titleLocator();
    return title?.text() ?? null;
  }

  async isLoading(): Promise<boolean> {
    const content = await this.contentLocator();
    if (!content) {
      return false;
    }
    const text = await content.text();
    return text.toLowerCase().includes('loading settings');
  }

  async clickClose(): Promise<void> {
    const cancelButton = await this.cancelButtonLocator();
    if (!cancelButton) {
      throw new Error('Cancel button not found in webhook settings dialog.');
    }
    await cancelButton.click();
  }

  async clickSave(): Promise<void> {
    const saveButton = await this.saveButtonLocator();
    if (!saveButton) {
      throw new Error('Save button not found in webhook settings dialog.');
    }
    await saveButton.click();
  }
}
