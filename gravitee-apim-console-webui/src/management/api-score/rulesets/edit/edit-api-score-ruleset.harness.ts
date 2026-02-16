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
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';

export class EditApiScoreRulesetHarness extends ComponentHarness {
  static readonly hostSelector: string = 'edit-api-score-ruleset';

  private readonly nameLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=name-input]' }),
  );

  private readonly descriptionLocator: AsyncFactoryFn<MatInputHarness> = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid=description]' }),
  );

  public async setName(value: string) {
    return this.nameLocator().then(input => input.setValue(value));
  }

  public async setDescription(value: string) {
    return this.descriptionLocator().then(input => input.setValue(value));
  }

  public saveBarLocator = this.locatorForOptional(GioSaveBarHarness);

  public deleteRulesetButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid=delete-ruleset-button]' }));
}
