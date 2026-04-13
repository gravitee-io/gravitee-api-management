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
import { BaseHarnessFilters, ComponentHarness, HarnessPredicate } from '@angular/cdk/testing';
import { MatDialogSection } from '@angular/material/dialog/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export interface KafkaClustersAddDialogHarnessOptions extends BaseHarnessFilters {}

export class KafkaClustersAddDialogHarness extends ComponentHarness {
  public static readonly hostSelector = `kafka-clusters-add-dialog`;

  public static with(options: KafkaClustersAddDialogHarnessOptions): HarnessPredicate<KafkaClustersAddDialogHarness> {
    return new HarnessPredicate(KafkaClustersAddDialogHarness, options);
  }

  protected _title = this.locatorForOptional(MatDialogSection.TITLE);
  protected nameInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="name"]' }));
  protected descriptionInput = this.locatorForOptional(MatInputHarness.with({ selector: '[formControlName="description"]' }));

  public async cancel(): Promise<void> {
    const cancelButton = await this.locatorFor(MatButtonHarness.with({ text: /Cancel/ }))();
    await cancelButton.click();
  }

  public async setName(text: string) {
    await this.nameInput().then(input => input.setValue(text));
  }
  public async getName() {
    return await this.nameInput().then(input => input.getValue());
  }

  public async setDescription(text: string) {
    await this.descriptionInput().then(input => input.setValue(text));
  }
  public async getDescription() {
    return await this.descriptionInput().then(input => input.getValue());
  }

  async create() {
    const createButton = await this.locatorFor(MatButtonHarness.with({ text: /Create/ }))();
    await createButton.click();
  }
}
