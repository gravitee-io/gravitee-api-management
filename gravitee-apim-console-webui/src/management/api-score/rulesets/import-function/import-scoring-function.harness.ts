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
import { GioFormFilePickerInputHarness } from '@gravitee/ui-particles-angular';
import { MatButtonHarness } from '@angular/material/button/testing';

export class ImportScoringFunctionHarness extends ComponentHarness {
  static readonly hostSelector = 'import-scoring-function';

  private getFilePicker = this.locatorFor(GioFormFilePickerInputHarness);

  public async pickFiles(files: File[]) {
    return this.getFilePicker().then(filePicker => filePicker.dropFiles(files));
  }

  locatorForSubmitImportButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=import-button]' }));
}
