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

  protected locateNameInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="certificate-name-input"]' }));
  protected locatePemInput = this.locatorFor(MatInputHarness.with({ selector: '[data-testid="certificate-pem-input"]' }));
  protected locateContinueUploadButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="certificate-continue-upload-button"]' }),
  );
  protected locateContinueConfigureButton = this.locatorFor(
    MatButtonHarness.with({ selector: '[data-testid="certificate-continue-configure-button"]' }),
  );
  protected locateSubmitButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="certificate-submit-button"]' }));
  protected locateCancelButton = this.locatorFor(MatButtonHarness.with({ selector: '[data-testid="certificate-cancel-button"]' }));
  protected locatePreviousButton = this.locatorForOptional(
    MatButtonHarness.with({ selector: '[data-testid="certificate-previous-button"]' }),
  );
  protected locateGracePeriodInput = this.locatorForOptional(
    MatInputHarness.with({ selector: '[data-testid="certificate-grace-period-input"]' }),
  );
  protected locateSubmitErrorEl = this.locatorForOptional('[data-testid="certificate-submit-error"]');
  protected locateValidateErrorEl = this.locatorForOptional('[data-testid="certificate-validate-error"]');

  public async nameInput(): Promise<MatInputHarness> {
    return this.locateNameInput();
  }

  public async pemInput(): Promise<MatInputHarness> {
    return this.locatePemInput();
  }

  public async clickContinueUpload(): Promise<void> {
    return (await this.locateContinueUploadButton()).click();
  }

  public async clickContinueConfigure(): Promise<void> {
    return (await this.locateContinueConfigureButton()).click();
  }

  public async clickSubmit(): Promise<void> {
    return (await this.locateSubmitButton()).click();
  }

  public async clickCancel(): Promise<void> {
    return (await this.locateCancelButton()).click();
  }

  public async previousButton(): Promise<MatButtonHarness | null> {
    return this.locatePreviousButton();
  }

  public async gracePeriodInput(): Promise<MatInputHarness | null> {
    return this.locateGracePeriodInput();
  }

  public async submitErrorText(): Promise<string | null> {
    const el = await this.locateSubmitErrorEl();
    return el ? el.text() : null;
  }

  public async validateErrorText(): Promise<string | null> {
    const el = await this.locateValidateErrorEl();
    return el ? el.text() : null;
  }
}
