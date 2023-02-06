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

export class Step3Endpoints2ConfigHarness extends ComponentHarness {
  static hostSelector = 'step-3-endpoints-2-config';

  protected getPreviousButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#previous',
    }),
  );

  protected getValidateButton = this.locatorFor(
    MatButtonHarness.with({
      selector: '#validate',
    }),
  );

  async clickPrevious(): Promise<void> {
    return this.getPreviousButton().then((elt) => elt.click());
  }

  async clickValidate() {
    return this.getValidateButton().then((elt) => elt.click());
  }
}
