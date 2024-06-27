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
import { MatInputHarness } from '@angular/material/input/testing';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';

export class DeveloperPortalApiHarness extends ComponentHarness {
  static readonly hostSelector = 'developer-portal-api';

  private getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[formControlName=name]' }));
  private getSaveBar = this.locatorFor(GioSaveBarHarness);

  public async setName(name: string) {
    return this.getNameInput().then((input) => input.setValue(name));
  }

  public async getName() {
    return this.getNameInput().then((input) => input.getValue());
  }

  public async submit() {
    return this.getSaveBar().then((saveBar) => saveBar.clickSubmit());
  }

  public async isSubmitInvalid() {
    return this.getSaveBar().then((saveBar) => saveBar.isSubmitButtonInvalid());
  }

  public reset() {
    return this.getSaveBar().then((saveBar) => saveBar.clickReset());
  }
}
