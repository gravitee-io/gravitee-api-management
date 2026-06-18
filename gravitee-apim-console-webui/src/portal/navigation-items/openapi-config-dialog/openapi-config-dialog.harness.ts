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
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';

export class OpenApiConfigDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'openapi-config-dialog';

  private readonly locateSaveButton = this.locatorFor(MatButtonHarness.with({ text: /^Save$/ }));
  private readonly locateCancelButton = this.locatorFor(MatButtonHarness.with({ text: /^Cancel$/ }));
  private readonly locateBaseUrlInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="base-url"]' }));
  private readonly locateEntrypointsAsServersToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="entrypoints-as-servers-toggle"]' }),
  );
  private readonly locateContextPathAsServerToggle = this.locatorFor(
    MatSlideToggleHarness.with({ selector: '[data-testid="context-path-as-server-toggle"]' }),
  );

  async clickSaveButton(): Promise<void> {
    return (await this.locateSaveButton()).click();
  }

  async clickCancelButton(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  async getBaseUrlInput(): Promise<MatInputHarness> {
    return this.locateBaseUrlInput();
  }

  async getEntrypointsAsServersToggle(): Promise<MatSlideToggleHarness> {
    return this.locateEntrypointsAsServersToggle();
  }

  async getContextPathAsServerToggle(): Promise<MatSlideToggleHarness> {
    return this.locateContextPathAsServerToggle();
  }
}
