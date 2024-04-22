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

export class ApiGeneralInfoExportV4DialogHarness extends ComponentHarness {
  static hostSelector = 'api-general-info-export-v4-dialog';

  public async getExportOptions() {
    const checkboxes = await this.locatorForAll(MatCheckboxHarness.with({ selector: '.content__options__option' }))();

    return parallel(() => checkboxes.map(async (checkbox) => checkbox.getLabelText()));
  }

  public async setExportOptions(options: { groups?: boolean; members?: boolean; pages?: boolean; plans?: boolean; metadata?: boolean }) {
    await parallel(() =>
      Object.entries(options).map(async ([key, value]) => {
        const checkbox = await this.locatorFor(MatCheckboxHarness.with({ selector: `[ng-reflect-name="${key}"]` }))();
        value ? await checkbox.check() : await checkbox.uncheck();
      }),
    );
  }

  public async export() {
    const exportButton = await this.locatorFor('[data-testid="api_info_export_api"]')();
    return exportButton.click();
  }
}
