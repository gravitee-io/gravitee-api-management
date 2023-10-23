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
import { MatRadioButtonHarness } from '@angular/material/radio/testing';

export class ApiDocumentationV4VisibilityHarness extends ComponentHarness {
  public static hostSelector = 'api-documentation-visibility';

  private publicRadioLocator = this.locatorFor(MatRadioButtonHarness.with({ label: 'PublicRequires no subscription to view' }));
  private privateRadioLocator = this.locatorFor(MatRadioButtonHarness.with({ label: 'PrivateRequires approved subscription to view' }));

  public getPublicRadioOption() {
    return this.publicRadioLocator();
  }
  public getPrivateRadioOption() {
    return this.privateRadioLocator();
  }
}
