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

export class ApiDocumentationV4NewtAiDialogHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-v4-newt-ai-dialog';

  getCancelButton = this.locatorFor(MatButtonHarness.with({ text: 'Cancel' }));
  getGenerateButton = this.locatorFor(MatButtonHarness.with({ selector: '[type=submit]' }));

  async clickCancel(): Promise<void> {
    return this.getCancelButton().then(async (b) => b.click());
  }

  async clickGenerate(): Promise<void> {
    return this.getGenerateButton().then(async (b) => b.click());
  }

  async getListItems(): Promise<string[]> {
    const items = await this.locatorForAll('mat-list-item')();
    return Promise.all(items.map((item) => item.text()));
  }
}
