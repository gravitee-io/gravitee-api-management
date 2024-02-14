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
import { MatRadioButtonHarness, MatRadioGroupHarness } from '@angular/material/radio/testing';

export class ApiDocumentationV4VisibilityHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-visibility';

  private radioGroupLocator = this.locatorFor(MatRadioGroupHarness);
  private publicRadioLocator = this.locatorFor(MatRadioButtonHarness.with({ selector: '[value="PUBLIC"]' }));
  private privateRadioLocator = this.locatorFor(MatRadioButtonHarness.with({ selector: '[value="PRIVATE"]' }));

  public getPublicRadioOption() {
    return this.publicRadioLocator();
  }
  public getPrivateRadioOption() {
    return this.privateRadioLocator();
  }

  public getValue(): Promise<string> {
    return this.radioGroupLocator().then((radioGroup) => radioGroup.getCheckedValue());
  }

  public async formIsDisabled(): Promise<boolean> {
    const publicIsDisabled = await this.publicRadioLocator().then((btn) => btn.isDisabled());
    const privateIsDisabled = await this.privateRadioLocator().then((btn) => btn.isDisabled());

    return publicIsDisabled && privateIsDisabled;
  }
}
