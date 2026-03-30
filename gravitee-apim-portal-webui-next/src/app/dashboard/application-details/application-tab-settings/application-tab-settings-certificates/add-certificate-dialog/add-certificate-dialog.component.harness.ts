/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

export class AddCertificateDialogHarness extends ComponentHarness {
  static readonly hostSelector = 'app-add-certificate-dialog';

  private readonly getNameInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
  private readonly getPemInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
  private readonly getContinueUploadButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="certificate-continue-upload-button"]' }),
  );
  private readonly getContinueConfigureButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="certificate-continue-configure-button"]' }),
  );
  private readonly getSubmitButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="certificate-submit-button"]' }));
  private readonly getCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="certificate-cancel-button"]' }));
  private readonly getPreviousButton = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="certificate-previous-button"]' }),
  );
  private readonly getGracePeriodInput = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid="certificate-grace-period-input"]' }),
  );
  private readonly getSubmitErrorEl = this.locatorForOptional('[data-testid="certificate-submit-error"]');

  async nameInput(): Promise<MatInputHarness> {
    return this.getNameInput();
  }

  async pemInput(): Promise<MatInputHarness> {
    return this.getPemInput();
  }

  async clickContinueUpload(): Promise<void> {
    return (await this.getContinueUploadButton()).click();
  }

  async clickContinueConfigure(): Promise<void> {
    return (await this.getContinueConfigureButton()).click();
  }

  async clickSubmit(): Promise<void> {
    return (await this.getSubmitButton()).click();
  }

  async clickCancel(): Promise<void> {
    return (await this.getCancelButton()).click();
  }

  async previousButton(): Promise<MatButtonHarness | null> {
    return this.getPreviousButton();
  }

  async gracePeriodInput(): Promise<MatInputHarness | null> {
    return this.getGracePeriodInput();
  }

  async submitErrorText(): Promise<string | null> {
    const el = await this.getSubmitErrorEl();
    return el ? el.text() : null;
  }
}
