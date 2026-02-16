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
import { ComponentHarness, parallel } from '@angular/cdk/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';

export class ApiGeneralInfoExportV4DialogHarness extends ComponentHarness {
  static hostSelector = 'api-general-info-export-v4-dialog';

  public async getExportOptions() {
    const checkboxes = await this.locatorForAll(MatCheckboxHarness.with({ selector: '.content__options__option' }))();

    return parallel(() => checkboxes.map(async checkbox => checkbox.getLabelText()));
  }

  public async setExportOptions(options: string[]): Promise<void> {
    const checkboxes = await this.locatorForAll(MatCheckboxHarness.with({ selector: '.content__options__option' }))();
    for (const checkbox of checkboxes) {
      const label = await checkbox.getLabelText();
      if (options.includes(label)) {
        const isChecked = await checkbox.isChecked();
        if (!isChecked) {
          await checkbox.check();
        }
      } else {
        const isChecked = await checkbox.isChecked();
        if (isChecked) {
          await checkbox.uncheck();
        }
      }
    }
  }

  public async selectCRDTab(): Promise<void> {
    const tabs = await this.locatorFor(MatTabGroupHarness)();
    await tabs.selectTab({ label: 'CRD API Definition' });
  }

  public async export() {
    const exportButton = await this.locatorFor('[data-testid="api_info_export_api"]')();
    return exportButton.click();
  }
}
