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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class WebhookSettingsDialogHarness extends ComponentHarness {
  static hostSelector = 'webhook-settings-dialog';

  private readonly titleLocator = this.locatorForOptional('[mat-dialog-title]');
  private readonly contentLocator = this.locatorForOptional('mat-dialog-content');
  private readonly saveBarLocator = this.locatorForOptional(GioSaveBarHarness);

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
    // The discard button (reset action) is provided by gio-save-bar
    // The save bar only appears when the form has unsaved changes
    const saveBar = await this.saveBarLocator();
    if (!saveBar) {
      throw new Error('Save bar not found in webhook settings dialog. Make sure the form has unsaved changes.');
    }
    await saveBar.clickReset();
  }
}
