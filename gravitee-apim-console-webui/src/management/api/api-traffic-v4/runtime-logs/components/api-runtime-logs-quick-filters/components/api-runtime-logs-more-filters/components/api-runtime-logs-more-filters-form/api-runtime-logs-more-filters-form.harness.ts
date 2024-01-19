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
import { MatLegacySelectHarness as MatSelectHarness } from '@angular/material/legacy-select/testing';
import { MatLegacyInputHarness as MatInputHarness } from '@angular/material/legacy-input/testing';
import { MatLegacyChipListHarness as MatChipListHarness } from '@angular/material/legacy-chips/testing';
import { MatLegacyFormFieldHarness as MatFormFieldHarness } from '@angular/material/legacy-form-field/testing';
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';

export class ApiRuntimeLogsMoreFiltersFormHarness extends ComponentHarness {
  static hostSelector = 'api-runtime-logs-more-filters-form';

  public getPeriodSelectInput = this.locatorFor(MatSelectHarness.with({ selector: '[formControlName="period"]' }));
  public getFromInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="from"]' }));
  public getToInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName="to"]' }));
  public getStatusesChips = this.locatorFor(MatChipListHarness.with({ selector: '[formControlName="statuses"]' }));
  public getApplicationField = this.locatorFor(MatFormFieldHarness.with({ floatingLabelText: 'Applications' }));

  public async getApplicationAutocomplete() {
    const applicationFormField = await this.getApplicationField();
    const tagsInput: GioFormTagsInputHarness | null = await applicationFormField.getControl(GioFormTagsInputHarness);
    return await tagsInput.getMatAutocompleteHarness();
  }
}
