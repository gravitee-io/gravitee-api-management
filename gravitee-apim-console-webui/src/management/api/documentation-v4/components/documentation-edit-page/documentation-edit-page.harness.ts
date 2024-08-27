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
import { MatTabHarness } from '@angular/material/tabs/testing';

export class DocumentationEditPageHarness extends ComponentHarness {
  public static hostSelector = 'documentation-edit-page';

  private locateDeleteButton = this.locatorForOptional(MatButtonHarness.with({ text: 'Delete page' }));
  private locatePublishChangesButton = this.locatorFor(MatButtonHarness.with({ text: 'Publish changes' }));
  private locateConfigurePageTab = this.locatorFor(MatTabHarness.with({ label: 'Configure Page' }));
  private locateContentTab = this.locatorFor(MatTabHarness.with({ label: 'Content' }));

  async getDeleteButton(): Promise<MatButtonHarness | undefined> {
    return await this.locateDeleteButton();
  }

  async getPublishChangesButton(): Promise<MatButtonHarness> {
    return await this.locatePublishChangesButton();
  }

  async openConfigurePageTab(): Promise<void> {
    return await this.locateConfigurePageTab().then((tab) => tab.select());
  }

  async openContentTab(): Promise<void> {
    return await this.locateContentTab().then((tab) => tab.select());
  }
}
