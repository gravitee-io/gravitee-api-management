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
import { MatButtonHarness } from '@angular/material/button/testing';

export class ApiCreationV4Step1Harness extends ComponentHarness {
  static hostSelector = 'api-creation-v4-step-1';

  protected getNameInputElement = this.locatorFor(
    MatInputHarness.with({
      selector: '#name',
    }),
  );

  protected getVersionInputElement = this.locatorFor(
    MatInputHarness.with({
      selector: '#version',
    }),
  );

  protected getDescriptionInputElement = this.locatorFor(
    MatInputHarness.with({
      selector: '#description',
    }),
  );

  protected getValidateButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#validate',
    }),
  );

  protected getExitButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#exit',
    }),
  );

  async setName(name: string): Promise<void> {
    return this.getNameInputElement().then((elt) => elt.setValue(name));
  }

  async setVersion(version: string): Promise<void> {
    return this.getVersionInputElement().then((elt) => elt.setValue(version));
  }

  async setDescription(description: string): Promise<void> {
    return this.getDescriptionInputElement().then((elt) => elt.setValue(description));
  }

  async clickValidate(): Promise<void> {
    return this.getValidateButton().then((elt) => elt.click());
  }

  async clickExit(): Promise<void> {
    return this.getExitButton().then((elt) => elt.click());
  }
}
