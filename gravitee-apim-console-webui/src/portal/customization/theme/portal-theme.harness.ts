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

import { GioFormColorInputHarness } from '../../../shared/components/gio-form-color-input/gio-form-color-input.harness';

export class PortalThemeHarness extends ComponentHarness {
  static readonly hostSelector = 'developer-portal-theme';

  private getPrimaryColorInput = this.locatorFor(GioFormColorInputHarness.with({ selector: '[formControlName=primaryColor]' }));
  private getPublishButton = this.locatorFor(MatButtonHarness.with({ selector: '[type=submit]' }));
  private getDiscardButton = this.locatorFor(MatButtonHarness.with({ selector: '[type=button]' }));

  public async setPrimaryColor(color: string) {
    return this.getPrimaryColorInput().then((input) => input.setValue(color));
  }

  public async getPrimaryColor() {
    return this.getPrimaryColorInput().then((input) => input.getValue());
  }

  public async submit() {
    return this.getPublishButton().then((publishButton) => publishButton.click());
  }

  public async isSubmitInvalid() {
    return this.getPublishButton().then((publishButton) => publishButton.isDisabled());
  }

  public reset() {
    return this.getDiscardButton().then((discardButton) => discardButton.click());
  }
}
