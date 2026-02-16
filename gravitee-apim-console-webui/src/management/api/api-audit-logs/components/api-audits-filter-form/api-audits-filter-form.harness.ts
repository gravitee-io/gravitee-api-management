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
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { MatDateRangeInputHarness } from '@angular/material/datepicker/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiAuditsFilterFormHarness extends ComponentHarness {
  static hostSelector = 'api-audits-filter-form';

  public async searchEvent(event: string) {
    const field = await this.getEventField();
    const autocomplete = await field.getMatAutocompleteHarness();
    await autocomplete.enterText(event);
    return autocomplete.getOptions();
  }

  public async setDateRange(range: { start: string; end: string }) {
    const field = await this.getDateRangeField();
    await field.getStartInput().then(input => input.setValue(range.start));
    await field.getEndInput().then(input => input.setValue(range.end));
  }

  public async reset() {
    return this.getResetButton().then(button => button.click());
  }

  private getEventField = this.locatorFor(GioFormTagsInputHarness.with({ selector: '[formControlName="events"]' }));
  private getDateRangeField = this.locatorFor(MatDateRangeInputHarness.with({ selector: '[formGroupName=range]' }));
  private getResetButton = this.locatorFor(MatButtonHarness.with({ text: 'Reset' }));
}
