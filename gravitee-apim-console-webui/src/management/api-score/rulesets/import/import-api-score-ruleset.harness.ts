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

import { AsyncFactoryFn, ComponentHarness } from '@angular/cdk/testing';
import { GioFormFilePickerInputHarness, GioFormSelectionInlineHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatInputHarness } from '@angular/material/input/testing';

export class ImportApiScoreRulesetHarness extends ComponentHarness {
  static hostSelector = 'import-api-score-ruleset';

  locatorForDefinitionFormatRadioGroup = this.locatorFor(
    GioFormSelectionInlineHarness.with({ selector: '[data-testid=definition-format-selection]' }),
  );
  locatorForGraviteeApiDefinitionFormatRadioGroup = this.locatorFor(
    GioFormSelectionInlineHarness.with({ selector: '[data-testid=gravitee-api-format-selection]' }),
  );

  private nameInputLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=name-input]' }),
  );
  private descriptionTextAreaLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=description]' }),
  );

  public async setName(name: string) {
    return this.nameInputLocator().then((input: MatInputHarness) => input.setValue(name));
  }

  public async setDescription(description: string) {
    return this.descriptionTextAreaLocator().then((input: MatInputHarness) => input.setValue(description));
  }

  private getFilePicker = this.locatorFor(GioFormFilePickerInputHarness);

  public async pickFiles(files: File[]) {
    return this.getFilePicker().then((filePicker) => filePicker.dropFiles(files));
  }

  locatorForSubmitImportButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=import-button]' }));
  locatorForCancelImportButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=cancel-button]' }));
}
